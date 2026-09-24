package github.detrig.feature.savings.domain

import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.economy.domain.FinancialOperationResult
import github.detrig.feature.economy.domain.OperationContext
import github.detrig.feature.economy.domain.SavingsGoal
import github.detrig.feature.planning.api.PlanningApi
import github.detrig.feature.planning.domain.PlanActualOperation
import github.detrig.feature.savings.api.SavingsGoalDraft

internal class CreateSavingsGoalInteractor(
    private val economy: EconomyApi,
    private val learning: SavingsLearningInteractor,
) {
    suspend operator fun invoke(draft: SavingsGoalDraft): SavingsGoal {
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
    suspend operator fun invoke(operationId: String, goalId: String, amountRub: Long): FinancialOperationResult {
        val currentWeek = learning.currentWeek()
        val target = economy.getGoals().firstOrNull { it.id == goalId }?.targetRub
        val previous = economy.getSavingsHistory().firstOrNull { it.id == operationId }
        val originalContext = previous?.context?.takeIf { it.reasonId == goalId }
        val operationWeek = originalContext?.metadata?.split(';')
            ?.firstOrNull { it.startsWith("week=") }
            ?.substringAfter('=')?.toLongOrNull() ?: currentWeek
        val result = economy.transferToSavings(
            operationId = operationId,
            amountRub = amountRub,
            context = originalContext ?: OperationContext(
                reasonId = goalId,
                metadata = "source=savings-goal;week=$currentWeek;target=${target ?: 0}",
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
            learning.recordTransfer(when (result) {
                is FinancialOperationResult.Applied -> result.operation
                is FinancialOperationResult.AlreadyApplied -> result.operation
            })
        }
        return result
    }
}

internal class TransferFromSavingsInteractor(private val economy: EconomyApi) {
    suspend operator fun invoke(operationId: String, goalId: String, amountRub: Long): FinancialOperationResult =
        economy.transferFromSavings(
            operationId = operationId,
            amountRub = amountRub,
            context = OperationContext(reasonId = goalId, metadata = "source=savings-goal"),
        )
}
