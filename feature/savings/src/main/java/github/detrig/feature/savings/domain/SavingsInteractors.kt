package github.detrig.feature.savings.domain

import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.economy.domain.FinancialOperationResult
import github.detrig.feature.economy.domain.OperationContext
import github.detrig.feature.economy.domain.SavingsGoal
import github.detrig.feature.planning.api.PlanningApi
import github.detrig.feature.planning.domain.PlanActualOperation
import github.detrig.feature.savings.api.SavingsGoalDraft
import github.detrig.feature.savings.api.SavingsGoalPurchaseResult
import github.detrig.feature.savings.api.SavingsGoalPurchaser
import github.detrig.core.database.RoomTransactionRunner

internal class CreateSavingsGoalInteractor(
    private val economy: EconomyApi,
    private val learning: SavingsLearningInteractor,
) {
    suspend operator fun invoke(draft: SavingsGoalDraft): SavingsGoal {
        economy.initialize()
        val existing = economy.getGoals().firstOrNull { it.id == draft.id }
        val metadata = existing?.metadata ?: listOfNotNull(
            draft.metadata,
            "week=${learning.currentWeek()}",
        ).joinToString(";")
        return economy.saveGoal(
            SavingsGoal(draft.id, draft.title, draft.targetRub, metadata, isActive = true),
        ).also { learning.recordGoal(it) }
    }
}

internal class TransferToSavingsInteractor(
    private val economy: EconomyApi,
    private val planning: PlanningApi,
    private val learning: SavingsLearningInteractor,
) {
    suspend operator fun invoke(operationId: String, goalId: String?, amountRub: Long): FinancialOperationResult {
        val currentWeek = learning.currentWeek()
        val target = goalId?.let { id -> economy.getGoals().firstOrNull { it.id == id }?.targetRub }
        val previous = economy.getSavingsHistory().firstOrNull { it.id == operationId }
        val reasonId = goalId ?: UNASSIGNED_SAVINGS_ID
        val originalContext = previous?.context?.takeIf { it.reasonId == reasonId }
        val operationWeek = originalContext?.metadata?.split(';')
            ?.firstOrNull { it.startsWith("week=") }
            ?.substringAfter('=')?.toLongOrNull() ?: currentWeek
        val result = economy.transferToSavings(
            operationId = operationId,
            amountRub = amountRub,
            context = originalContext ?: OperationContext(
                reasonId = reasonId,
                metadata = "source=${if (goalId == null) "savings" else "savings-goal"};week=$currentWeek;target=${target ?: 0}",
            ),
        )
        if (result is FinancialOperationResult.Applied || result is FinancialOperationResult.AlreadyApplied) {
            if (planning.getPlanProgress(operationWeek) != null) {
                planning.recordActual(
                    PlanActualOperation.SavingsContribution(
                        operationId = operationId,
                        weekNumber = operationWeek,
                        amountRub = amountRub,
                    ),
                )
            }
            if (goalId != null) {
                learning.recordTransfer(when (result) {
                    is FinancialOperationResult.Applied -> result.operation
                    is FinancialOperationResult.AlreadyApplied -> result.operation
                })
            }
        }
        return result
    }
}

internal class TransferFromSavingsInteractor(
    private val economy: EconomyApi,
    private val planning: PlanningApi,
    private val learning: SavingsLearningInteractor,
) {
    suspend operator fun invoke(operationId: String, goalId: String?, amountRub: Long): FinancialOperationResult {
        val previous = economy.getSavingsHistory().firstOrNull { it.id == operationId }
        val reasonId = goalId ?: UNASSIGNED_SAVINGS_ID
        val originalContext = previous?.context?.takeIf { it.reasonId == reasonId }
        val operationWeek = originalContext?.metadata?.weekNumber() ?: learning.currentWeek()
        val result = economy.transferFromSavings(
            operationId = operationId,
            amountRub = amountRub,
            context = originalContext ?: OperationContext(
                reasonId = reasonId,
                metadata = "source=${if (goalId == null) "savings" else "savings-goal"};week=$operationWeek",
            ),
        )
        if (result is FinancialOperationResult.Applied || result is FinancialOperationResult.AlreadyApplied) {
            if (planning.getPlanProgress(operationWeek) != null) {
                planning.recordActual(
                    PlanActualOperation.SavingsWithdrawal(
                        operationId = operationId,
                        weekNumber = operationWeek,
                        amountRub = amountRub,
                    ),
                )
            }
        }
        return result
    }

    private fun String?.weekNumber(): Long? = this?.split(';')
        ?.firstOrNull { it.startsWith("week=") }
        ?.substringAfter('=')?.toLongOrNull()
}

internal const val UNASSIGNED_SAVINGS_ID = "unassigned-savings"

internal class PurchaseSavingsGoalInteractor(
    private val economy: EconomyApi,
    private val purchaser: SavingsGoalPurchaser,
    private val transactionRunner: RoomTransactionRunner,
) {
    suspend operator fun invoke(goal: SavingsGoal): SavingsGoalPurchaseResult =
        transactionRunner.runInTransaction {
            val result = purchaser.purchase(goal)
            if (result == SavingsGoalPurchaseResult.Purchased ||
                result == SavingsGoalPurchaseResult.AlreadyPurchased
            ) {
                economy.deleteGoal(goal.id)
            }
            result
        }
}
