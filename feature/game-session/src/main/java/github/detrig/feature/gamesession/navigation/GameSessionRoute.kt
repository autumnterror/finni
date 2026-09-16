package github.detrig.feature.gamesession.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface GameSessionRoute : NavKey {

    @Serializable
    data object Home : GameSessionRoute
}
