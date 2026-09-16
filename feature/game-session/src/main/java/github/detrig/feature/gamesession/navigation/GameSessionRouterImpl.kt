package github.detrig.feature.gamesession.navigation

import github.detrig.core.presentation.message.GlobalMessageController
import github.detrig.core.presentation.navigation.GlobalNavigator

internal class GameSessionRouterImpl(
    private val navigator: GlobalNavigator,
    private val messageController: GlobalMessageController,
) : GameSessionRouter {

    override fun openGameSession() {
        navigator.openNewStartRoute(GameSessionRoute.Home)
    }

    override fun showMessage(message: String) {
        messageController.showMessage(message)
    }

    override fun showErrorMessage(message: String) {
        messageController.showErrorMessage(message)
    }

    override fun back() {
        navigator.back()
    }
}
