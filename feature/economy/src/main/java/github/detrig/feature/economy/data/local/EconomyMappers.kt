package github.detrig.feature.economy.data.local

import github.detrig.feature.economy.domain.EconomyState
import github.detrig.feature.economy.domain.FinancialOperation
import github.detrig.feature.economy.domain.FinancialOperationType
import github.detrig.feature.economy.domain.FinancialSnapshot
import github.detrig.feature.economy.domain.OperationContext
import github.detrig.feature.economy.domain.PeriodicIncome
import github.detrig.feature.economy.domain.SavingsGoal
import github.detrig.feature.economy.domain.ParentHelpState

internal fun EconomyStateEntity.toDomain() = EconomyState(
    availableRub = availableRub,
    savingsRub = savingsRub,
    debtRub = debtRub,
    periodicIncome = PeriodicIncome(periodicAmountRub, periodicPeriodMillis, nextPeriodicAtMillis),
)

internal fun EconomyState.toEntity() = EconomyStateEntity(
    availableRub = availableRub,
    savingsRub = savingsRub,
    debtRub = debtRub,
    periodicAmountRub = periodicIncome.amountRub,
    periodicPeriodMillis = periodicIncome.periodMillis,
    nextPeriodicAtMillis = periodicIncome.nextAtMillis,
)

internal fun FinancialOperationEntity.toDomain() = FinancialOperation(
    id = id,
    type = FinancialOperationType(typeCode),
    amountRub = amountRub,
    timestampMillis = timestampMillis,
    availableDeltaRub = availableDeltaRub,
    savingsDeltaRub = savingsDeltaRub,
    debtDeltaRub = debtDeltaRub,
    before = FinancialSnapshot(beforeAvailableRub, beforeSavingsRub, beforeDebtRub),
    after = FinancialSnapshot(afterAvailableRub, afterSavingsRub, afterDebtRub),
    context = OperationContext(reasonId, metadata),
)

internal fun FinancialOperation.toEntity() = FinancialOperationEntity(
    id, type.code, amountRub, timestampMillis,
    availableDeltaRub, savingsDeltaRub, debtDeltaRub,
    before.availableRub, before.savingsRub, before.debtRub,
    after.availableRub, after.savingsRub, after.debtRub,
    context.reasonId, context.metadata,
)

internal fun SavingsGoalEntity.toDomain() = SavingsGoal(id, title, targetRub, metadata, isActive)
internal fun SavingsGoal.toEntity() = SavingsGoalEntity(id, title, targetRub, metadata, isActive)
internal fun ParentHelpStateEntity.toDomain() = ParentHelpState(
    offerId, receivedRub, totalRepaymentRub, remainingRub, paymentsRemaining,
)
internal fun ParentHelpState.toEntity() = ParentHelpStateEntity(
    offerId = offerId,
    receivedRub = receivedRub,
    totalRepaymentRub = totalRepaymentRub,
    remainingRub = remainingRub,
    paymentsRemaining = paymentsRemaining,
)
