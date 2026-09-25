package github.detrig.internetbooster.mediators

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import github.detrig.core.Mediator
import github.detrig.core.di.CoreComponent
import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.core.audio.GameAudio
import github.detrig.internetbooster.audio.AppAudioCues
import github.detrig.internetbooster.R
import github.detrig.feature.shop.ShopDependencies
import github.detrig.feature.shop.ShopFeature
import github.detrig.feature.shop.api.ShopApi
import github.detrig.feature.shop.api.ShopArtworkResolver
import github.detrig.feature.shop.api.ShopHost
import github.detrig.feature.shop.api.ShopItemDetail
import github.detrig.feature.shop.api.ShopItemDetailIcon
import github.detrig.feature.shop.api.ShopItemDetailsResolver
import github.detrig.feature.shop.api.ShopPetPortrait
import github.detrig.feature.shop.api.ShopPurchaseFeedback
import github.detrig.feature.shop.domain.ShopCatalogRegistry
import github.detrig.feature.shop.domain.ShopDecisionEvent
import github.detrig.feature.shop.domain.ShopDecisionEventType
import github.detrig.feature.shop.domain.ShopLearningEventConfig
import github.detrig.feature.shop.domain.ShopLearningEventGenerator
import github.detrig.feature.learning.domain.PurchaseProblem
import github.detrig.products.FoodItem
import github.detrig.products.GroceryCatalog
import github.detrig.products.GroceryStoreIds
import github.detrig.products.ProductQuantity
import github.detrig.feature.inventory.api.InventoryApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Wires the reusable product catalog screen to application state. */
internal class ShopMediator(
    private val coreComponent: CoreComponent,
    private val economyMediator: EconomyMediator,
    private val weekMediator: WeekMediator,
    private val planningMediator: PlanningMediator,
    private val inventoryApi: InventoryApi,
    private val gameAudio: GameAudio,
    private val learningMediator: LearningMediator,
    private val petMediator: PetMediator,
) : Mediator<ShopApi> {
    private val groceryCatalog = GroceryCatalog()
    private val artworkResolver = GroceryArtworkResolver(R.drawable.grocery_product_atlas)
    private val catalogRegistry = ShopCatalogRegistry { storeId ->
        groceryCatalog.takeIf { storeId == GroceryStoreIds.Store }
    }
    private val learningEventConfig = ShopLearningEventConfig(
        promotionEventProbability = PROMOTION_EVENT_PROBABILITY,
        impulseWishEventProbability = IMPULSE_WISH_EVENT_PROBABILITY,
        firstPromotionDelayDays = FIRST_PROMOTION_DELAY_DAYS,
        receiptCheckEventProbability = RECEIPT_CHECK_EVENT_PROBABILITY,
        promotionDiscountPercent = PROMOTION_DISCOUNT_PERCENT,
        buyTwoGetOnePromotionProbability = BUY_TWO_GET_ONE_PROMOTION_PROBABILITY,
        randomSeed = SHOP_EVENT_RANDOM_SEED,
    )
    private val learningEventGenerator = ShopLearningEventGenerator(learningEventConfig)
    private val checkoutGateway = ShopCheckoutGateway(
        catalogRegistry = catalogRegistry,
        currentBalanceRub = { economyMediator.getApi().getState().availableRub },
        debit = { operationId, amountRub, context ->
            economyMediator.getApi().debit(operationId, amountRub, context)
        },
        purchaseHistory = { economyMediator.getApi().getExpenseHistory() },
        deliverFood = { operationId, storeId, lines ->
            if (storeId == GroceryStoreIds.Store) {
                inventoryApi.deliver(
                    operationId = operationId,
                    items = lines.map { ProductQuantity(it.itemId, it.quantity) },
                )
            }
        },
    )
    private val learningCoordinator by lazy {
        ShopLearningCoordinator(
            economyApi = economyMediator.getApi(),
            weekApi = weekMediator.getApi(),
            planningApi = planningMediator.getApi(),
            inventoryApi = inventoryApi,
            learningApi = learningMediator.getApi(),
            catalogRegistry = catalogRegistry,
        )
    }

    private val detailsResolver = ShopItemDetailsResolver { item ->
        val food = item as? FoodItem ?: return@ShopItemDetailsResolver emptyList()
        listOf(
            ShopItemDetail(
                text = "+${food.effects.satietyPercent}%",
                icon = ShopItemDetailIcon.SATIETY,
            ),
        )
    }
    private val petPortrait = ShopPetPortrait { modifier ->
        CurrentPetPortrait(modifier)
    }

    private val host = object : ShopHost {
        override suspend fun preparePlayer() {
            economyMediator.getApi().initialize()
            weekMediator.getApi().initialize()
            learningCoordinator.reconcile(economyMediator.getApi().getExpenseHistory())
        }

        override fun observeBalanceRub() = economyMediator.getApi()
            .observeState()
            .map { it.availableRub }
            .distinctUntilChanged()

        override fun observePetName() = petMediator.getApi().observeProfile()
            .map { it?.name ?: DEFAULT_PET_NAME }
            .distinctUntilChanged()

        override suspend fun currentDecisionEvent(storeId: github.detrig.products.StoreId) =
            generateCurrentEvent(storeId)

        override suspend fun claimPromotionIntroduction(): Boolean =
            learningMediator.getApi().claimFirstExplanation(
                CURRENT_PROFILE_ID,
                PROMOTION_INTRODUCTION_ID,
            )

        override suspend fun recordEventDeclined(
            event: github.detrig.feature.shop.domain.ShopDecisionEvent,
        ) {
            learningCoordinator.recordDeclinedEvent(event)
        }

        override suspend fun checkout(request: github.detrig.feature.shop.api.ShopCheckoutRequest):
            github.detrig.feature.shop.api.ShopCheckoutResult {
            val prepared = learningCoordinator.prepareCheckout(request)
            val result = checkoutGateway.checkout(
                request = request,
                lineTotalOverrides = prepared.lineTotalOverrides,
                additionalMetadata = prepared.learningMetadata,
            )
            if (result is github.detrig.feature.shop.api.ShopCheckoutResult.Completed) {
                if (!result.alreadyApplied) gameAudio.play(AppAudioCues.Purchase)
                val operation = economyMediator.getApi().getExpenseHistory()
                    .firstOrNull { it.id == request.operationId }
                // The economy metadata is the durable source outbox. A failed projection must not
                // turn an already paid purchase into a UI error; the next preparePlayer reconciles it.
                runCatching {
                    learningCoordinator.recordCompletedPurchase(operation?.context?.metadata)
                }
            }
            return if (result is github.detrig.feature.shop.api.ShopCheckoutResult.Completed) {
                result.copy(feedback = prepared.purchaseProblem.toShopFeedback())
            } else {
                result
            }
        }
    }

    suspend fun claimRoomImpulseWish(): PendingImpulseWish? {
        val event = generateCurrentEvent(GroceryStoreIds.Store)
            ?.takeIf { it.type == ShopDecisionEventType.IMPULSE_WISH }
            ?: return null
        val learning = learningMediator.getApi()
        val firstPresentation = learning.claimFirstExplanation(
            CURRENT_PROFILE_ID,
            "$ROOM_IMPULSE_PRESENTATION_PREFIX${event.eventId}",
        )
        if (!firstPresentation) return null
        val showIntroduction = learning.claimFirstExplanation(
            CURRENT_PROFILE_ID,
            IMPULSE_INTRODUCTION_ID,
        )
        return PendingImpulseWish(
            eventId = event.eventId,
            productTitle = event.productTitle,
            phraseVariant = Math.floorMod(event.eventId.hashCode(), IMPULSE_PHRASE_COUNT),
            showIntroduction = showIntroduction,
        )
    }

    private suspend fun generateCurrentEvent(storeId: github.detrig.products.StoreId): ShopDecisionEvent? {
        val storefront = catalogRegistry.catalog(storeId)?.storefront ?: return null
        val week = weekMediator.getApi().initialize()
        return learningEventGenerator.eventFor(
            storefront = storefront,
            gamePeriod = week.weekNumber,
            eventPeriod = week.absoluteDay,
        )
    }

    @Composable
    private fun CurrentPetPortrait(modifier: Modifier) {
        val petApi = petMediator.getApi()
        val profile by petApi.observeProfile().collectAsState(initial = null)
        profile?.let { petApi.Portrait(it, modifier) }
    }

    fun init() {
        ShopFeature.dependenciesProvider = ModuleDependenciesProvider {
            object : ShopDependencies {
                override fun host(): ShopHost = host
                override fun catalogRegistry(): ShopCatalogRegistry = catalogRegistry
                override fun artworkResolver(): ShopArtworkResolver = artworkResolver
                override fun itemDetailsResolver(): ShopItemDetailsResolver = detailsResolver
                override fun petPortrait(): ShopPetPortrait = petPortrait
                override fun globalNavigator() = coreComponent.globalNavigator
                override fun gameAudio() = gameAudio
            }
        }
    }

    override fun getApi(): ShopApi = ShopFeature.getApi()

    fun artworkResolver(): ShopArtworkResolver = artworkResolver

    fun minimumGroceryPriceRub(): Long = groceryCatalog.storefront.items.minOf { it.priceRub }

    private companion object {
        // Independent knobs kept here so every random learning event can be forced during debugging.
        const val PROMOTION_EVENT_PROBABILITY = 0.25
        const val IMPULSE_WISH_EVENT_PROBABILITY = 0.25
        const val FIRST_PROMOTION_DELAY_DAYS = 4L
        const val RECEIPT_CHECK_EVENT_PROBABILITY = 0.0
        const val PROMOTION_DISCOUNT_PERCENT = 30
        const val BUY_TWO_GET_ONE_PROMOTION_PROBABILITY = 0.5
        const val SHOP_EVENT_RANDOM_SEED = 6_202L
        const val CURRENT_PROFILE_ID = "current"
        const val DEFAULT_PET_NAME = "Финни"
        const val PROMOTION_INTRODUCTION_ID = "purchase.promotion.introduction"
        const val IMPULSE_INTRODUCTION_ID = "purchase.impulse.introduction"
        const val ROOM_IMPULSE_PRESENTATION_PREFIX = "purchase.impulse.room:"
        const val IMPULSE_PHRASE_COUNT = 4
    }
}

internal data class PendingImpulseWish(
    val eventId: String,
    val productTitle: String,
    val phraseVariant: Int,
    val showIntroduction: Boolean,
)

private fun PurchaseProblem?.toShopFeedback(): ShopPurchaseFeedback? = when (this) {
    PurchaseProblem.REQUIRED_FOOD_MISSING -> ShopPurchaseFeedback.REQUIRED_FOOD_MISSING
    PurchaseProblem.MANDATORY_MONEY_AT_RISK -> ShopPurchaseFeedback.MANDATORY_MONEY_AT_RISK
    PurchaseProblem.PROMOTION_OVERBUY -> ShopPurchaseFeedback.PROMOTION_OVERBUY
    PurchaseProblem.TOO_MANY_EXTRAS -> ShopPurchaseFeedback.TOO_MANY_EXTRAS
    null -> null
}
