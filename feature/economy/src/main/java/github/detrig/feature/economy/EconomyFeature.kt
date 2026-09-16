package github.detrig.feature.economy

import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.core.di.diDemand
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.economy.di.EconomyComponent
import github.detrig.feature.economy.di.EconomyModule

object EconomyFeature {
    var dependenciesProvider: ModuleDependenciesProvider<EconomyDependencies>? = null

    private var component: EconomyComponent? by diDemand {
        EconomyModule(requireNotNull(dependenciesProvider?.getDependencies()))
    }

    fun getApi(): EconomyApi = requireNotNull(component).api
    internal fun destroyComponent() { component = null }
}
