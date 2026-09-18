package github.detrig.feature.planning

import github.detrig.core.database.RoomTransactionRunner
import github.detrig.feature.planning.data.local.PlanningDao
import github.detrig.feature.planning.domain.PlanningConfig

interface PlanningDependencies {
    fun planningDao(): PlanningDao
    fun transactionRunner(): RoomTransactionRunner
    fun config(): PlanningConfig
}
