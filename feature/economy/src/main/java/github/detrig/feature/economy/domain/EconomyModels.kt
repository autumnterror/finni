package github.detrig.feature.economy.domain

data class PeriodicIncome(
    val amountRub: Long,
    val periodMillis: Long,
    val nextAtMillis: Long,
)

data class EconomyState(
    val availableRub: Long,
    val savingsRub: Long,
    val debtRub: Long,
    val periodicIncome: PeriodicIncome,
) {
    val totalMoneyRub: Long get() = Math.addExact(availableRub, savingsRub)
    val netWorthRub: Long get() = Math.subtractExact(totalMoneyRub, debtRub)
    val hasActiveDebt: Boolean get() = debtRub > 0
}

data class FinancialSnapshot(
    val availableRub: Long,
    val savingsRub: Long,
    val debtRub: Long,
) {
    val totalMoneyRub: Long get() = Math.addExact(availableRub, savingsRub)
    val netWorthRub: Long get() = Math.subtractExact(totalMoneyRub, debtRub)
}

data class OperationContext(
    val reasonId: String? = null,
    val metadata: String? = null,
)

@JvmInline
value class FinancialOperationType(val code: String) {
    init { require(code.isNotBlank()) }

    companion object {
        val CREDIT = FinancialOperationType("credit")
        val DEBIT = FinancialOperationType("debit")
        val PERIODIC_INCOME = FinancialOperationType("periodic_income")
        val DEBT_CREATED = FinancialOperationType("debt_created")
        val DEBT_REPAYMENT = FinancialOperationType("debt_repayment")
        val DEBT_AUTO_REPAYMENT = FinancialOperationType("debt_auto_repayment")
        val TRANSFER_TO_SAVINGS = FinancialOperationType("transfer_to_savings")
        val TRANSFER_FROM_SAVINGS = FinancialOperationType("transfer_from_savings")
    }
}

data class FinancialOperation(
    val id: String,
    val type: FinancialOperationType,
    val amountRub: Long,
    val timestampMillis: Long,
    val availableDeltaRub: Long,
    val savingsDeltaRub: Long,
    val debtDeltaRub: Long,
    val before: FinancialSnapshot,
    val after: FinancialSnapshot,
    val context: OperationContext = OperationContext(),
)

enum class RejectionReason {
    INVALID_OPERATION_ID,
    INVALID_AMOUNT,
    OPERATION_ID_CONFLICT,
    INSUFFICIENT_AVAILABLE_FUNDS,
    INSUFFICIENT_SAVINGS,
    ACTIVE_DEBT_EXISTS,
    DEBT_LIMIT_EXCEEDED,
    NO_ACTIVE_DEBT,
}

sealed interface FinancialOperationResult {
    val state: EconomyState

    data class Applied(
        val operation: FinancialOperation,
        override val state: EconomyState,
    ) : FinancialOperationResult

    data class AlreadyApplied(
        val operation: FinancialOperation,
        override val state: EconomyState,
    ) : FinancialOperationResult

    data class Rejected(
        val reason: RejectionReason,
        override val state: EconomyState,
    ) : FinancialOperationResult
}

data class PeriodicIncomeResult(
    val processedCycles: Int,
    val grossIncomeRub: Long,
    val debtRepaidRub: Long,
    val receivedRub: Long,
    val operations: List<FinancialOperation>,
    val state: EconomyState,
)

data class SavingsGoal(
    val id: String,
    val title: String,
    val targetRub: Long,
    val metadata: String? = null,
    val isActive: Boolean = true,
)

data class SavingsGoalProgress(
    val goal: SavingsGoal,
    val savedRub: Long,
    val remainingRub: Long,
    val isReached: Boolean,
    val nextPeriodicIncome: PeriodicIncome,
)

data class HistoryFilter(
    val fromInclusiveMillis: Long? = null,
    val toExclusiveMillis: Long? = null,
    val types: Set<FinancialOperationType> = emptySet(),
)

data class FinancialChange(
    val availableDeltaRub: Long,
    val savingsDeltaRub: Long,
    val debtDeltaRub: Long,
) {
    val totalMoneyDeltaRub: Long get() = Math.addExact(availableDeltaRub, savingsDeltaRub)
    val netWorthDeltaRub: Long get() = Math.subtractExact(totalMoneyDeltaRub, debtDeltaRub)
}

data class FinancialSummary(
    val state: EconomyState,
    val operations: List<FinancialOperation>,
    val totalIncomeRub: Long,
    val totalExpensesRub: Long,
    val change: FinancialChange,
)
