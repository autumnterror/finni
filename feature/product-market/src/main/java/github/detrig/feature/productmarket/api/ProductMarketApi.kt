package github.detrig.feature.productmarket.api

import github.detrig.core.presentation.navigation.v3.EntryHostProviderInstaller
import github.detrig.products.ProductCatalog
import kotlinx.coroutines.flow.Flow

interface ProductMarketApi {
    fun open()
    fun entries(): EntryHostProviderInstaller
    fun productCatalog(): ProductCatalog
    fun observeLastResult(): Flow<MarketTripResult?>
}
