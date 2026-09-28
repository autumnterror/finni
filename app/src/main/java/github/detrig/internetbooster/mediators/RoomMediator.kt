package github.detrig.internetbooster.mediators

import android.content.res.Resources
import github.detrig.core.Mediator
import github.detrig.core.di.CoreComponent
import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.core.audio.GameAudio
import github.detrig.core.presentation.message.GlobalMessageController
import github.detrig.feature.gamestate.api.GameStateApi
import github.detrig.feature.room.RoomDependencies
import github.detrig.feature.room.RoomFeature
import github.detrig.feature.room.api.RoomApi
import github.detrig.feature.room.api.RoomGameLauncher
import github.detrig.internetbooster.navigation.RoomGameLauncherImpl
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.room.domain.model.RoomImpulseWish
import github.detrig.feature.room.domain.model.RoomImpulseWishSource

internal class RoomMediator(
    private val coreComponent: CoreComponent,
    private val gameStateMediator: GameStateMediator,
    private val economyMediator: EconomyMediator,
    private val weekMediator: WeekMediator,
    private val planningMediator: PlanningMediator,
    private val learningMediator: LearningMediator,
    private val savingsMediator: SavingsMediator,
    private val shopMediator: ShopMediator,
    private val petMediator: PetMediator,
    private val wardrobeMediator: WardrobeMediator,
    private val inventoryMediator: InventoryMediator,
    private val gameAudio: GameAudio,
    private val learningTestsMediator: LearningTestsMediator,
    private val resetDemoProgress: suspend (skipOnboarding: Boolean) -> Unit,
) : Mediator<RoomApi> {
    fun init() {
        RoomFeature.dependenciesProvider = ModuleDependenciesProvider {
            object : RoomDependencies {
                override fun housePreferences() = coreComponent.context.getSharedPreferences("finpet_house", android.content.Context.MODE_PRIVATE)
                override fun gameStateApi(): GameStateApi = gameStateMediator.getApi()
                override fun economyApi(): EconomyApi = economyMediator.getApi()
                override fun weekApi(): github.detrig.feature.week.api.WeekApi = weekMediator.getApi()
                override fun planningApi(): github.detrig.feature.planning.api.PlanningApi = planningMediator.getApi()
                override fun learningApi(): github.detrig.feature.learning.api.LearningApi = learningMediator.getApi()
                override fun savingsApi(): github.detrig.feature.savings.api.SavingsApi = savingsMediator.getApi()
                override fun inventoryApi() = inventoryMediator.getApi()
                override fun marketLauncher() = github.detrig.feature.room.api.RoomMarketLauncher {
                    shopMediator.getApi().open(github.detrig.products.GroceryStoreIds.Store)
                }
                override fun wardrobeLauncher() = github.detrig.feature.room.api.RoomWardrobeLauncher {
                    wardrobeMediator.getApi().open()
                }
                override fun testsLauncher() = github.detrig.feature.room.api.RoomTestsLauncher {
                    learningTestsMediator.getApi().open()
                }
                override fun impulseWishSource() = object : RoomImpulseWishSource {
                    override suspend fun currentWish(): RoomImpulseWish? {
                        val scheduled = shopMediator.currentPetWish() ?: return null
                        return scheduled.toRoomImpulseWish()
                    }

                    override suspend fun currentWishes(
                        absoluteDay: Long,
                        activeGoalId: String?,
                    ): List<RoomImpulseWish> = shopMediator.currentPetWishBoardWishes(
                        absoluteDay = absoluteDay,
                        activeGoalId = activeGoalId,
                    ).map { it.toRoomImpulseWish() }

                    override suspend fun claimDialogueWish(wish: RoomImpulseWish): RoomImpulseWish? =
                        shopMediator.claimRoomWishDialogue(wish)

                    override suspend fun recordDeclined(wish: RoomImpulseWish) {
                        shopMediator.recordRoomWishDeclined(wish)
                    }

                    override suspend fun recordFulfilled(wish: RoomImpulseWish) {
                        shopMediator.recordRoomWishFulfilled(wish)
                    }

                    override suspend fun purchaseWishId(productId: String) =
                        shopMediator.currentInteriorPurchaseWishId(productId)

                    override suspend fun recordPurchaseFulfilled(wishId: String) {
                        shopMediator.recordInteriorPurchaseWishFulfilled(wishId)
                    }

                    override suspend fun currentFulfillments() = shopMediator.currentWishFulfillments()

                    override suspend fun acknowledgeFulfillment(id: String) {
                        shopMediator.acknowledgeWishFulfillment(id)
                    }
                }
                override fun wishArtwork() = GameRoomWishArtwork(
                    shopMediator.artworkResolver(), petMediator.getApi())
                override fun globalMessageController(): GlobalMessageController = coreComponent.globalMessageController
                override fun resources(): Resources = coreComponent.resources
                override fun gameAudio() = gameAudio
                override fun minimumProductPriceRub(): Long = shopMediator.minimumGroceryPriceRub()
                override suspend fun resetDemoProgress(skipOnboarding: Boolean) =
                    this@RoomMediator.resetDemoProgress(skipOnboarding)
                override fun gameLauncher(): RoomGameLauncher =
                    RoomGameLauncherImpl(coreComponent.globalMessageController, coreComponent.resources)
            }
        }
        // The room is the persistent hub: warm its assets before the first navigation to it.
        RoomFeature.getApi()
    }

    override fun getApi(): RoomApi = RoomFeature.getApi()

    private fun PetWishSchedule.Wish.toRoomImpulseWish() = RoomImpulseWish(
        eventId = eventId,
        productTitle = title,
        phraseVariant = Math.floorMod(eventId.hashCode(), RoomImpulseWish.PHRASE_VARIANT_COUNT),
        showIntroduction = false,
        kind = when (kind) {
            PetWishSchedule.Kind.GROCERY -> RoomImpulseWish.Kind.GROCERY
            PetWishSchedule.Kind.CLOTHING -> RoomImpulseWish.Kind.CLOTHING
            PetWishSchedule.Kind.MINI_GAME -> RoomImpulseWish.Kind.MINI_GAME
            PetWishSchedule.Kind.TOY -> RoomImpulseWish.Kind.TOY
            PetWishSchedule.Kind.SAVINGS_TOP_UP -> RoomImpulseWish.Kind.SAVINGS_TOP_UP
        },
        priceRub = priceRub,
        productId = productId,
        createdOnAbsoluteDay = createdAbsoluteDay,
        expiresOnAbsoluteDayExclusive = expiresOnAbsoluteDayExclusive,
    )
}
