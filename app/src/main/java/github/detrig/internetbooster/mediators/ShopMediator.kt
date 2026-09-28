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
import github.detrig.feature.gamestate.domain.model.PetWishHappinessRewards
import github.detrig.feature.gamestate.domain.model.MiniGameAccess
import github.detrig.feature.room.domain.model.RoomWishObjectCandidate
import github.detrig.feature.room.domain.model.RoomWishFulfillment
import kotlinx.coroutines.CancellationException
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
    private val savingsMediator: SavingsMediator,
    private val interiorWishCandidates: () -> List<RoomWishObjectCandidate>,
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

        override fun observeSavingsRub() = economyMediator.getApi()
            .observeState()
            .map { it.savingsRub }
            .distinctUntilChanged()

        override fun observePetName() = petMediator.getApi().observeProfile()
            .map { it?.name ?: DEFAULT_PET_NAME }
            .distinctUntilChanged()

        override fun openSavings() = savingsMediator.getApi().open()

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
                prepared.assessment.outcome != PurchaseOutcome.GOOD &&
                !prepared.suppressPlanWarning
            ) {
                return github.detrig.feature.shop.api.ShopCheckoutResult.RequiresConfirmation(
                    balanceRub = currentBalance,
                    consequence = prepared.assessment.problem.toShopFeedback()
                        ?: ShopPurchaseFeedback.PLAN_CHANGED,
                    categoryOverrunRub = prepared.assessment.categoryOverrunRub,
                    savingsPlanReductionRub = prepared.assessment.savingsPlanReductionRub,
                )
            }
            val wishForPurchase = currentPetWishBoardWishes(
                absoluteDay = weekMediator.getApi().initialize().absoluteDay,
                activeGoalId = savingsMediator.getApi().getActiveGoalProgress()?.goal?.id,
            ).firstOrNull { wish ->
                wish.kind == PetWishSchedule.Kind.GROCERY &&
                    request.storeId == GroceryStoreIds.Store &&
                    request.lines.any { it.itemId.value == wish.productId }
            }
            val result = checkoutGateway.checkout(
                request = request,
                lineTotalOverrides = prepared.lineTotalOverrides,
                additionalMetadata = prepared.learningMetadata,
                petWishEventId = wishForPurchase?.eventId,
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
                wishForPurchase?.let { wish ->
                    wishMutex.withLock {
                        recordCompletedPetWish(wish.eventId)
                    }
                }
            }
            return if (result is github.detrig.feature.shop.api.ShopCheckoutResult.Completed) {
                result.copy(feedback = if (request.confirmedConsequence || prepared.suppressPlanWarning) null else {
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

    suspend fun claimRoomWishDialogue(
        wish: github.detrig.feature.room.domain.model.RoomImpulseWish,
    ): github.detrig.feature.room.domain.model.RoomImpulseWish? {
        val learning = learningMediator.getApi()
        val firstPresentation = learning.claimFirstExplanation(
            CURRENT_PROFILE_ID,
            "$ROOM_IMPULSE_PRESENTATION_PREFIX${wish.eventId}",
        )
        if (!firstPresentation) return null
        val showIntroduction = learning.claimFirstExplanation(
            CURRENT_PROFILE_ID,
            IMPULSE_INTRODUCTION_ID,
        )
        return wish.copy(showIntroduction = showIntroduction)
    }

    suspend fun currentPetWish(): PetWishSchedule.Wish? {
        val week = weekMediator.getApi().initialize()
        return currentPetWishBoardWishes(
            absoluteDay = week.absoluteDay,
            activeGoalId = savingsMediator.getApi().getActiveGoalProgress()?.goal?.id,
        ).filter { it.kind != PetWishSchedule.Kind.SAVINGS_TOP_UP }
            .maxByOrNull { it.createdAbsoluteDay }
    }

    suspend fun currentPetWishBoardWishes(
        absoluteDay: Long,
        activeGoalId: String?,
    ): List<PetWishSchedule.Wish> = wishMutex.withLock {
        val week = weekMediator.getApi().initialize()
        if (absoluteDay != week.absoluteDay) return@withLock emptyList()
        val game = gameStateMediator.getApi().initialize()
        val pet = petMediator.getApi()
        val storedDay = wishPreferences.getLong("day", -1L)
        val currentOwnedClothing = pet.currentProfile()?.clothing?.ownedIds.orEmpty()
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
        var wishes = readStoredPetWishes()
        val alreadyCompleted = wishPreferences.getStringSet("completed_wishes", emptySet()).orEmpty().toSet()
        wishes = wishes.filterNot { it.eventId in alreadyCompleted }

        val purchasedWishIds = economyMediator.getApi().getExpenseHistory().mapNotNull { operation ->
            operation.context.metadata?.split(';')?.firstOrNull { it.startsWith("pet_wish=") }
                ?.substringAfter('=')
        }.toSet()
        val fulfilledRoomItems = wishes.filter { wish ->
            wish.kind == PetWishSchedule.Kind.TOY && wish.productId?.let { it in game.ownedZoneIds } == true
        }.map { it.eventId }
        val fulfilledIds = (purchasedWishIds + fulfilledRoomItems)
            .intersect(wishes.map { it.eventId }.toSet())
            .filter { recordCompletedPetWish(it) }
            .toSet()
        wishes = wishes.filterNot { it.eventId in fulfilledIds }

        val activities = gameStateMediator.getApi().petWishActivities()
        val contributions = economyMediator.getApi().getSavingsHistory(
            github.detrig.feature.economy.domain.HistoryFilter(
                types = setOf(github.detrig.feature.economy.domain.FinancialOperationType.TRANSFER_TO_SAVINGS),
            ),
        )
        val fulfilledActivities = wishes.filter { wish ->
            when (wish.kind) {
                PetWishSchedule.Kind.MINI_GAME -> activities.any { activity ->
                    activity.appliedAtMillis >= wish.createdAtMillis &&
                        (activity.source == "pet-wish-play:${wish.productId}" ||
                            (activity.source == "mini-game-launch:${wish.productId}" &&
                                activity.sourceOperationId.toLongOrNull()?.let {
                                    it >= wish.createdAbsoluteDay && it < wish.expiresOnAbsoluteDayExclusive
                                } == true))
                }
                PetWishSchedule.Kind.SAVINGS_TOP_UP -> contributions.any { operation ->
                    val day = operation.context.metadata?.split(';')
                        ?.firstOrNull { it.startsWith("day=") }?.substringAfter('=')?.toLongOrNull()
                    operation.context.reasonId == wish.productId &&
                        operation.timestampMillis >= wish.createdAtMillis &&
                        day != null && day >= wish.createdAbsoluteDay && day < wish.expiresOnAbsoluteDayExclusive
                }
                else -> false
            }
        }.filter { persistCompletedWish(it) }.map { it.eventId }.toSet()
        wishes = wishes.filterNot { it.eventId in fulfilledActivities }
            .filter { it.expiresOnAbsoluteDayExclusive > week.absoluteDay }
            .filterNot { it.kind == PetWishSchedule.Kind.SAVINGS_TOP_UP && it.productId != activeGoalId }

        val newDay = storedDay != week.absoluteDay
        val storedHappiness = if (newDay) game.pet.happiness else
            wishPreferences.getInt("happiness", game.pet.happiness)
        val storedOwnedClothing = if (newDay) currentOwnedClothing else
            wishPreferences.getString("owned_clothing_ids", null)
                ?.split(',')?.filter(String::isNotBlank)?.toSet() ?: currentOwnedClothing
        val storedSafeOptionalRub = if (newDay) safeOptionalRub else
            wishPreferences.getLong("safe_optional_rub", safeOptionalRub)

        if (newDay) {
            val activeProductIds = wishes.mapNotNull { it.productId }.toSet()
            val playableMiniGames = listOf("fishing" to "Рыбалка", "flight" to "Полёт")
                .filter { (gameId, _) -> MiniGameAccess.isOpen(gameId, game.ownedZoneIds) }
            PetWishSchedule.next(
                absoluteDay = week.absoluteDay,
                happiness = storedHappiness,
                clothing = pet.clothingItems(),
                ownedClothingIds = storedOwnedClothing,
                safeOptionalRub = storedSafeOptionalRub,
                playableMiniGames = playableMiniGames,
                roomObjects = interiorWishCandidates(),
                activeWishProductIds = activeProductIds,
            )?.let { wishes = wishes + it.copy(createdAtMillis = System.currentTimeMillis()) }
        }

        val lastTopUpGoalId = wishPreferences.getString("top_up_goal_id", null)
        if (activeGoalId != null && lastTopUpGoalId != activeGoalId && wishes.none {
                it.kind == PetWishSchedule.Kind.SAVINGS_TOP_UP && it.productId == activeGoalId
            }
        ) {
            val goal = savingsMediator.getApi().getActiveGoalProgress()?.goal
            if (goal?.id == activeGoalId) {
                val created = week.absoluteDay
                wishes = wishes + PetWishSchedule.Wish(
                    eventId = "savings-top-up:$activeGoalId:$created",
                    kind = PetWishSchedule.Kind.SAVINGS_TOP_UP,
                    title = "Пополнить копилку",
                    productId = activeGoalId,
                    createdAbsoluteDay = created,
                    expiresOnAbsoluteDayExclusive = created + 2L,
                    createdAtMillis = System.currentTimeMillis(),
                )
                wishPreferences.edit().putString("top_up_goal_id", activeGoalId).commit()
            }
        }

        val editor = wishPreferences.edit()
            .putLong("day", week.absoluteDay)
            .putInt("happiness", storedHappiness)
            .putString("owned_clothing_ids", storedOwnedClothing.sorted().joinToString(","))
            .putLong("safe_optional_rub", storedSafeOptionalRub)
        writeStoredPetWishes(editor, wishes)
        check(editor.commit()) { "Failed to persist pet wish board" }
        wishes.sortedWith(compareByDescending<PetWishSchedule.Wish> { it.createdAbsoluteDay }
            .thenBy { it.eventId })
    }

    suspend fun currentClothingWishId(itemId: String): String? = currentPetWishBoardWishes(
        absoluteDay = weekMediator.getApi().initialize().absoluteDay,
        activeGoalId = savingsMediator.getApi().getActiveGoalProgress()?.goal?.id,
    ).firstOrNull { it.kind == PetWishSchedule.Kind.CLOTHING && it.productId == itemId }
        ?.eventId

    suspend fun currentInteriorPurchaseWishId(productId: String): String? = wishMutex.withLock {
        val day = weekMediator.getApi().initialize().absoluteDay
        val completed = wishPreferences.getStringSet("completed_wishes", emptySet()).orEmpty()
        readStoredPetWishes().firstOrNull { wish ->
            wish.kind == PetWishSchedule.Kind.TOY && wish.productId == productId &&
                wish.eventId !in completed && wish.expiresOnAbsoluteDayExclusive > day
        }?.eventId
    }

    suspend fun recordInteriorPurchaseWishFulfilled(wishId: String) = wishMutex.withLock {
        check(recordCompletedPetWish(wishId)) { "Failed to fulfill interior purchase wish" }
    }

    suspend fun recordRoomWishFulfilled(
        wish: github.detrig.feature.room.domain.model.RoomImpulseWish,
    ) {
        wishMutex.withLock {
            if (wish.kind == github.detrig.feature.room.domain.model.RoomImpulseWish.Kind.TOY) {
                recordCompletedPetWish(wish.eventId)
                return@withLock
            }
        }
    }

    private suspend fun recordCompletedPetWish(eventId: String): Boolean {
        try {
            gameStateMediator.getApi().rewardPetWishHappiness(
                wishRewardId = eventId,
                happinessPoints = PetWishHappinessRewards.optionalPurchase(eventId),
            )
            readStoredPetWishes().firstOrNull { it.eventId == eventId }?.let {
                if (!persistCompletedWish(it)) return false
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // The purchase state is durable; the next wish-board refresh retries the reward.
            return false
        }
        return true
    }

    private fun persistCompletedWish(wish: PetWishSchedule.Wish): Boolean {
        val completed = wishPreferences.getStringSet("completed_wishes", emptySet()).orEmpty().toSet()
        val editor = wishPreferences.edit()
            .putStringSet("completed_wishes", completed + wish.eventId)
            .putString("completed_title:${wish.eventId}", wish.title)
            .putString("completed_kind:${wish.eventId}", wish.kind.name)
            .putLong("completed_at:${wish.eventId}", System.currentTimeMillis())
        writeStoredPetWishes(editor, readStoredPetWishes().filterNot { it.eventId == wish.eventId })
        return editor.commit()
    }

    suspend fun currentWishFulfillments(): List<RoomWishFulfillment> = wishMutex.withLock {
        val acknowledged = wishPreferences.getStringSet("celebrated_wishes", emptySet()).orEmpty()
        val rewards = gameStateMediator.getApi().petWishActivities().mapNotNull { activity ->
            val kind = when {
                activity.source == "pet-wish-game-unlocked" -> RoomWishFulfillment.Kind.GAME_UNLOCKED
                activity.source != "pet-wish-happiness" -> return@mapNotNull null
                activity.sourceOperationId.startsWith("savings-halfway:") -> RoomWishFulfillment.Kind.SAVINGS_HALF_WAY
                activity.sourceOperationId.startsWith("savings-goal-reached:") -> RoomWishFulfillment.Kind.SAVINGS_REACHED
                else -> RoomWishFulfillment.Kind.ITEM
            }
            activity.appliedAtMillis to RoomWishFulfillment(
                id = activity.id,
                kind = kind,
                title = wishPreferences.getString("completed_title:${activity.sourceOperationId}", "").orEmpty(),
            )
        }
        val otherWishes = wishPreferences.getStringSet("completed_wishes", emptySet()).orEmpty()
            .mapNotNull { id ->
                val kind = when (wishPreferences.getString("completed_kind:$id", null)) {
                    PetWishSchedule.Kind.MINI_GAME.name -> RoomWishFulfillment.Kind.MINI_GAME
                    PetWishSchedule.Kind.SAVINGS_TOP_UP.name -> RoomWishFulfillment.Kind.SAVINGS_TOP_UP
                    else -> return@mapNotNull null
                }
                wishPreferences.getLong("completed_at:$id", 0L) to RoomWishFulfillment(
                    id = "pet-wish-completed:$id",
                    kind = kind,
                    title = wishPreferences.getString("completed_title:$id", "").orEmpty(),
                )
            }
        (rewards + otherWishes).sortedWith(compareBy({ it.first }, { it.second.id }))
            .map { it.second }.filterNot { it.id in acknowledged }
    }

    suspend fun acknowledgeWishFulfillment(id: String) = wishMutex.withLock {
        val acknowledged = wishPreferences.getStringSet("celebrated_wishes", emptySet()).orEmpty().toSet()
        check(wishPreferences.edit().putStringSet("celebrated_wishes", acknowledged + id).commit()) {
            "Failed to acknowledge pet wish celebration"
        }
    }

    private fun readStoredPetWishes(): List<PetWishSchedule.Wish> = runCatching {
        val array = org.json.JSONArray(wishPreferences.getString("wish_board", "[]") ?: "[]")
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            PetWishSchedule.Wish(
                eventId = item.getString("id"),
                kind = PetWishSchedule.Kind.valueOf(item.getString("kind")),
                title = item.getString("title"),
                priceRub = item.optLong("price"),
                productId = item.optString("product").takeIf(String::isNotBlank),
                createdAbsoluteDay = item.getLong("created"),
                expiresOnAbsoluteDayExclusive = item.getLong("expires"),
                createdAtMillis = item.optLong("created_at"),
            )
        }
    }.getOrDefault(emptyList())

    private fun writeStoredPetWishes(
        editor: android.content.SharedPreferences.Editor,
        wishes: List<PetWishSchedule.Wish>,
    ) {
        val array = org.json.JSONArray()
        wishes.distinctBy { it.eventId }.forEach { wish ->
            array.put(org.json.JSONObject()
                .put("id", wish.eventId)
                .put("kind", wish.kind.name)
                .put("title", wish.title)
                .put("price", wish.priceRub)
                .put("product", wish.productId)
                .put("created", wish.createdAbsoluteDay)
                .put("expires", wish.expiresOnAbsoluteDayExclusive)
                .put("created_at", wish.createdAtMillis))
        }
        editor.putString("wish_board", array.toString())
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
            github.detrig.feature.room.domain.model.RoomImpulseWish.Kind.MINI_GAME,
            github.detrig.feature.room.domain.model.RoomImpulseWish.Kind.TOY,
            github.detrig.feature.room.domain.model.RoomImpulseWish.Kind.SAVINGS_TOP_UP,
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
        val wish = currentPetWishBoardWishes(
            absoluteDay = week.absoluteDay,
            activeGoalId = savingsMediator.getApi().getActiveGoalProgress()?.goal?.id,
        ).firstOrNull { it.kind == PetWishSchedule.Kind.GROCERY } ?: return null
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
    }
}

private fun PurchaseProblem?.toShopFeedback(): ShopPurchaseFeedback? = when (this) {
    PurchaseProblem.REQUIRED_FOOD_MISSING -> ShopPurchaseFeedback.REQUIRED_FOOD_MISSING
    PurchaseProblem.MANDATORY_MONEY_AT_RISK -> ShopPurchaseFeedback.MANDATORY_MONEY_AT_RISK
    PurchaseProblem.RESERVE_AT_RISK -> ShopPurchaseFeedback.RESERVE_AT_RISK
    PurchaseProblem.PROMOTION_OVERBUY -> ShopPurchaseFeedback.PROMOTION_OVERBUY
    PurchaseProblem.TOO_MANY_EXTRAS -> ShopPurchaseFeedback.TOO_MANY_EXTRAS
    null -> null
}
