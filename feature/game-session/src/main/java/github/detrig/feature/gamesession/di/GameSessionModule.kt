package github.detrig.feature.gamesession.di

import github.detrig.feature.gamesession.GameSessionDependencies
import github.detrig.feature.gamesession.api.GameSessionApi
import github.detrig.feature.gamesession.api.GameSessionApiImpl
import github.detrig.feature.gamesession.domain.interactor.ObserveGameStateInteractor
import github.detrig.feature.gamesession.navigation.GameSessionRouter
import github.detrig.feature.gamesession.navigation.GameSessionRouterImpl
import github.detrig.feature.gamesession.presentation.GameSessionViewModel
import github.detrig.feature.pet.api.PetApi
import github.detrig.feature.room.api.RoomApi
import github.detrig.feature.phone.api.PhoneApi
import github.detrig.feature.fridge.api.FridgeApi

internal class GameSessionModule(
    private val dependencies: GameSessionDependencies,
) : GameSessionComponent {

    override val roomApi: RoomApi by lazy { dependencies.roomApi() }
    override val petApi: PetApi by lazy { dependencies.petApi() }
    override val phoneApi: PhoneApi by lazy { dependencies.phoneApi() }
    override val fridgeApi: FridgeApi by lazy { dependencies.fridgeApi() }

    override val api: GameSessionApi by lazy {
        GameSessionApiImpl(router)
    }

    override val router: GameSessionRouter by lazy {
        GameSessionRouterImpl(
            navigator = dependencies.globalNavigator(),
            messageController = dependencies.globalMessageController(),
        )
    }

    private val observeGameStateInteractor by lazy {
        ObserveGameStateInteractor(dependencies.gameStateApi(), dependencies.economyApi())
    }

    override fun getGameSessionViewModel(): GameSessionViewModel {
        return GameSessionViewModel(
            observeGameStateInteractor = observeGameStateInteractor,
            router = router,
        )
    }
}
