package github.detrig.feature.productmarket.di

import github.detrig.feature.productmarket.api.ProductMarketApi
import github.detrig.feature.productmarket.presentation.MarketViewModel

internal interface ProductMarketComponent {
    val api: ProductMarketApi
    fun viewModel(): MarketViewModel
}
