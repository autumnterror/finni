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

internal class RoomMediator(
    private val coreComponent: CoreComponent,
    private val gameStateMediator: GameStateMediator,
    private val economyMediator: EconomyMediator,
) : Mediator<RoomApi> {
    fun init() {
        RoomFeature.dependenciesProvider = ModuleDependenciesProvider {
            object : RoomDependencies {
                override fun housePreferences() = coreComponent.context.getSharedPreferences("finpet_house", android.content.Context.MODE_PRIVATE)
                override fun gameStateApi(): GameStateApi = gameStateMediator.getApi()
                override fun economyApi(): EconomyApi = economyMediator.getApi()
                override fun marketLauncher() = github.detrig.feature.room.api.RoomMarketLauncher {
                    github.detrig.feature.productmarket.ProductMarketFeature.getApi().open()
                }
                override fun globalMessageController(): GlobalMessageController = coreComponent.globalMessageController
                override fun resources(): Resources = coreComponent.resources
                override fun gameLauncher(): RoomGameLauncher =
                    RoomGameLauncherImpl(coreComponent.globalMessageController, coreComponent.resources)
            }
        }
    }

    override fun getApi(): RoomApi = RoomFeature.getApi()
}
