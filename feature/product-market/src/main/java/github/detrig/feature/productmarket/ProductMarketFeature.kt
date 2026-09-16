package github.detrig.feature.productmarket

import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.core.di.diDemand
import github.detrig.feature.productmarket.api.ProductMarketApi
import github.detrig.feature.productmarket.di.ProductMarketComponent
import github.detrig.feature.productmarket.di.ProductMarketModule

object ProductMarketFeature {
    var dependenciesProvider: ModuleDependenciesProvider<ProductMarketDependencies>? = null
    private var component: ProductMarketComponent? by diDemand {
        ProductMarketModule(requireNotNull(dependenciesProvider?.getDependencies()))
    }
    fun getApi(): ProductMarketApi = component().api
    internal fun component(): ProductMarketComponent = requireNotNull(component)
    internal fun destroyComponent() { component = null }
}
