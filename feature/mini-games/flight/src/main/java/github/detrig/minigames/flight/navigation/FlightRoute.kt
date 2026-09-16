package github.detrig.minigames.flight.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface FlightRoute : NavKey {
    @Serializable data object Play : FlightRoute
}
