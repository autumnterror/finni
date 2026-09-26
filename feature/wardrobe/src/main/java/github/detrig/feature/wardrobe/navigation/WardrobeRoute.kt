package github.detrig.feature.wardrobe.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface WardrobeRoute : NavKey {
    @Serializable
    data object Home : WardrobeRoute
}
