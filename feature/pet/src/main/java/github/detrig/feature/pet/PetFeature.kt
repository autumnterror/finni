package github.detrig.feature.pet

import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.core.di.diDemand
import github.detrig.feature.pet.api.PetApi
import github.detrig.feature.pet.di.PetComponent
import github.detrig.feature.pet.di.PetModule

object PetFeature {
    var dependenciesProvider: ModuleDependenciesProvider<PetDependencies>? = null

    private var component: PetComponent? by diDemand {
        PetModule(requireNotNull(dependenciesProvider?.getDependencies()))
    }

    fun getApi(): PetApi = requireNotNull(component).api
    internal fun component(): PetComponent = requireNotNull(component)
    internal fun destroyComponent() { component = null }
}
