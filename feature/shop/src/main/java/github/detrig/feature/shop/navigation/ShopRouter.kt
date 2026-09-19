package github.detrig.feature.shop.navigation

import github.detrig.core.presentation.navigation.GlobalNavigator
import github.detrig.products.StoreId

internal interface ShopRouter {
    fun open(storeId: StoreId)
    fun openCart(storeId: StoreId)
    fun back()
    fun closeToRoom()
}

internal class ShopRouterImpl(
    private val navigator: GlobalNavigator,
) : ShopRouter {
    override fun open(storeId: StoreId) {
        navigator.navigate(ShopRoute.Catalog(storeId.value), launchSingleTop = true)
    }

    override fun openCart(storeId: StoreId) {
        navigator.navigate(ShopRoute.Cart(storeId.value), launchSingleTop = true)
    }

    override fun back() = navigator.back()

    override fun closeToRoom() = navigator.backToHostStartRoute()
}
