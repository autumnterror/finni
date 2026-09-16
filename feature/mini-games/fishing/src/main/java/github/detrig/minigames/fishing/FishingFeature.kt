package github.detrig.minigames.fishing

import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.core.di.diDemand
import github.detrig.minigames.fishing.api.FishingApi
import github.detrig.minigames.fishing.di.FishingComponent
import github.detrig.minigames.fishing.di.FishingModule

object FishingFeature {
    var dependenciesProvider: ModuleDependenciesProvider<FishingDependencies>? = null
    private var component: FishingComponent? by diDemand {
        FishingModule(requireNotNull(dependenciesProvider?.getDependencies()))
    }
    fun getApi(): FishingApi = component().api
    internal fun component(): FishingComponent = requireNotNull(component)
    internal fun destroyComponent() { component = null }
}
