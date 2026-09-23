package github.detrig.feature.planning.data

import github.detrig.core.database.RoomTransactionRunner
import github.detrig.feature.planning.api.PlanningApi
import github.detrig.feature.planning.data.local.PlanActualOperationEntity
import github.detrig.feature.planning.data.local.PlanningDao
import github.detrig.feature.planning.data.local.WeeklyPlanEntity
import github.detrig.feature.planning.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

internal class PlanningRepository(
    private val dao: PlanningDao,
    private val transactionRunner: RoomTransactionRunner,
    private val config: PlanningConfig,
) : PlanningApi {
    override fun assessPlan(percentages: PlanPercentages): PlanAssessment =
        PlanningCalculator.assess(percentages, config)

    override suspend fun getPlanProgress(weekNumber: Long): WeeklyPlanProgress? = transactionRunner.runInTransaction {
        dao.deleteLegacyDemoActualOperations()
        dao.getPlan(weekNumber)?.toProgress(dao.getActualOperations(weekNumber))
    }

    override fun observePlanProgress(weekNumber: Long): Flow<WeeklyPlanProgress?> = flow {
        transactionRunner.runInTransaction { dao.deleteLegacyDemoActualOperations() }
        emitAll(combine(
            dao.observePlan(weekNumber),
            dao.observeActualOperations(weekNumber),
        ) { plan, actuals -> plan?.toProgress(actuals) })
    }

    override suspend fun savePlan(
        weekNumber: Long,
        availableRub: Long,
        percentages: PlanPercentages,
    ): SavePlanResult = transactionRunner.runInTransaction {
        require(weekNumber >= 1) { "The game week must be positive" }
        require(availableRub >= 0)
        dao.deleteLegacyDemoActualOperations()
        dao.getPlan(weekNumber)?.let { return@runInTransaction SavePlanResult.AlreadySaved(it.toProgress(dao.getActualOperations(weekNumber))) }
        val entity = WeeklyPlanEntity(weekNumber, availableRub, percentages.mandatory, percentages.wants, percentages.savings)
        dao.insertPlan(entity)
        SavePlanResult.Saved(entity.toProgress(dao.getActualOperations(weekNumber)))
    }

    override suspend fun recordActual(
        operationId: String,
        weekNumber: Long,
        category: PlanCategory,
        amountRub: Long,
    ): RecordActualResult = transactionRunner.runInTransaction {
        if (operationId.isBlank() || amountRub <= 0) throw IllegalArgumentException("Operation ID and amount must be positive")
        val incoming = PlanActualOperationEntity(operationId, weekNumber, category.code, amountRub)
        dao.getActualOperation(operationId)?.let { current ->
            return@runInTransaction if (current == incoming) RecordActualResult.AlreadyRecorded else RecordActualResult.OperationIdConflict
        }
        requireNotNull(dao.getPlan(weekNumber)) { "A plan must be saved before recording its result" }
        dao.insertActualOperation(incoming)
        RecordActualResult.Recorded
    }

    private fun WeeklyPlanEntity.toProgress(actuals: List<PlanActualOperationEntity>): WeeklyPlanProgress {
        val plan = WeeklyPlan(weekNumber, availableRub, PlanPercentages(mandatoryPercent, wantsPercent, savingsPercent))
        val totals = actuals.groupBy { PlanCategory.entries.first { category -> category.code == it.categoryCode } }
            .mapValues { (_, operations) -> operations.sumOf { it.amountRub } }
        return PlanningCalculator.progress(plan, totals)
    }
}
