package github.detrig.feature.shop

import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.core.di.diDemand
import github.detrig.feature.shop.api.ShopApi
import github.detrig.feature.shop.di.ShopComponent
import github.detrig.feature.shop.di.ShopModule

object ShopFeature {
    var dependenciesProvider: ModuleDependenciesProvider<ShopDependencies>? = null

    private var component: ShopComponent? by diDemand {
        ShopModule(requireNotNull(dependenciesProvider?.getDependencies()))
    }

    fun getApi(): ShopApi = component().api

    internal fun component(): ShopComponent = requireNotNull(component)

    internal fun destroyComponent() {
        component = null
    }
}
