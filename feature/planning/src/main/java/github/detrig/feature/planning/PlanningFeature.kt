package github.detrig.feature.planning

import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.core.di.diDemand
import github.detrig.feature.planning.api.PlanningApi
import github.detrig.feature.planning.di.PlanningComponent
import github.detrig.feature.planning.di.PlanningModule

object PlanningFeature {
    var dependenciesProvider: ModuleDependenciesProvider<PlanningDependencies>? = null

    private var component: PlanningComponent? by diDemand {
        PlanningModule(requireNotNull(dependenciesProvider?.getDependencies()))
    }

    fun getApi(): PlanningApi = requireNotNull(component).api
}
