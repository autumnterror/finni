package github.detrig.feature.savings.domain

import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.economy.domain.FinancialOperation
import github.detrig.feature.economy.domain.FinancialOperationType
import github.detrig.feature.economy.domain.HistoryFilter
import github.detrig.feature.economy.domain.SavingsGoal
import github.detrig.feature.learning.api.LearningApi
import github.detrig.feature.learning.domain.RecordLearningResult
import github.detrig.feature.learning.domain.SavingsLearning
import github.detrig.feature.week.api.WeekApi
import kotlinx.coroutines.flow.first

/** Replays committed source facts after process death; Learning deduplicates by stable IDs. */
internal class SavingsLearningInteractor(
    private val economy: EconomyApi,
    private val week: WeekApi,
    private val learning: LearningApi,
) {
    suspend fun currentWeek(): Long {
        week.initialize()
        return week.observeState().first().weekNumber
    }

    suspend fun recordGoal(goal: SavingsGoal) {
        val period = goal.metadata.period() ?: 0L
        record(SavingsLearning.goalCreated(PROFILE_ID, goal.id, period))
    }

    suspend fun recordTransfer(operation: FinancialOperation) {
        if (operation.type != FinancialOperationType.TRANSFER_TO_SAVINGS) return
        val goalId = operation.context.reasonId ?: return
        val period = operation.context.metadata.period() ?: return
        record(SavingsLearning.contribution(PROFILE_ID, operation.id, goalId, operation.amountRub, period))
        val target = operation.context.metadata.field("target")?.toLongOrNull() ?: return
        if (operation.before.savingsRub < target && operation.after.savingsRub >= target) {
            val earlierReach = economy.getSavingsHistory(
                HistoryFilter(types = setOf(FinancialOperationType.TRANSFER_TO_SAVINGS)),
            ).any { previous ->
                previous.context.reasonId == goalId &&
                    (previous.timestampMillis < operation.timestampMillis ||
                        (previous.timestampMillis == operation.timestampMillis && previous.id < operation.id)) &&
                    previous.context.metadata.field("target")?.toLongOrNull()?.let { previousTarget ->
                        previous.before.savingsRub < previousTarget && previous.after.savingsRub >= previousTarget
                    } == true
            }
            if (!earlierReach) record(SavingsLearning.goalReached(PROFILE_ID, goalId, period))
        }
    }

    suspend fun reconcile() {
        economy.initialize()
        economy.getGoals().forEach { recordGoal(it) }
        economy.getSavingsHistory(HistoryFilter(types = setOf(FinancialOperationType.TRANSFER_TO_SAVINGS)))
            .sortedWith(compareBy(FinancialOperation::timestampMillis, FinancialOperation::id))
            .forEach { recordTransfer(it) }
    }

    private suspend fun record(action: github.detrig.feature.learning.domain.LearningAction) {
        when (val result = learning.record(action)) {
            is RecordLearningResult.Processed,
            is RecordLearningResult.AlreadyProcessed -> Unit
            is RecordLearningResult.OperationIdConflict -> error("Conflicting learning action ${result.actionId}")
            is RecordLearningResult.UnsupportedAction -> error("Unsupported learning action ${result.actionType.value}")
        }
    }

    private fun String?.period(): Long? = field("week")?.toLongOrNull()?.takeIf { it >= 0 }
    private fun String?.field(key: String): String? = this?.split(';')
        ?.firstOrNull { it.startsWith("$key=") }
        ?.substringAfter('=')

    private companion object { const val PROFILE_ID = "current" }
}
