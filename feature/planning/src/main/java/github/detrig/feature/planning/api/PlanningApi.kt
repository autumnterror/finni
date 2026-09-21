package github.detrig.feature.planning.api

import github.detrig.feature.planning.domain.PlanCategory
import github.detrig.feature.planning.domain.PlanAssessment
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

    /** Вызывается магазином, копилкой или событием после успешной финансовой операции. */
    suspend fun recordActual(
        operationId: String,
        weekNumber: Long,
        category: PlanCategory,
        amountRub: Long,
    ): RecordActualResult
}
