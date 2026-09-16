package github.detrig.feature.gamesession.navigation

import github.detrig.core.presentation.navigation.v3.Nav3HostGraphSpec
import github.detrig.core.presentation.navigation.v3.composable
import github.detrig.feature.gamesession.presentation.GameSessionScreen

fun gameSessionGraph(): Nav3HostGraphSpec {
    return Nav3HostGraphSpec(
        startRoute = GameSessionRoute.Home,
    ) {
        composable<GameSessionRoute.Home> {
            GameSessionScreen()
        }
    }
}
