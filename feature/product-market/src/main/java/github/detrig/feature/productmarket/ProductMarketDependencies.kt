package github.detrig.feature.productmarket

import github.detrig.core.presentation.navigation.GlobalNavigator
import github.detrig.feature.productmarket.api.ProductMarketHost
import github.detrig.feature.productmarket.data.MarketTripDao
import github.detrig.feature.productmarket.domain.MarketConfiguration
import github.detrig.products.ProductCatalog
import kotlinx.coroutines.CoroutineScope

interface ProductMarketDependencies {
    fun host(): ProductMarketHost
    fun tripDao(): MarketTripDao
    fun productCatalog(): ProductCatalog
    fun configuration(): MarketConfiguration
    fun applicationScope(): CoroutineScope
    fun globalNavigator(): GlobalNavigator
}
