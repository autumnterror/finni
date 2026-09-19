package github.detrig.feature.shop.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface ShopRoute : NavKey {
    @Serializable
    data class Catalog(val storeId: String) : ShopRoute
}
