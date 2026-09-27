package github.detrig.feature.learningtests

import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.core.di.diDemand
import github.detrig.feature.learningtests.api.LearningTestsApi
import github.detrig.feature.learningtests.di.LearningTestsComponent
import github.detrig.feature.learningtests.di.LearningTestsModule

object LearningTestsFeature {
    var dependenciesProvider: ModuleDependenciesProvider<LearningTestsDependencies>? = null

    private var component: LearningTestsComponent? by diDemand {
        LearningTestsModule(requireNotNull(dependenciesProvider?.getDependencies()))
    }

    fun getApi(): LearningTestsApi = requireNotNull(component).api

    internal fun component(): LearningTestsComponent = requireNotNull(component)

    internal fun destroyComponent() {
        component = null
    }
}
