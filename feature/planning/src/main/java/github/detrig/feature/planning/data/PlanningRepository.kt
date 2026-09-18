package github.detrig.feature.planning.data

import github.detrig.core.database.RoomTransactionRunner
import github.detrig.feature.planning.api.PlanningApi
import github.detrig.feature.planning.data.local.PlanActualOperationEntity
import github.detrig.feature.planning.data.local.PlanningDao
import github.detrig.feature.planning.data.local.WeeklyPlanEntity
import github.detrig.feature.planning.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal class PlanningRepository(
    private val dao: PlanningDao,
    private val transactionRunner: RoomTransactionRunner,
    private val config: PlanningConfig,
) : PlanningApi {
    override suspend fun getPlanProgress(weekNumber: Long): WeeklyPlanProgress? = transactionRunner.runInTransaction {
        dao.getPlan(weekNumber)?.toProgress(dao.getActualOperations(weekNumber))
    }

    override fun observePlanProgress(weekNumber: Long): Flow<WeeklyPlanProgress?> = combine(
        dao.observePlan(weekNumber),
        dao.observeActualOperations(weekNumber),
    ) { plan, actuals -> plan?.toProgress(actuals) }

    override suspend fun savePlan(
        weekNumber: Long,
        availableRub: Long,
        percentages: PlanPercentages,
    ): SavePlanResult = transactionRunner.runInTransaction {
        require(weekNumber >= 2) { "The first game week has no plan" }
        require(availableRub >= 0)
        dao.getPlan(weekNumber)?.let { return@runInTransaction SavePlanResult.AlreadySaved(it.toProgress(dao.getActualOperations(weekNumber))) }
        val entity = WeeklyPlanEntity(weekNumber, availableRub, percentages.mandatory, percentages.wants, percentages.savings)
        dao.insertPlan(entity)
        if (config.seedDemoProgress) seedDemoActuals(entity)
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

    private suspend fun seedDemoActuals(plan: WeeklyPlanEntity) {
        val progress = plan.toProgress(emptyList())
        val green = progress.category(PlanCategory.MANDATORY).plannedRub * 60 / 100
        val yellowBase = progress.category(PlanCategory.WANTS).plannedRub
        val redBase = progress.category(PlanCategory.SAVINGS).plannedRub
        val demoOperations = listOf(
            PlanActualOperationEntity("demo-plan:${plan.weekNumber}:mandatory", plan.weekNumber, PlanCategory.MANDATORY.code, green),
            PlanActualOperationEntity("demo-plan:${plan.weekNumber}:wants", plan.weekNumber, PlanCategory.WANTS.code, yellowBase + maxOf(1, yellowBase / 10)),
            PlanActualOperationEntity("demo-plan:${plan.weekNumber}:savings", plan.weekNumber, PlanCategory.SAVINGS.code, redBase + maxOf(2, redBase / 2)),
        )
        demoOperations.forEach { dao.insertActualOperation(it) }
    }

    private fun WeeklyPlanEntity.toProgress(actuals: List<PlanActualOperationEntity>): WeeklyPlanProgress {
        val plan = WeeklyPlan(weekNumber, availableRub, PlanPercentages(mandatoryPercent, wantsPercent, savingsPercent))
        val totals = actuals.groupBy { PlanCategory.entries.first { category -> category.code == it.categoryCode } }
            .mapValues { (_, operations) -> operations.sumOf { it.amountRub } }
        return PlanningCalculator.progress(plan, totals)
    }
}
