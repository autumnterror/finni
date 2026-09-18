package github.detrig.feature.week

import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.core.di.diDemand
import github.detrig.feature.week.api.WeekApi
import github.detrig.feature.week.di.WeekComponent
import github.detrig.feature.week.di.WeekModule

object WeekFeature {
    var dependenciesProvider: ModuleDependenciesProvider<WeekDependencies>? = null

    private var component: WeekComponent? by diDemand {
        WeekModule(requireNotNull(dependenciesProvider?.getDependencies()))
    }

    fun getApi(): WeekApi = requireNotNull(component).api
}
