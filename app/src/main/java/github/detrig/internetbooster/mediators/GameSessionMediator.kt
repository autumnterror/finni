package github.detrig.internetbooster.mediators

import androidx.annotation.MainThread
import github.detrig.core.Mediator
import github.detrig.core.di.CoreComponent
import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.core.presentation.message.GlobalMessageController
import github.detrig.core.presentation.navigation.GlobalNavigator
import github.detrig.feature.gamesession.GameSessionDependencies
import github.detrig.feature.gamesession.GameSessionFeature
import github.detrig.feature.gamesession.api.GameSessionApi
import github.detrig.feature.gamestate.api.GameStateApi
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.pet.api.PetApi
import github.detrig.feature.room.api.RoomApi
import github.detrig.feature.phone.api.PhoneApi
import github.detrig.feature.fridge.api.FridgeApi

internal class GameSessionMediator(
    private val coreComponent: CoreComponent,
    private val gameStateMediator: GameStateMediator,
    private val economyMediator: EconomyMediator,
    private val roomMediator: RoomMediator,
    private val petMediator: PetMediator,
    private val phoneMediator: PhoneMediator,
    private val fridgeMediator: FridgeMediator,
) : Mediator<GameSessionApi> {

    @MainThread
    fun init() {
        GameSessionFeature.dependenciesProvider = ModuleDependenciesProvider {
            GameSessionDependenciesImpl(
                coreComponent = coreComponent,
                gameStateApi = gameStateMediator.getApi(),
                economyApi = economyMediator.getApi(),
                roomApi = roomMediator.getApi(),
                petApi = petMediator.getApi(),
                phoneApi = phoneMediator.getApi(),
                fridgeApi = fridgeMediator.getApi(),
            )
        }
    }

    @MainThread
    override fun getApi(): GameSessionApi {
        return GameSessionFeature.getApi()
    }
}

private class GameSessionDependenciesImpl(
    private val coreComponent: CoreComponent,
    private val gameStateApi: GameStateApi,
    private val economyApi: EconomyApi,
    private val roomApi: RoomApi,
    private val petApi: PetApi,
    private val phoneApi: PhoneApi,
    private val fridgeApi: FridgeApi,
) : GameSessionDependencies {

    override fun globalNavigator(): GlobalNavigator {
        return coreComponent.globalNavigator
    }

    override fun globalMessageController(): GlobalMessageController {
        return coreComponent.globalMessageController
    }

    override fun gameStateApi(): GameStateApi {
        return gameStateApi
    }

    override fun petApi(): PetApi = petApi

    override fun roomApi(): RoomApi = roomApi

    override fun economyApi(): EconomyApi = economyApi

    override fun phoneApi(): PhoneApi = phoneApi

    override fun fridgeApi(): FridgeApi = fridgeApi
}
