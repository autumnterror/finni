package github.detrig.feature.shop.di

import github.detrig.feature.shop.ShopDependencies
import github.detrig.feature.shop.api.ShopApi
import github.detrig.feature.shop.api.ShopApiImpl
import github.detrig.feature.shop.navigation.ShopRouterImpl
import github.detrig.feature.shop.presentation.ShopViewModel
import github.detrig.products.StoreId

internal class ShopModule(
    private val dependencies: ShopDependencies,
) : ShopComponent {
    private val router by lazy { ShopRouterImpl(dependencies.globalNavigator()) }

    override val artworkResolver by lazy { dependencies.artworkResolver() }
    override val itemDetailsResolver by lazy { dependencies.itemDetailsResolver() }

    override val api: ShopApi by lazy { ShopApiImpl(router) }

    override fun viewModel(storeId: StoreId) = ShopViewModel(
        storeId = storeId,
        catalogRegistry = dependencies.catalogRegistry(),
        host = dependencies.host(),
        router = router,
    )
}
