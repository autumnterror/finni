package github.detrig.feature.productmarket.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface MarketRoute : NavKey {
    @Serializable data object Home : MarketRoute
}
