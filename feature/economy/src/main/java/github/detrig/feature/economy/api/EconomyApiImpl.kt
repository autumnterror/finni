package github.detrig.feature.economy.api

import github.detrig.feature.economy.domain.EconomyRepository
import github.detrig.feature.economy.domain.HistoryFilter
import github.detrig.feature.economy.domain.OperationContext
import github.detrig.feature.economy.domain.PeriodicIncome
import github.detrig.feature.economy.domain.SavingsGoal
import github.detrig.feature.economy.domain.FinancialOperationType

internal class EconomyApiImpl(private val repository: EconomyRepository) : EconomyApi {
    override suspend fun initialize() = repository.initialize()
    override suspend fun getState() = repository.state()
    override fun observeState() = repository.observeState()
    override suspend fun canDebit(amountRub: Long) = repository.canDebit(amountRub)
    override suspend fun provideZeroBalanceHelp() = repository.provideZeroBalanceHelp()
    override suspend fun credit(operationId: String, amountRub: Long, context: OperationContext) = repository.credit(operationId, amountRub, context)
    override suspend fun debit(operationId: String, amountRub: Long, context: OperationContext) = repository.debit(operationId, amountRub, context)
    override suspend fun createDebt(operationId: String, amountRub: Long, context: OperationContext) = repository.createDebt(operationId, amountRub, context)
    override suspend fun repayDebt(operationId: String, amountRub: Long, context: OperationContext) = repository.repayDebt(operationId, amountRub, context)
    override suspend fun transferToSavings(operationId: String, amountRub: Long, context: OperationContext) = repository.transferToSavings(operationId, amountRub, context)
    override suspend fun transferFromSavings(operationId: String, amountRub: Long, context: OperationContext) = repository.transferFromSavings(operationId, amountRub, context)
    override suspend fun configurePeriodicIncome(periodicIncome: PeriodicIncome) = repository.configurePeriodicIncome(periodicIncome)
    override suspend fun isPeriodicIncomeDue(atMillis: Long) = repository.isPeriodicIncomeDue(atMillis)
    override suspend fun processPeriodicIncome(atMillis: Long) = repository.processPeriodicIncome(atMillis)
    override suspend fun grantWeeklyAllowance(weekNumber: Long) = repository.grantWeeklyAllowance(weekNumber)
    override fun parentHelpOffers() = repository.parentHelpOffers()
    override suspend fun getParentHelp() = repository.parentHelp()
    override suspend fun requestParentHelp(operationId: String, offerId: String) =
        repository.requestParentHelp(operationId, offerId)
    override suspend fun getHistory(filter: HistoryFilter) = repository.history(filter)
    override suspend fun getIncomeHistory(filter: HistoryFilter) = repository.history(filter.withTypes(setOf(
        FinancialOperationType.CREDIT, FinancialOperationType.PERIODIC_INCOME, FinancialOperationType.WEEKLY_ALLOWANCE,
        FinancialOperationType.ZERO_BALANCE_HELP,
    )))
    override suspend fun getExpenseHistory(filter: HistoryFilter) = repository.history(filter.withTypes(setOf(FinancialOperationType.DEBIT)))
    override suspend fun getDebtHistory(filter: HistoryFilter) = repository.history(filter.withTypes(setOf(
        FinancialOperationType.DEBT_CREATED, FinancialOperationType.DEBT_REPAYMENT, FinancialOperationType.DEBT_AUTO_REPAYMENT,
    )))
    override suspend fun getSavingsHistory(filter: HistoryFilter) = repository.history(filter.withTypes(setOf(
        FinancialOperationType.TRANSFER_TO_SAVINGS, FinancialOperationType.TRANSFER_FROM_SAVINGS,
    )))
    override suspend fun getSummary(filter: HistoryFilter) = repository.summary(filter)
    override suspend fun saveGoal(goal: SavingsGoal) = repository.upsertGoal(goal)
    override suspend fun deleteGoal(id: String) = repository.deleteGoal(id)
    override suspend fun getGoals() = repository.goals()
    override suspend fun getActiveGoal() = repository.activeGoal()
    override suspend fun getGoalProgress(id: String) = repository.goalProgress(id)

    private fun HistoryFilter.withTypes(category: Set<FinancialOperationType>): HistoryFilter {
        return copy(types = category)
    }
}
