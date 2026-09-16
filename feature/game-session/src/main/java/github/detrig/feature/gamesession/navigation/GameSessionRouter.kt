package github.detrig.feature.gamesession.navigation

internal interface GameSessionRouter {

    fun openGameSession()

    fun showMessage(message: String)

    fun showErrorMessage(message: String)

    fun back()
}
