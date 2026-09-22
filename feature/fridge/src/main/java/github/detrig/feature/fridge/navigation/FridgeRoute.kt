package github.detrig.feature.fridge.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface FridgeRoute : NavKey {
    @Serializable
    data object Home : FridgeRoute

    @Serializable
    data object Feeding : FridgeRoute
}
