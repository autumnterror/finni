package github.detrig.feature.gamesession.api

import github.detrig.feature.gamesession.navigation.GameSessionRouter

internal class GameSessionApiImpl(
    private val router: GameSessionRouter,
) : GameSessionApi {

    override fun openGameSession() {
        router.openGameSession()
    }
}
