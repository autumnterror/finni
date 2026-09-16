package github.detrig.feature.productmarket.di

import github.detrig.feature.productmarket.ProductMarketDependencies
import github.detrig.feature.productmarket.api.ProductMarketApi
import github.detrig.feature.productmarket.api.ProductMarketApiImpl
import github.detrig.feature.productmarket.data.MarketTripCodec
import github.detrig.feature.productmarket.data.MarketTripRepositoryImpl
import github.detrig.feature.productmarket.domain.*
import github.detrig.feature.productmarket.navigation.MarketRouterImpl
import github.detrig.feature.productmarket.presentation.MarketViewModel

internal class ProductMarketModule(private val dependencies: ProductMarketDependencies) : ProductMarketComponent {
    private val catalog by lazy { dependencies.productCatalog() }
    private val rules by lazy { MarketRules(dependencies.configuration(), catalog) }
    private val repository by lazy {
        MarketTripRepositoryImpl(dependencies.tripDao(), MarketTripCodec(), dependencies.applicationScope())
    }
    private val router by lazy { MarketRouterImpl(dependencies.globalNavigator()) }
    override val api: ProductMarketApi by lazy { ProductMarketApiImpl(router, catalog, repository) }
    override fun viewModel() = MarketViewModel(
        rules, catalog, repository,
        RestoreMarketTripInteractor(repository, rules),
        ObserveMarketBalanceInteractor(dependencies.host()),
        FinishMarketTripInteractor(repository, rules), router,
    )
}
