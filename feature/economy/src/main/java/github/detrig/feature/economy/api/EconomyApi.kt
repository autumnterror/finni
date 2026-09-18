package github.detrig.feature.economy.api

import github.detrig.feature.economy.domain.EconomyState
import github.detrig.feature.economy.domain.FinancialOperation
import github.detrig.feature.economy.domain.FinancialOperationResult
import github.detrig.feature.economy.domain.FinancialSummary
import github.detrig.feature.economy.domain.HistoryFilter
import github.detrig.feature.economy.domain.OperationContext
import github.detrig.feature.economy.domain.PeriodicIncome
import github.detrig.feature.economy.domain.PeriodicIncomeResult
import github.detrig.feature.economy.domain.SavingsGoal
import github.detrig.feature.economy.domain.SavingsGoalProgress
import github.detrig.feature.economy.domain.WeeklyAllowanceResult
import github.detrig.feature.economy.domain.ParentHelpOffer
import github.detrig.feature.economy.domain.ParentHelpState
import github.detrig.feature.economy.domain.ParentHelpRequestResult
import kotlinx.coroutines.flow.Flow

interface EconomyApi {
    suspend fun initialize(): EconomyState
    suspend fun getState(): EconomyState
    fun observeState(): Flow<EconomyState>
    suspend fun canDebit(amountRub: Long): Boolean
    suspend fun credit(operationId: String, amountRub: Long, context: OperationContext = OperationContext()): FinancialOperationResult
    suspend fun debit(operationId: String, amountRub: Long, context: OperationContext = OperationContext()): FinancialOperationResult
    suspend fun createDebt(operationId: String, amountRub: Long, context: OperationContext = OperationContext()): FinancialOperationResult
    suspend fun repayDebt(operationId: String, amountRub: Long, context: OperationContext = OperationContext()): FinancialOperationResult
    suspend fun transferToSavings(operationId: String, amountRub: Long, context: OperationContext = OperationContext()): FinancialOperationResult
    suspend fun transferFromSavings(operationId: String, amountRub: Long, context: OperationContext = OperationContext()): FinancialOperationResult
    suspend fun configurePeriodicIncome(periodicIncome: PeriodicIncome): EconomyState
    suspend fun isPeriodicIncomeDue(atMillis: Long): Boolean
    suspend fun processPeriodicIncome(atMillis: Long): PeriodicIncomeResult
    suspend fun grantWeeklyAllowance(weekNumber: Long): WeeklyAllowanceResult
    fun parentHelpOffers(): List<ParentHelpOffer> = emptyList()
    suspend fun getParentHelp(): ParentHelpState? = null
    suspend fun requestParentHelp(operationId: String, offerId: String): ParentHelpRequestResult =
        error("Parent help is unavailable")
    suspend fun getHistory(filter: HistoryFilter = HistoryFilter()): List<FinancialOperation>
    suspend fun getIncomeHistory(filter: HistoryFilter = HistoryFilter()): List<FinancialOperation>
    suspend fun getExpenseHistory(filter: HistoryFilter = HistoryFilter()): List<FinancialOperation>
    suspend fun getDebtHistory(filter: HistoryFilter = HistoryFilter()): List<FinancialOperation>
    suspend fun getSavingsHistory(filter: HistoryFilter = HistoryFilter()): List<FinancialOperation>
    suspend fun getSummary(filter: HistoryFilter = HistoryFilter()): FinancialSummary
    suspend fun saveGoal(goal: SavingsGoal): SavingsGoal
    suspend fun deleteGoal(id: String): Boolean
    suspend fun getGoals(): List<SavingsGoal>
    suspend fun getActiveGoal(): SavingsGoal?
    suspend fun getGoalProgress(id: String): SavingsGoalProgress?
}
