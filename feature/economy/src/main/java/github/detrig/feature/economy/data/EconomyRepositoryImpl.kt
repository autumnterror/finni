package github.detrig.feature.economy.data

import github.detrig.core.database.RoomTransactionRunner
import github.detrig.feature.economy.data.local.EconomyDao
import github.detrig.feature.economy.data.local.toDomain
import github.detrig.feature.economy.data.local.toEntity
import github.detrig.feature.economy.domain.EconomyConfig
import github.detrig.feature.economy.domain.EconomyRepository
import github.detrig.feature.economy.domain.EconomyState
import github.detrig.feature.economy.domain.FinancialChange
import github.detrig.feature.economy.domain.FinancialOperation
import github.detrig.feature.economy.domain.FinancialOperationResult
import github.detrig.feature.economy.domain.FinancialOperationType
import github.detrig.feature.economy.domain.FinancialSnapshot
import github.detrig.feature.economy.domain.FinancialSummary
import github.detrig.feature.economy.domain.HistoryFilter
import github.detrig.feature.economy.domain.OperationContext
import github.detrig.feature.economy.domain.PeriodicIncome
import github.detrig.feature.economy.domain.PeriodicIncomeResult
import github.detrig.feature.economy.domain.RejectionReason
import github.detrig.feature.economy.domain.SavingsGoal
import github.detrig.feature.economy.domain.SavingsGoalProgress
import github.detrig.feature.economy.domain.WeeklyAllowanceResult
import github.detrig.feature.economy.domain.ZeroBalanceHelpResult
import github.detrig.feature.economy.domain.ParentHelpOffer
import github.detrig.feature.economy.domain.ParentHelpRequestResult
import github.detrig.feature.economy.domain.ParentHelpState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class EconomyRepositoryImpl(
    private val dao: EconomyDao,
    private val transactionRunner: RoomTransactionRunner,
    private val config: EconomyConfig,
    private val currentTimeMillis: () -> Long,
) : EconomyRepository {

    private val mutex = Mutex()

    override suspend fun initialize(): EconomyState = atomic { ensureState() }

    override suspend fun state(): EconomyState = atomic { ensureState() }

    override fun observeState(): Flow<EconomyState> = dao.observeState().filterNotNull().map { it.toDomain() }

    override suspend fun canDebit(amountRub: Long): Boolean = amountRub > 0 && state().availableRub >= amountRub

    override suspend fun provideZeroBalanceHelp(
        minimumRequiredBalanceRub: Long,
    ): ZeroBalanceHelpResult = atomic {
        require(minimumRequiredBalanceRub > 0)
        val state = ensureState()
        if (state.availableRub >= minimumRequiredBalanceRub) {
            return@atomic ZeroBalanceHelpResult.NotNeeded(state)
        }

        val number = Math.incrementExact(
            dao.getOperations().count { it.typeCode == FinancialOperationType.ZERO_BALANCE_HELP.code },
        )
        val updated = state.copy(availableRub = Math.addExact(state.availableRub, config.zeroBalanceHelpRub))
        validate(updated)
        val operation = operation(
            id = "zero-balance-help:$number",
            amountRub = config.zeroBalanceHelpRub,
            type = FinancialOperationType.ZERO_BALANCE_HELP,
            context = OperationContext(
                reasonId = "cannot-afford-cheapest-product",
                metadata = "source=parents;minimumRequiredBalanceRub=$minimumRequiredBalanceRub",
            ),
            before = state,
            after = updated,
            timestamp = currentTimeMillis(),
        )
        dao.updateState(updated.toEntity())
        dao.insertOperation(operation.toEntity())
        ZeroBalanceHelpResult.Granted(config.zeroBalanceHelpRub, updated)
    }

    override suspend fun credit(id: String, amountRub: Long, context: OperationContext) = mutate(
        id, amountRub, FinancialOperationType.CREDIT, context,
        reject = { null },
        transform = { it.copy(availableRub = Math.addExact(it.availableRub, amountRub)) },
    )

    override suspend fun debit(id: String, amountRub: Long, context: OperationContext) = mutate(
        id, amountRub, FinancialOperationType.DEBIT, context,
        reject = { if (it.availableRub < amountRub) RejectionReason.INSUFFICIENT_AVAILABLE_FUNDS else null },
        transform = { it.copy(availableRub = it.availableRub - amountRub) },
    )

    override suspend fun createDebt(id: String, amountRub: Long, context: OperationContext) = mutate(
        id, amountRub, FinancialOperationType.DEBT_CREATED, context,
        reject = {
            when {
                it.hasActiveDebt -> RejectionReason.ACTIVE_DEBT_EXISTS
                amountRub > config.maximumDebtRub -> RejectionReason.DEBT_LIMIT_EXCEEDED
                else -> null
            }
        },
        transform = {
            it.copy(
                availableRub = Math.addExact(it.availableRub, amountRub),
                debtRub = amountRub,
            )
        },
    )

    override suspend fun repayDebt(id: String, amountRub: Long, context: OperationContext) = atomic {
        val state = ensureState()
        replayOrReject(id, amountRub, FinancialOperationType.DEBT_REPAYMENT, context, state)?.let { return@atomic it }
        val reason = when {
            dao.getParentHelp() != null -> RejectionReason.SCHEDULED_REPAYMENT_ONLY
            state.debtRub == 0L -> RejectionReason.NO_ACTIVE_DEBT
            amountRub > state.debtRub -> RejectionReason.AMOUNT_EXCEEDS_DEBT
            state.availableRub < amountRub -> RejectionReason.INSUFFICIENT_AVAILABLE_FUNDS
            else -> null
        }
        if (reason != null) return@atomic FinancialOperationResult.Rejected(reason, state)
        persistOperation(
            id, amountRub, FinancialOperationType.DEBT_REPAYMENT, context, state,
            state.copy(availableRub = state.availableRub - amountRub, debtRub = state.debtRub - amountRub),
        )
    }

    override suspend fun transferToSavings(id: String, amountRub: Long, context: OperationContext) = mutate(
        id, amountRub, FinancialOperationType.TRANSFER_TO_SAVINGS, context,
        reject = { if (it.availableRub < amountRub) RejectionReason.INSUFFICIENT_AVAILABLE_FUNDS else null },
        transform = {
            it.copy(
                availableRub = it.availableRub - amountRub,
                savingsRub = Math.addExact(it.savingsRub, amountRub),
            )
        },
    )

    override suspend fun transferFromSavings(id: String, amountRub: Long, context: OperationContext) = mutate(
        id, amountRub, FinancialOperationType.TRANSFER_FROM_SAVINGS, context,
        reject = { if (it.savingsRub < amountRub) RejectionReason.INSUFFICIENT_SAVINGS else null },
        transform = {
            it.copy(
                availableRub = Math.addExact(it.availableRub, amountRub),
                savingsRub = it.savingsRub - amountRub,
            )
        },
    )

    override suspend fun configurePeriodicIncome(periodicIncome: PeriodicIncome): EconomyState {
        require(periodicIncome.amountRub > 0)
        require(periodicIncome.periodMillis > 0)
        return atomic {
            val updated = ensureState().copy(periodicIncome = periodicIncome)
            dao.updateState(updated.toEntity())
            updated
        }
    }

    override suspend fun isPeriodicIncomeDue(atMillis: Long): Boolean = state().periodicIncome.nextAtMillis <= atMillis

    override suspend fun processPeriodicIncome(atMillis: Long): PeriodicIncomeResult = atomic {
        var state = ensureState()
        val hasScheduledParentHelp = dao.getParentHelp() != null
        val operations = mutableListOf<FinancialOperation>()
        var cycles = 0
        var gross = 0L
        var repaid = 0L
        while (state.periodicIncome.nextAtMillis <= atMillis) {
            val scheduledAt = state.periodicIncome.nextAtMillis
            val amount = state.periodicIncome.amountRub
            val incomeId = "periodic:$scheduledAt:income"
            val beforeIncome = state
            state = state.copy(availableRub = Math.addExact(state.availableRub, amount))
            val income = operation(
                incomeId, amount, FinancialOperationType.PERIODIC_INCOME,
                OperationContext(reasonId = "periodic:$scheduledAt"), beforeIncome, state, scheduledAt,
            )
            dao.insertOperation(income.toEntity())
            operations += income
            gross = Math.addExact(gross, amount)

            val payment = if (hasScheduledParentHelp) 0 else minOf(state.debtRub, amount)
            if (payment > 0) {
                val beforePayment = state
                state = state.copy(availableRub = state.availableRub - payment, debtRub = state.debtRub - payment)
                val debtOperation = operation(
                    "periodic:$scheduledAt:debt", payment, FinancialOperationType.DEBT_AUTO_REPAYMENT,
                    OperationContext(reasonId = incomeId), beforePayment, state, scheduledAt,
                )
                dao.insertOperation(debtOperation.toEntity())
                operations += debtOperation
                repaid = Math.addExact(repaid, payment)
            }
            state = state.copy(
                periodicIncome = state.periodicIncome.copy(
                    nextAtMillis = Math.addExact(scheduledAt, state.periodicIncome.periodMillis),
                ),
            )
            cycles = Math.incrementExact(cycles)
        }
        if (cycles > 0) dao.updateState(state.toEntity())
        PeriodicIncomeResult(cycles, gross, repaid, gross - repaid, operations, state)
    }

    override suspend fun grantWeeklyAllowance(weekNumber: Long): WeeklyAllowanceResult = atomic {
        require(weekNumber >= 2) { "The first week uses the opening balance" }
        var state = ensureState()
        val incomeId = "week:$weekNumber:allowance"
        val parentHelpDebtId = "week:$weekNumber:parent-help-repayment"
        val legacyDebtId = "week:$weekNumber:debt-repayment"
        dao.getOperation(incomeId)?.toDomain()?.let { existing ->
            check(existing.type == FinancialOperationType.WEEKLY_ALLOWANCE)
            val parentHelpRepayment = dao.getOperation(parentHelpDebtId)?.amountRub ?: 0
            val legacyRepayment = dao.getOperation(legacyDebtId)?.amountRub ?: 0
            return@atomic WeeklyAllowanceResult(
                weekNumber, existing.amountRub, parentHelpRepayment + legacyRepayment,
                state, alreadyApplied = true,
                parentHelpRepaidRub = parentHelpRepayment,
            )
        }
        val amount = config.weeklyAllowanceRub
        val beforeIncome = state
        state = state.copy(availableRub = Math.addExact(state.availableRub, amount))
        dao.insertOperation(operation(
            incomeId, amount, FinancialOperationType.WEEKLY_ALLOWANCE,
            OperationContext(reasonId = "week:$weekNumber"), beforeIncome, state, currentTimeMillis(),
        ).toEntity())
        val parentHelp = dao.getParentHelp()?.toDomain()
        val debtId = if (parentHelp != null) parentHelpDebtId else legacyDebtId
        val repayment = when {
            parentHelp != null -> minOf(parentHelp.nextPaymentRub, state.debtRub, amount)
            else -> minOf(state.debtRub, amount)
        }
        if (repayment > 0) {
            val beforeRepayment = state
            state = state.copy(
                availableRub = state.availableRub - repayment,
                debtRub = state.debtRub - repayment,
            )
            dao.insertOperation(operation(
                debtId, repayment, FinancialOperationType.DEBT_AUTO_REPAYMENT,
                OperationContext(reasonId = incomeId), beforeRepayment, state, currentTimeMillis(),
            ).toEntity())
            if (parentHelp != null) {
                val remaining = parentHelp.remainingRub - repayment
                val paymentsRemaining = parentHelp.paymentsRemaining - 1
                if (remaining == 0L) {
                    dao.clearParentHelp()
                } else {
                    check(paymentsRemaining > 0)
                    dao.upsertParentHelp(parentHelp.copy(
                        remainingRub = remaining,
                        paymentsRemaining = paymentsRemaining,
                    ).toEntity())
                }
            }
        }
        validate(state)
        dao.updateState(state.toEntity())
        WeeklyAllowanceResult(
            weekNumber, amount, repayment, state, alreadyApplied = false,
            parentHelpRepaidRub = if (parentHelp != null) repayment else 0,
        )
    }

    override fun parentHelpOffers(): List<ParentHelpOffer> = config.parentHelpOffers

    override suspend fun parentHelp(): ParentHelpState? = atomic { dao.getParentHelp()?.toDomain() }

    override suspend fun requestParentHelp(
        operationId: String,
        offerId: String,
    ): ParentHelpRequestResult = atomic {
        val state = ensureState()
        dao.getParentHelp()?.toDomain()?.let { return@atomic ParentHelpRequestResult.AlreadyActive(it) }
        val offer = config.parentHelpOffers.firstOrNull { it.id == offerId }
            ?: return@atomic ParentHelpRequestResult.Rejected(RejectionReason.INVALID_AMOUNT, state)
        if (state.hasActiveDebt) return@atomic ParentHelpRequestResult.Rejected(RejectionReason.ACTIVE_DEBT_EXISTS, state)
        if (operationId.isBlank()) return@atomic ParentHelpRequestResult.Rejected(RejectionReason.INVALID_OPERATION_ID, state)
        dao.getOperation(operationId)?.let {
            return@atomic ParentHelpRequestResult.Rejected(RejectionReason.OPERATION_ID_CONFLICT, state)
        }
        val updated = state.copy(
            availableRub = Math.addExact(state.availableRub, offer.receivedRub),
            debtRub = Math.addExact(state.debtRub, offer.totalRepaymentRub),
        )
        validate(updated)
        val operation = operation(
            operationId, offer.receivedRub, FinancialOperationType.DEBT_CREATED,
            OperationContext(reasonId = offer.id, metadata = "source=parent-help;totalRepaymentRub=${offer.totalRepaymentRub}"),
            state, updated, currentTimeMillis(),
        )
        dao.updateState(updated.toEntity())
        dao.insertOperation(operation.toEntity())
        val help = ParentHelpState(
            offerId = offer.id,
            receivedRub = offer.receivedRub,
            totalRepaymentRub = offer.totalRepaymentRub,
            remainingRub = offer.totalRepaymentRub,
            paymentsRemaining = offer.repaymentWeeks,
        )
        dao.upsertParentHelp(help.toEntity())
        ParentHelpRequestResult.Accepted(help, updated)
    }

    override suspend fun history(filter: HistoryFilter): List<FinancialOperation> = atomic {
        require(filter.fromInclusiveMillis == null || filter.toExclusiveMillis == null || filter.fromInclusiveMillis <= filter.toExclusiveMillis)
        dao.getOperations().asSequence().map { it.toDomain() }.filter { operation ->
            (filter.fromInclusiveMillis == null || operation.timestampMillis >= filter.fromInclusiveMillis) &&
                (filter.toExclusiveMillis == null || operation.timestampMillis < filter.toExclusiveMillis) &&
                (filter.types.isEmpty() || operation.type in filter.types)
        }.toList()
    }

    override suspend fun summary(filter: HistoryFilter): FinancialSummary = atomic {
        require(filter.fromInclusiveMillis == null || filter.toExclusiveMillis == null || filter.fromInclusiveMillis <= filter.toExclusiveMillis)
        val currentState = ensureState()
        val operations = dao.getOperations().asSequence().map { it.toDomain() }.filter { operation ->
            (filter.fromInclusiveMillis == null || operation.timestampMillis >= filter.fromInclusiveMillis) &&
                (filter.toExclusiveMillis == null || operation.timestampMillis < filter.toExclusiveMillis) &&
                (filter.types.isEmpty() || operation.type in filter.types)
        }.toList()
        val incomeTypes = setOf(FinancialOperationType.CREDIT, FinancialOperationType.PERIODIC_INCOME,
            FinancialOperationType.WEEKLY_ALLOWANCE, FinancialOperationType.ZERO_BALANCE_HELP)
        val income = operations.filter { it.type in incomeTypes }.sumExact { it.amountRub }
        val expenses = operations.filter { it.type == FinancialOperationType.DEBIT }.sumExact { it.amountRub }
        FinancialSummary(
            state = currentState,
            operations = operations,
            totalIncomeRub = income,
            totalExpensesRub = expenses,
            change = FinancialChange(
                operations.sumExact { it.availableDeltaRub },
                operations.sumExact { it.savingsDeltaRub },
                operations.sumExact { it.debtDeltaRub },
            ),
        )
    }

    override suspend fun upsertGoal(goal: SavingsGoal): SavingsGoal {
        require(goal.id.isNotBlank())
        require(goal.title.isNotBlank())
        require(goal.targetRub > 0)
        return atomic {
            if (goal.isActive) dao.deactivateOtherGoals(goal.id)
            dao.upsertGoal(goal.toEntity())
            goal
        }
    }

    override suspend fun deleteGoal(id: String): Boolean = atomic { id.isNotBlank() && dao.deleteGoal(id) > 0 }
    override suspend fun goals(): List<SavingsGoal> = atomic { dao.getGoals().map { it.toDomain() } }
    override suspend fun activeGoal(): SavingsGoal? = goals().firstOrNull { it.isActive }

    override suspend fun goalProgress(id: String): SavingsGoalProgress? = atomic {
        val goal = dao.getGoal(id)?.toDomain() ?: return@atomic null
        val state = ensureState()
        SavingsGoalProgress(
            goal = goal,
            savedRub = state.savingsRub,
            remainingRub = maxOf(0, goal.targetRub - state.savingsRub),
            isReached = state.savingsRub >= goal.targetRub,
            nextPeriodicIncome = state.periodicIncome,
        )
    }

    private suspend fun mutate(
        id: String,
        amountRub: Long,
        type: FinancialOperationType,
        context: OperationContext,
        reject: (EconomyState) -> RejectionReason?,
        transform: (EconomyState) -> EconomyState,
    ): FinancialOperationResult = atomic {
        val state = ensureState()
        replayOrReject(id, amountRub, type, context, state)?.let { return@atomic it }
        reject(state)?.let { return@atomic FinancialOperationResult.Rejected(it, state) }
        persistOperation(id, amountRub, type, context, state, transform(state))
    }

    private suspend fun replayOrReject(
        id: String,
        amountRub: Long,
        type: FinancialOperationType,
        context: OperationContext,
        state: EconomyState,
    ): FinancialOperationResult? {
        if (id.isBlank()) return FinancialOperationResult.Rejected(RejectionReason.INVALID_OPERATION_ID, state)
        dao.getOperation(id)?.toDomain()?.let { existing ->
            return if (existing.amountRub == amountRub && existing.type == type && existing.context == context) {
                FinancialOperationResult.AlreadyApplied(existing, state)
            } else {
                FinancialOperationResult.Rejected(RejectionReason.OPERATION_ID_CONFLICT, state)
            }
        }
        if (amountRub <= 0) return FinancialOperationResult.Rejected(RejectionReason.INVALID_AMOUNT, state)
        return null
    }

    private suspend fun persistOperation(
        id: String,
        amountRub: Long,
        type: FinancialOperationType,
        context: OperationContext,
        before: EconomyState,
        after: EconomyState,
    ): FinancialOperationResult.Applied {
        validate(after)
        val operation = operation(id, amountRub, type, context, before, after, currentTimeMillis())
        dao.updateState(after.toEntity())
        dao.insertOperation(operation.toEntity())
        return FinancialOperationResult.Applied(operation, after)
    }

    private fun operation(
        id: String,
        amountRub: Long,
        type: FinancialOperationType,
        context: OperationContext,
        before: EconomyState,
        after: EconomyState,
        timestamp: Long,
    ) = FinancialOperation(
        id, type, amountRub, timestamp,
        after.availableRub - before.availableRub,
        after.savingsRub - before.savingsRub,
        after.debtRub - before.debtRub,
        before.snapshot(), after.snapshot(), context,
    )

    private suspend fun ensureState(): EconomyState {
        dao.getState()?.let { return it.toDomain() }
        val now = currentTimeMillis()
        val initial = config.initialState(now)
        val openingAmount = Math.addExact(initial.availableRub, initial.savingsRub)
        if (dao.insertInitialState(initial.toEntity()) != -1L && openingAmount > 0) {
            dao.insertOperation(operation(
                "economy:opening-balance", openingAmount, FinancialOperationType.OPENING_BALANCE,
                OperationContext(reasonId = "initial-state"),
                initial.copy(availableRub = 0, savingsRub = 0), initial, now,
            ).toEntity())
        }
        return checkNotNull(dao.getState()).toDomain()
    }

    private suspend fun <T> atomic(block: suspend () -> T): T = mutex.withLock {
        transactionRunner.runInTransaction { block() }
    }

    private fun validate(state: EconomyState) {
        require(state.availableRub >= 0 && state.savingsRub >= 0 && state.debtRub >= 0)
        require(state.debtRub <= config.maximumDebtRub)
    }

    private fun EconomyState.snapshot() = FinancialSnapshot(availableRub, savingsRub, debtRub)
    private inline fun <T> Iterable<T>.sumExact(selector: (T) -> Long): Long = fold(0L) { total, item -> Math.addExact(total, selector(item)) }
}
