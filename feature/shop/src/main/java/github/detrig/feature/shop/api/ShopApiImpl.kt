package github.detrig.feature.shop.api

import github.detrig.core.presentation.navigation.v3.EntryHostProviderInstaller
import github.detrig.core.presentation.navigation.v3.composable
import github.detrig.feature.shop.navigation.ShopRoute
import github.detrig.feature.shop.navigation.ShopRouter
import github.detrig.feature.shop.presentation.ShopScreen
import github.detrig.products.StoreId

internal class ShopApiImpl(
    private val router: ShopRouter,
) : ShopApi {
    override fun open(storeId: StoreId) = router.open(storeId)

    override fun entries(): EntryHostProviderInstaller = {
        composable<ShopRoute.Catalog> { route -> ShopScreen(StoreId(route.storeId)) }
    }
}
