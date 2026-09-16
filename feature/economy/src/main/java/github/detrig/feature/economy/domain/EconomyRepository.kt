package github.detrig.feature.economy.domain

import kotlinx.coroutines.flow.Flow

internal interface EconomyRepository {
    suspend fun initialize(): EconomyState
    suspend fun state(): EconomyState
    fun observeState(): Flow<EconomyState>
    suspend fun canDebit(amountRub: Long): Boolean
    suspend fun credit(id: String, amountRub: Long, context: OperationContext): FinancialOperationResult
    suspend fun debit(id: String, amountRub: Long, context: OperationContext): FinancialOperationResult
    suspend fun createDebt(id: String, amountRub: Long, context: OperationContext): FinancialOperationResult
    suspend fun repayDebt(id: String, amountRub: Long, context: OperationContext): FinancialOperationResult
    suspend fun transferToSavings(id: String, amountRub: Long, context: OperationContext): FinancialOperationResult
    suspend fun transferFromSavings(id: String, amountRub: Long, context: OperationContext): FinancialOperationResult
    suspend fun configurePeriodicIncome(periodicIncome: PeriodicIncome): EconomyState
    suspend fun isPeriodicIncomeDue(atMillis: Long): Boolean
    suspend fun processPeriodicIncome(atMillis: Long): PeriodicIncomeResult
    suspend fun history(filter: HistoryFilter): List<FinancialOperation>
    suspend fun summary(filter: HistoryFilter): FinancialSummary
    suspend fun upsertGoal(goal: SavingsGoal): SavingsGoal
    suspend fun deleteGoal(id: String): Boolean
    suspend fun goals(): List<SavingsGoal>
    suspend fun activeGoal(): SavingsGoal?
    suspend fun goalProgress(id: String): SavingsGoalProgress?
}
