package github.detrig.internetbooster.mediators

import android.content.res.Resources
import github.detrig.core.Mediator
import github.detrig.core.di.CoreComponent
import github.detrig.core.di.ModuleDependenciesProvider
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
                override fun marketLauncher() = github.detrig.feature.room.api.RoomMarketLauncher {
                    shopMediator.getApi().open(github.detrig.products.GroceryStoreIds.Store)
                }
                override fun impulseWishSource() = RoomImpulseWishSource {
                    shopMediator.claimRoomImpulseWish()?.let { wish ->
                        RoomImpulseWish(
                            eventId = wish.eventId,
                            productTitle = wish.productTitle,
                            phraseVariant = wish.phraseVariant,
                            showIntroduction = wish.showIntroduction,
                        )
                    }
                }
                override fun globalMessageController(): GlobalMessageController = coreComponent.globalMessageController
                override fun resources(): Resources = coreComponent.resources
                override fun minimumProductPriceRub(): Long = shopMediator.minimumGroceryPriceRub()
                override fun gameLauncher(): RoomGameLauncher =
                    RoomGameLauncherImpl(coreComponent.globalMessageController, coreComponent.resources)
            }
        }
        // The room is the persistent hub: warm its assets before the first navigation to it.
        RoomFeature.getApi()
    }

    override fun getApi(): RoomApi = RoomFeature.getApi()
}
