package github.detrig.feature.week

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import github.detrig.core.database.RoomTransactionRunner
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.economy.domain.*
import github.detrig.feature.week.data.WeekRepository
import github.detrig.feature.week.data.local.WeekDao
import github.detrig.feature.week.data.local.WeekStateEntity
import github.detrig.feature.week.domain.EndDayResult
import github.detrig.feature.week.domain.EarlyWeekEndResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@Database(entities = [WeekStateEntity::class], version = 1, exportSchema = false)
abstract class WeekTestDatabase : RoomDatabase() { abstract fun weekDao(): WeekDao }

@RunWith(AndroidJUnit4::class)
class WeekRepositoryTest {
    @Test fun sevenBedActionsGrantOnceAndPersist() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, WeekTestDatabase::class.java).build()
        try {
            val economy = FakeEconomy()
            val repository = WeekRepository(db.weekDao(), economy, RoomTransactionRunner(db))
            assertEquals(1L, repository.initialize().absoluteDay)
            for (day in 1L..6L) {
                val result = repository.endDay(day) as EndDayResult.Advanced
                assertEquals(0L, result.allowanceReceivedRub)
            }
            assertEquals(0, economy.grants)
            val rollover = repository.endDay(7) as EndDayResult.Advanced
            assertEquals(500L, rollover.allowanceReceivedRub)
            assertEquals(8L, rollover.state.absoluteDay)
            assertEquals(2L, rollover.state.weekNumber)
            assertTrue(repository.endDay(7) is EndDayResult.AlreadyAdvanced)
            assertEquals(1, economy.grants)
            val restored = WeekRepository(db.weekDao(), economy, RoomTransactionRunner(db))
            assertEquals(8L, restored.initialize().absoluteDay)
        } finally {
            db.close()
        }
    }

    @Test fun failedAllowanceDoesNotAdvanceDay() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, WeekTestDatabase::class.java).build()
        try {
            val economy = FakeEconomy()
            val repository = WeekRepository(db.weekDao(), economy, RoomTransactionRunner(db))
            for (day in 1L..6L) repository.endDay(day)
            economy.fail = true
            assertTrue(runCatching { repository.endDay(7) }.isFailure)
            assertEquals(7L, repository.initialize().absoluteDay)
        } finally {
            db.close()
        }
    }

    @Test fun earlyFinishGrantsOnlyNetWeeklyAllowance() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, WeekTestDatabase::class.java).build()
        try {
            val economy = FakeEconomy(initialAvailableRub = 0).apply {
                allowanceRepaymentRub = 120
                activeParentHelp = ParentHelpState(
                    offerId = "quick",
                    receivedRub = 600,
                    totalRepaymentRub = 720,
                    remainingRub = 720,
                    paymentsRemaining = 2,
                )
            }
            val repository = WeekRepository(db.weekDao(), economy, RoomTransactionRunner(db))

            val result = repository.endWeekEarlyWithParentHelp(
                expectedAbsoluteDay = 1,
                minimumRequiredBalanceRub = 25,
            ) as EarlyWeekEndResult.Completed

            assertEquals(2L, result.state.weekNumber)
            assertEquals(1, result.state.dayOfWeek)
            assertEquals(6, result.skippedDays)
            assertEquals(500L, result.allowanceGrossRub)
            assertEquals(120L, result.parentHelpRepaidRub)
            assertEquals(380L, result.allowanceReceivedRub)
            assertEquals(1, economy.grants)
            assertEquals(0, economy.zeroBalanceHelpCalls)
        } finally {
            db.close()
        }
    }

    private class FakeEconomy(initialAvailableRub: Long = 500) : EconomyApi {
        var grants = 0
        var fail = false
        var allowanceRepaymentRub = 0L
        var zeroBalanceHelpCalls = 0
        var activeParentHelp: ParentHelpState? = null
        private val state = EconomyConfig().initialState(0).copy(availableRub = initialAvailableRub)

        override suspend fun grantWeeklyAllowance(weekNumber: Long): WeeklyAllowanceResult {
            if (fail) error("Storage failed")
            grants++
            return WeeklyAllowanceResult(
                weekNumber = weekNumber,
                grossRub = 500,
                debtRepaidRub = allowanceRepaymentRub,
                state = state,
                alreadyApplied = false,
                parentHelpRepaidRub = allowanceRepaymentRub,
            )
        }
        override suspend fun provideZeroBalanceHelp(minimumRequiredBalanceRub: Long): ZeroBalanceHelpResult {
            zeroBalanceHelpCalls++
            return ZeroBalanceHelpResult.Granted(500, state)
        }
        override suspend fun getParentHelp(): ParentHelpState? = activeParentHelp
        override suspend fun initialize() = state
        override suspend fun getState() = state
        override fun observeState(): Flow<EconomyState> = flowOf(state)
        override suspend fun canDebit(amountRub: Long) = error("unused")
        override suspend fun credit(operationId: String, amountRub: Long, context: OperationContext): FinancialOperationResult = error("unused")
        override suspend fun debit(operationId: String, amountRub: Long, context: OperationContext): FinancialOperationResult = error("unused")
        override suspend fun createDebt(operationId: String, amountRub: Long, context: OperationContext): FinancialOperationResult = error("unused")
        override suspend fun repayDebt(operationId: String, amountRub: Long, context: OperationContext): FinancialOperationResult = error("unused")
        override suspend fun transferToSavings(operationId: String, amountRub: Long, context: OperationContext): FinancialOperationResult = error("unused")
        override suspend fun transferFromSavings(operationId: String, amountRub: Long, context: OperationContext): FinancialOperationResult = error("unused")
        override suspend fun configurePeriodicIncome(periodicIncome: PeriodicIncome) = state
        override suspend fun isPeriodicIncomeDue(atMillis: Long) = false
        override suspend fun processPeriodicIncome(atMillis: Long): PeriodicIncomeResult = error("unused")
        override suspend fun getHistory(filter: HistoryFilter) = emptyList<FinancialOperation>()
        override suspend fun getIncomeHistory(filter: HistoryFilter) = emptyList<FinancialOperation>()
        override suspend fun getExpenseHistory(filter: HistoryFilter) = emptyList<FinancialOperation>()
        override suspend fun getDebtHistory(filter: HistoryFilter) = emptyList<FinancialOperation>()
        override suspend fun getSavingsHistory(filter: HistoryFilter) = emptyList<FinancialOperation>()
        override suspend fun getSummary(filter: HistoryFilter): FinancialSummary = error("unused")
        override suspend fun saveGoal(goal: SavingsGoal) = goal
        override suspend fun deleteGoal(id: String) = false
        override suspend fun getGoals() = emptyList<SavingsGoal>()
        override suspend fun getActiveGoal(): SavingsGoal? = null
        override suspend fun getGoalProgress(id: String): SavingsGoalProgress? = null
    }
}
