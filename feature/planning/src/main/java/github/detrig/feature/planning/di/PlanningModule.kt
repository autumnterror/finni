package github.detrig.feature.planning.di

import github.detrig.feature.planning.PlanningDependencies
import github.detrig.feature.planning.api.PlanningApi
import github.detrig.feature.planning.data.PlanningRepository

internal class PlanningModule(private val dependencies: PlanningDependencies) : PlanningComponent {
    override val api: PlanningApi by lazy {
        PlanningRepository(dependencies.planningDao(), dependencies.transactionRunner(), dependencies.config())
    }
}
