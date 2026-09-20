package github.detrig.feature.phone.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface PhoneRoute : NavKey {
    @Serializable
    data object Home : PhoneRoute

    @Serializable
    data class App(val appId: String) : PhoneRoute
}
