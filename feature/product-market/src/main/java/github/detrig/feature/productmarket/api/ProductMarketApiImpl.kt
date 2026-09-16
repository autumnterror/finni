package github.detrig.feature.productmarket.api

import github.detrig.core.presentation.navigation.v3.EntryHostProviderInstaller
import github.detrig.core.presentation.navigation.v3.composable
import github.detrig.feature.productmarket.domain.MarketTripRepository
import github.detrig.feature.productmarket.navigation.MarketRoute
import github.detrig.feature.productmarket.navigation.MarketRouter
import github.detrig.feature.productmarket.presentation.MarketScreen
import github.detrig.products.ProductCatalog

internal class ProductMarketApiImpl(
    private val router: MarketRouter,
    private val catalog: ProductCatalog,
    private val repository: MarketTripRepository,
) : ProductMarketApi {
    override fun open() = router.open()
    override fun entries(): EntryHostProviderInstaller = {
        composable<MarketRoute.Home> { MarketScreen() }
    }
    override fun productCatalog() = catalog
    override fun observeLastResult() = repository.observeLastResult()
}
