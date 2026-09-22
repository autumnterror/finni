package github.detrig.feature.fridge

import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.core.di.diDemand
import github.detrig.feature.fridge.api.FridgeApi
import github.detrig.feature.fridge.di.FridgeComponent
import github.detrig.feature.fridge.di.FridgeModule

object FridgeFeature {
    var dependenciesProvider: ModuleDependenciesProvider<FridgeDependencies>? = null

    private var component: FridgeComponent? by diDemand {
        FridgeModule(requireNotNull(dependenciesProvider?.getDependencies()))
    }

    fun getApi(): FridgeApi = requireNotNull(component).api

    internal fun component(): FridgeComponent = requireNotNull(component)

    internal fun destroyComponent() {
        component = null
    }
}
