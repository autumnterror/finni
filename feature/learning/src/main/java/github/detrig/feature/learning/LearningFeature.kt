package github.detrig.feature.learning

import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.core.di.diDemand
import github.detrig.feature.learning.api.LearningApi
import github.detrig.feature.learning.di.LearningComponent
import github.detrig.feature.learning.di.LearningModule

object LearningFeature {
    var dependenciesProvider: ModuleDependenciesProvider<LearningDependencies>? = null

    private var component: LearningComponent? by diDemand {
        LearningModule(requireNotNull(dependenciesProvider?.getDependencies()))
    }

    fun getApi(): LearningApi = requireNotNull(component).api
}
