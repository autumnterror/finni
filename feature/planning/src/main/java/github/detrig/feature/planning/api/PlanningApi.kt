package github.detrig.feature.planning.api

import github.detrig.feature.planning.domain.PlanAssessment
import github.detrig.feature.planning.domain.PlanActualOperation
import github.detrig.feature.planning.domain.PlanPercentages
import github.detrig.feature.planning.domain.RecordActualResult
import github.detrig.feature.planning.domain.SavePlanResult
import github.detrig.feature.planning.domain.WeeklyPlanProgress
import kotlinx.coroutines.flow.Flow

interface PlanningApi {
    fun assessPlan(percentages: PlanPercentages): PlanAssessment
    suspend fun getPlanProgress(weekNumber: Long): WeeklyPlanProgress?
    fun observePlanProgress(weekNumber: Long): Flow<WeeklyPlanProgress?>
    suspend fun savePlan(weekNumber: Long, availableRub: Long, percentages: PlanPercentages): SavePlanResult

    /** Вызывается источником после успешной финансовой операции. */
    suspend fun recordActual(operation: PlanActualOperation): RecordActualResult
}
