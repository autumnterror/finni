package github.detrig.feature.week.data

import github.detrig.core.database.RoomTransactionRunner
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.week.api.WeekApi
import github.detrig.feature.week.data.local.WeekDao
import github.detrig.feature.week.data.local.WeekStateEntity
import github.detrig.feature.week.domain.EndDayResult
import github.detrig.feature.week.domain.EarlyWeekEndResult
import github.detrig.feature.week.domain.WeekState
import github.detrig.feature.economy.domain.ZeroBalanceHelpResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

internal class WeekRepository(
    private val dao: WeekDao,
    private val economyApi: EconomyApi,
    private val transactionRunner: RoomTransactionRunner,
) : WeekApi {
    override suspend fun initialize(): WeekState = transactionRunner.runInTransaction { ensureState() }

    override fun observeState(): Flow<WeekState> = dao.observeState().filterNotNull().map { WeekState(it.absoluteDay) }

    override suspend fun endDay(expectedAbsoluteDay: Long): EndDayResult = transactionRunner.runInTransaction {
        val current = ensureState()
        if (current.absoluteDay != expectedAbsoluteDay) return@runInTransaction EndDayResult.AlreadyAdvanced(current)
        val next = WeekState(Math.addExact(current.absoluteDay, 1))
        val allowance = if (next.dayOfWeek == 1) {
            economyApi.grantWeeklyAllowance(next.weekNumber)
        } else null
        check(dao.advance(expectedAbsoluteDay, next.absoluteDay) == 1)
        EndDayResult.Advanced(
            state = next,
            allowanceReceivedRub = allowance?.receivedRub ?: 0,
            allowanceGrossRub = allowance?.grossRub ?: 0,
            parentHelpRepaidRub = allowance?.parentHelpRepaidRub ?: 0,
        )
    }

    override suspend fun endWeekEarlyWithParentHelp(
        expectedAbsoluteDay: Long,
        minimumRequiredBalanceRub: Long,
    ): EarlyWeekEndResult = transactionRunner.runInTransaction {
        val current = ensureState()
        if (current.absoluteDay != expectedAbsoluteDay) {
            return@runInTransaction EarlyWeekEndResult.AlreadyCompleted(current)
        }
        val help = economyApi.provideZeroBalanceHelp(minimumRequiredBalanceRub)
        if (help is ZeroBalanceHelpResult.NotNeeded) {
            return@runInTransaction EarlyWeekEndResult.NotNeeded(current)
        }
        help as ZeroBalanceHelpResult.Granted
        val next = WeekState(current.weekNumber * WeekState.DAYS_PER_WEEK + 1)
        val allowance = economyApi.grantWeeklyAllowance(next.weekNumber)
        check(dao.advance(expectedAbsoluteDay, next.absoluteDay) == 1)
        EarlyWeekEndResult.Completed(
            state = next,
            skippedDays = (WeekState.DAYS_PER_WEEK - current.dayOfWeek).toInt(),
            parentHelpRub = help.amountRub,
            allowanceReceivedRub = allowance.receivedRub,
            allowanceGrossRub = allowance.grossRub,
            parentHelpRepaidRub = allowance.parentHelpRepaidRub,
        )
    }

    private suspend fun ensureState(): WeekState {
        dao.getState()?.let { return WeekState(it.absoluteDay) }
        dao.insertInitial(WeekStateEntity(absoluteDay = 1))
        return WeekState(checkNotNull(dao.getState()).absoluteDay)
    }
}
