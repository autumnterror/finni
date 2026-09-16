package github.detrig.minigames.fishing.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface FishingRoute : NavKey {
    @Serializable data object Home : FishingRoute
}
