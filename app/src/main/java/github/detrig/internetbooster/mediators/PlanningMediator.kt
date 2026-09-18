package github.detrig.internetbooster.mediators

import github.detrig.core.Mediator
import github.detrig.core.database.RoomTransactionRunner
import github.detrig.core.di.ModuleDependenciesProvider
import github.detrig.feature.planning.PlanningDependencies
import github.detrig.feature.planning.PlanningFeature
import github.detrig.feature.planning.api.PlanningApi
import github.detrig.feature.planning.data.local.PlanningDao
import github.detrig.feature.planning.domain.PlanningConfig
import github.detrig.internetbooster.database.AppDatabaseModule

internal class PlanningMediator(
    private val databaseModule: AppDatabaseModule,
) : Mediator<PlanningApi> {
    fun init() {
        PlanningFeature.dependenciesProvider = ModuleDependenciesProvider {
            object : PlanningDependencies {
                override fun planningDao(): PlanningDao = databaseModule.planningDao
                override fun transactionRunner(): RoomTransactionRunner = databaseModule.transactionRunner
                override fun config(): PlanningConfig = PlanningConfig()
            }
        }
    }

    override fun getApi(): PlanningApi = PlanningFeature.getApi()
}
