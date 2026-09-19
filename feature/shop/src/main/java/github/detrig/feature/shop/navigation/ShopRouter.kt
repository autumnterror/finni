package github.detrig.feature.shop.navigation

import github.detrig.core.presentation.navigation.GlobalNavigator
import github.detrig.products.StoreId

internal interface ShopRouter {
    fun open(storeId: StoreId)
    fun back()
}

internal class ShopRouterImpl(
    private val navigator: GlobalNavigator,
) : ShopRouter {
    override fun open(storeId: StoreId) {
        navigator.navigate(ShopRoute.Catalog(storeId.value), launchSingleTop = true)
    }

    override fun back() = navigator.back()
}
