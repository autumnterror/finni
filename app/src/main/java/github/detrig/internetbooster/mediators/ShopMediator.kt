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
import github.detrig.feature.shop.api.ShopItemDetailsResolver
import github.detrig.feature.shop.api.foodEffectDetails
import github.detrig.feature.shop.api.ShopPetPortrait
import github.detrig.feature.shop.api.ShopPurchaseFeedback
import github.detrig.feature.shop.domain.ShopCatalogRegistry
import github.detrig.feature.shop.domain.ShopDecisionEvent
import github.detrig.feature.shop.domain.ShopDecisionEventType
import github.detrig.feature.shop.domain.ShopLearningEventConfig
import github.detrig.feature.shop.domain.ShopLearningEventGenerator
import github.detrig.feature.learning.domain.PurchaseProblem
import github.detrig.feature.learning.domain.PurchaseOutcome
import github.detrig.feature.planning.domain.PlanCategory
import github.detrig.products.GroceryCatalog
import github.detrig.products.GroceryStoreIds
import github.detrig.products.ProductQuantity
import github.detrig.feature.inventory.api.InventoryApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

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
    private val gameStateMediator: GameStateMediator,
) : Mediator<ShopApi> {
    private val groceryCatalog = GroceryCatalog()
    private val wishMutex = Mutex()
    private val declineMutex = Mutex()
    private val wishPreferences = coreComponent.context.getSharedPreferences(
        "finpet_daily_wish", android.content.Context.MODE_PRIVATE)
    private val eventPreferences = coreComponent.context.getSharedPreferences(
        "finpet_shop_events", android.content.Context.MODE_PRIVATE)
    private val shopEventRandomSeed = if (eventPreferences.contains(SHOP_EVENT_SEED_KEY)) {
        eventPreferences.getLong(SHOP_EVENT_SEED_KEY, 0L)
    } else {
        Random.nextLong().also { seed ->
            check(eventPreferences.edit().putLong(SHOP_EVENT_SEED_KEY, seed).commit()) {
                "Failed to persist shop event seed"
            }
        }
    }
    private val artworkResolver = GroceryArtworkResolver(R.drawable.grocery_product_atlas)
    private val catalogRegistry = ShopCatalogRegistry { storeId ->
        groceryCatalog.takeIf { storeId == GroceryStoreIds.Store }
    }
    private val learningEventConfig = ShopLearningEventConfig(
        promotionEventProbability = PROMOTION_EVENT_PROBABILITY,
        // Pet wishes use one shared happiness-dependent daily schedule below.
        impulseWishEventProbability = 0.0,
        firstPromotionDelayDays = FIRST_PROMOTION_DELAY_DAYS,
        receiptCheckEventProbability = RECEIPT_CHECK_EVENT_PROBABILITY,
        promotionDiscountPercent = PROMOTION_DISCOUNT_PERCENT,
        buyTwoGetOnePromotionProbability = BUY_TWO_GET_ONE_PROMOTION_PROBABILITY,
        randomSeed = shopEventRandomSeed,
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
            petNeeds = {
                gameStateMediator.getApi().initialize().pet.let { it.hunger to it.happiness }
            },
        )
    }

    private val detailsResolver = ShopItemDetailsResolver(::foodEffectDetails)
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
            recordDeclinedOnce(event)
        }

        override suspend fun checkout(request: github.detrig.feature.shop.api.ShopCheckoutRequest):
            github.detrig.feature.shop.api.ShopCheckoutResult {
            val prepared = learningCoordinator.prepareCheckout(request)
            val currentBalance = economyMediator.getApi().getState().availableRub
            val alreadyPurchased = economyMediator.getApi().getExpenseHistory()
                .any { it.id == request.operationId }
            if (!request.confirmedConsequence && !alreadyPurchased &&
                currentBalance >= prepared.totalRub &&
                prepared.assessment.outcome != PurchaseOutcome.GOOD
            ) {
                return github.detrig.feature.shop.api.ShopCheckoutResult.RequiresConfirmation(
                    balanceRub = currentBalance,
                    consequence = prepared.assessment.problem.toShopFeedback()
                        ?: ShopPurchaseFeedback.PLAN_CHANGED,
                    categoryOverrunRub = prepared.assessment.categoryOverrunRub,
                    savingsPlanReductionRub = prepared.assessment.savingsPlanReductionRub,
                )
            }
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
                result.copy(feedback = if (request.confirmedConsequence) null else {
                    prepared.assessment.problem.toShopFeedback()
                        ?: ShopPurchaseFeedback.PLAN_CHANGED.takeIf {
                            prepared.assessment.outcome == PurchaseOutcome.PLAN_ADJUSTMENT
                        }
                })
            } else {
                result
            }
        }
    }

    suspend fun claimRoomImpulseWish(): PendingImpulseWish? {
        val event = currentPetWish()
            ?.takeIf { it.kind == PetWishSchedule.Kind.GROCERY }
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
            productTitle = event.title,
            phraseVariant = Math.floorMod(event.eventId.hashCode(), IMPULSE_PHRASE_COUNT),
            showIntroduction = showIntroduction,
        )
    }

    suspend fun currentPetWish(): PetWishSchedule.Wish? = wishMutex.withLock {
        val week = weekMediator.getApi().initialize()
        val game = gameStateMediator.getApi().initialize()
        val pet = petMediator.getApi()
        val storedDay = wishPreferences.getLong("day", -1L)
        val currentOwnedClothing = pet.currentProfile()?.clothing?.ownedIds.orEmpty()
        val currentUnlockedGame = game.ownedZoneIds.any { it in setOf("fishing", "drawing", "music") }
        val plan = planningMediator.getApi().getPlanProgress(week.weekNumber)
        val balance = economyMediator.getApi().getState().availableRub
        val wantsRemaining = plan?.category(PlanCategory.WANTS)?.let {
            (it.plannedRub - it.actualRub).coerceAtLeast(0L)
        } ?: balance
        val mandatoryRemaining = plan?.category(PlanCategory.MANDATORY)?.let {
            (it.plannedRub - it.actualRub).coerceAtLeast(0L)
        } ?: 0L
        val safeOptionalRub = minOf(wantsRemaining,
            (balance - mandatoryRemaining - (plan?.plan?.reserveRub ?: 0L)).coerceAtLeast(0L))
        if (storedDay != week.absoluteDay) {
            wishPreferences.edit()
                .putLong("day", week.absoluteDay)
                .putInt("happiness", game.pet.happiness)
                .putString("owned_clothing_ids", currentOwnedClothing.sorted().joinToString(","))
                .putBoolean("unlocked_game", currentUnlockedGame)
                .putLong("safe_optional_rub", safeOptionalRub)
                .commit()
        }
        PetWishSchedule.current(
            absoluteDay = week.absoluteDay,
            happiness = wishPreferences.getInt("happiness", game.pet.happiness),
            clothing = pet.clothingItems(),
            ownedClothingIds = wishPreferences.getString("owned_clothing_ids", null)
                ?.split(',')?.filter(String::isNotBlank)?.toSet() ?: currentOwnedClothing,
            hasUnlockedGame = wishPreferences.getBoolean("unlocked_game", currentUnlockedGame),
            safeOptionalRub = wishPreferences.getLong("safe_optional_rub", safeOptionalRub),
        )
    }

    suspend fun recordRoomWishDeclined(wish: github.detrig.feature.room.domain.model.RoomImpulseWish) {
        when (wish.kind) {
            github.detrig.feature.room.domain.model.RoomImpulseWish.Kind.GROCERY -> {
                val week = weekMediator.getApi().initialize()
                val item = groceryCatalog.storefront.items.firstOrNull {
                    it.id.value == wish.productId
                } ?: return
                recordDeclinedOnce(ShopDecisionEvent(
                    eventId = wish.eventId,
                    type = ShopDecisionEventType.IMPULSE_WISH,
                    gamePeriod = week.weekNumber,
                    eventPeriod = week.absoluteDay,
                    storeId = GroceryStoreIds.Store,
                    productId = item.id,
                    productTitle = item.title,
                    regularPriceRub = item.priceRub,
                    offeredPriceRub = item.priceRub,
                ))
            }
            github.detrig.feature.room.domain.model.RoomImpulseWish.Kind.CLOTHING ->
                learningCoordinator.recordDeclinedClothingWish(wish.eventId, wish.priceRub)
            github.detrig.feature.room.domain.model.RoomImpulseWish.Kind.FREE -> Unit
        }
    }

    private suspend fun recordDeclinedOnce(event: ShopDecisionEvent) = declineMutex.withLock {
        val day = weekMediator.getApi().initialize().absoluteDay
        val prior = if (wishPreferences.getLong("decision_day", -1L) == day) {
            wishPreferences.getStringSet("declined_events", emptySet()).orEmpty().toSet()
        } else emptySet()
        if (event.eventId in prior) return@withLock
        learningCoordinator.recordDeclinedEvent(event)
        wishPreferences.edit()
            .putLong("decision_day", day)
            .putStringSet("declined_events", prior + event.eventId)
            .commit()
    }

    private suspend fun generateCurrentEvent(storeId: github.detrig.products.StoreId): ShopDecisionEvent? {
        val storefront = catalogRegistry.catalog(storeId)?.storefront ?: return null
        val week = weekMediator.getApi().initialize()
        val promotion = learningEventGenerator.eventFor(
            storefront = storefront,
            gamePeriod = week.weekNumber,
            eventPeriod = week.absoluteDay,
        )
        if (promotion != null) return promotion
        val wish = currentPetWish()?.takeIf { it.kind == PetWishSchedule.Kind.GROCERY }
            ?: return null
        val item = storefront.items.firstOrNull { it.id.value == wish.productId } ?: return null
        return ShopDecisionEvent(
            eventId = wish.eventId,
            type = ShopDecisionEventType.IMPULSE_WISH,
            gamePeriod = week.weekNumber,
            eventPeriod = week.absoluteDay,
            storeId = storeId,
            productId = item.id,
            productTitle = item.title,
            regularPriceRub = item.priceRub,
            offeredPriceRub = item.priceRub,
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
        const val PROMOTION_EVENT_PROBABILITY = 0.15
        const val FIRST_PROMOTION_DELAY_DAYS = 4L
        const val RECEIPT_CHECK_EVENT_PROBABILITY = 0.0
        const val PROMOTION_DISCOUNT_PERCENT = 30
        const val BUY_TWO_GET_ONE_PROMOTION_PROBABILITY = 0.25
        const val SHOP_EVENT_SEED_KEY = "shop_event_seed_v1"
        const val CURRENT_PROFILE_ID = "current"
        const val DEFAULT_PET_NAME = "Питомец"
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
    PurchaseProblem.RESERVE_AT_RISK -> ShopPurchaseFeedback.RESERVE_AT_RISK
    PurchaseProblem.PROMOTION_OVERBUY -> ShopPurchaseFeedback.PROMOTION_OVERBUY
    PurchaseProblem.TOO_MANY_EXTRAS -> ShopPurchaseFeedback.TOO_MANY_EXTRAS
    null -> null
}
