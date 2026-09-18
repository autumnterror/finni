package github.detrig.feature.savings

import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.core.di.diDemand
import github.detrig.feature.savings.api.SavingsApi
import github.detrig.feature.savings.di.SavingsComponent
import github.detrig.feature.savings.di.SavingsModule

object SavingsFeature {
    var dependenciesProvider: ModuleDependenciesProvider<SavingsDependencies>? = null
    private var component: SavingsComponent? by diDemand {
        SavingsModule(requireNotNull(dependenciesProvider?.getDependencies()))
    }

    fun getApi(): SavingsApi = component().api
    internal fun component(): SavingsComponent = requireNotNull(component)
}
