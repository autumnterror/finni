package github.detrig.feature.savings.domain

import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.economy.domain.FinancialOperationResult
import github.detrig.feature.economy.domain.OperationContext
import github.detrig.feature.economy.domain.SavingsGoal
import github.detrig.feature.planning.api.PlanningApi
import github.detrig.feature.planning.domain.PlanActualOperation
import github.detrig.feature.savings.api.SavingsGoalDraft
import github.detrig.feature.week.api.WeekApi
import kotlinx.coroutines.flow.first

internal class CreateSavingsGoalInteractor(private val economy: EconomyApi) {
    suspend operator fun invoke(draft: SavingsGoalDraft): SavingsGoal = economy.saveGoal(
        SavingsGoal(draft.id, draft.title, draft.targetRub, draft.metadata, isActive = true),
    )
}

internal class TransferToSavingsInteractor(
    private val economy: EconomyApi,
    private val planning: PlanningApi,
    private val week: WeekApi,
) {
    suspend operator fun invoke(operationId: String, goalId: String, amountRub: Long): FinancialOperationResult {
        val result = economy.transferToSavings(
            operationId = operationId,
            amountRub = amountRub,
            context = OperationContext(reasonId = goalId, metadata = "source=savings-goal"),
        )
        if (result is FinancialOperationResult.Applied || result is FinancialOperationResult.AlreadyApplied) {
            week.initialize()
            val currentWeek = week.observeState().first()
            if (planning.getPlanProgress(currentWeek.weekNumber) != null) {
                planning.recordActual(
                    PlanActualOperation.SavingsContribution(
                        operationId = operationId,
                        weekNumber = currentWeek.weekNumber,
                        amountRub = amountRub,
                    ),
                )
            }
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
