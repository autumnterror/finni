package github.detrig.feature.inventory

import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.core.di.diDemand
import github.detrig.feature.inventory.api.InventoryApi
import github.detrig.feature.inventory.di.InventoryComponent
import github.detrig.feature.inventory.di.InventoryModule

object InventoryFeature {
    var dependenciesProvider: ModuleDependenciesProvider<InventoryDependencies>? = null

    private var component: InventoryComponent? by diDemand {
        InventoryModule(requireNotNull(dependenciesProvider?.getDependencies()))
    }

    fun getApi(): InventoryApi = requireNotNull(component).api

    internal fun component(): InventoryComponent = requireNotNull(component)

    internal fun destroyComponent() {
        component = null
    }
}
