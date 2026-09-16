package github.detrig.minigames.flight

import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.core.di.diDemand
import github.detrig.minigames.flight.api.FlightApi
import github.detrig.minigames.flight.di.FlightComponent
import github.detrig.minigames.flight.di.FlightModule

object FlightFeature {
    var dependenciesProvider: ModuleDependenciesProvider<FlightDependencies>? = null
    private var component: FlightComponent? by diDemand {
        FlightModule(requireNotNull(dependenciesProvider?.getDependencies()))
    }
    fun getApi(): FlightApi = requireNotNull(component).api
    internal fun component(): FlightComponent = requireNotNull(component)
    internal fun destroyComponent() { component = null }
}
