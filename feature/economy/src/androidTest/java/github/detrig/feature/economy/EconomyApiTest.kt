package github.detrig.feature.economy

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import github.detrig.core.database.RoomTransactionRunner
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.economy.data.local.EconomyDao
import github.detrig.feature.economy.data.local.EconomyStateEntity
import github.detrig.feature.economy.data.local.FinancialOperationEntity
import github.detrig.feature.economy.data.local.SavingsGoalEntity
import github.detrig.feature.economy.di.EconomyModule
import github.detrig.feature.economy.domain.EconomyConfig
import github.detrig.feature.economy.domain.FinancialOperationResult
import github.detrig.feature.economy.domain.FinancialOperationType
import github.detrig.feature.economy.domain.HistoryFilter
import github.detrig.feature.economy.domain.OperationContext
import github.detrig.feature.economy.domain.PeriodicIncome
import github.detrig.feature.economy.domain.RejectionReason
import github.detrig.feature.economy.domain.SavingsGoal
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@Database(
    entities = [EconomyStateEntity::class, FinancialOperationEntity::class, SavingsGoalEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class EconomyTestDatabase : RoomDatabase() { abstract fun dao(): EconomyDao }

@RunWith(AndroidJUnit4::class)
class EconomyApiTest {
    private lateinit var context: Context
    private lateinit var databaseName: String
    private lateinit var database: EconomyTestDatabase
    private var now = 1_000L

    @Before fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        databaseName = "economy-${UUID.randomUUID()}.db"
        database = openDatabase()
    }

    @After fun tearDown() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    @Test fun initialStateIsStableObservableAndRestored() = runBlocking {
        val api = api()
        val initial = api.initialize()
        assertEquals(500L, initial.availableRub)
        assertEquals(0L, initial.savingsRub)
        assertEquals(0L, initial.debtRub)
        assertEquals(500L, initial.periodicIncome.amountRub)
        assertEquals(initial, api.observeState().first())

        database.close()
        database = openDatabase()
        now = 99_000L
        assertEquals(initial, api(EconomyConfig(initialAvailableRub = 1)).initialize())
    }

    @Test fun creditsDebitsFailuresAndIdempotencyAreAtomic() = runBlocking {
        val api = api()
        val credit = api.credit("gift-1", 200, OperationContext("gift", "source=family"))
        assertTrue(credit is FinancialOperationResult.Applied)
        assertEquals(700L, api.getState().availableRub)
        assertTrue(api.credit("gift-1", 200) is FinancialOperationResult.AlreadyApplied)
        assertEquals(700L, api.getState().availableRub)
        assertEquals(
            RejectionReason.OPERATION_ID_CONFLICT,
            (api.credit("gift-1", 201) as FinancialOperationResult.Rejected).reason,
        )

        assertTrue(api.canDebit(650))
        assertTrue(api.debit("purchase-1", 650) is FinancialOperationResult.Applied)
        val rejected = api.debit("purchase-2", 51) as FinancialOperationResult.Rejected
        assertEquals(RejectionReason.INSUFFICIENT_AVAILABLE_FUNDS, rejected.reason)
        assertEquals(50L, api.getState().availableRub)
        assertTrue(api.debit("purchase-1", 650) is FinancialOperationResult.AlreadyApplied)
        assertEquals(2, api.getHistory().size)
    }

    @Test fun invalidAmountsAndIdsNeverChangeState() = runBlocking {
        val api = api()
        val initial = api.initialize()
        listOf(
            api.credit("zero", 0), api.credit("negative", -1), api.debit("", 1),
            api.createDebt("debt-zero", 0), api.transferToSavings("save-zero", 0),
        ).forEach { assertTrue(it is FinancialOperationResult.Rejected) }
        assertEquals(initial, api.getState())
        assertTrue(api.getHistory().isEmpty())
        assertThrows(IllegalArgumentException::class.java) { EconomyConfig(periodicIncomeAmountRub = 0) }
    }

    @Test fun missedPeriodicIncomeIsCaughtUpOnce() = runBlocking {
        val api = api(EconomyConfig(
            initialAvailableRub = 0,
            periodicIncomeAmountRub = 100,
            firstPeriodicIncomeDelayMillis = 100,
            periodicIncomePeriodMillis = 100,
        ))
        api.initialize()
        val result = api.processPeriodicIncome(1_350)
        assertEquals(3, result.processedCycles)
        assertEquals(300L, result.grossIncomeRub)
        assertEquals(300L, result.receivedRub)
        assertEquals(300L, result.state.availableRub)
        assertEquals(1_400L, result.state.periodicIncome.nextAtMillis)
        assertFalse(api.isPeriodicIncomeDue(1_350))
        assertEquals(0, api.processPeriodicIncome(1_350).processedCycles)
        assertEquals(3, api.getHistory().size)
    }

    @Test fun debtRulesAutoRepaymentAndEarlyRepaymentAreExplicit() = runBlocking {
        val api = api(EconomyConfig(
            initialAvailableRub = 0,
            periodicIncomeAmountRub = 100,
            firstPeriodicIncomeDelayMillis = 100,
            periodicIncomePeriodMillis = 100,
            maximumDebtRub = 250,
        ))
        assertTrue(api.createDebt("too-big", 251) is FinancialOperationResult.Rejected)
        assertTrue(api.createDebt("loan", 220) is FinancialOperationResult.Applied)
        assertEquals(220L, api.getState().debtRub)
        assertEquals(RejectionReason.ACTIVE_DEBT_EXISTS, (api.createDebt("second", 1) as FinancialOperationResult.Rejected).reason)

        val periodic = api.processPeriodicIncome(1_100)
        assertEquals(100L, periodic.grossIncomeRub)
        assertEquals(100L, periodic.debtRepaidRub)
        assertEquals(0L, periodic.receivedRub)
        assertEquals(120L, periodic.state.debtRub)
        assertTrue(periodic.operations.any { it.type == FinancialOperationType.DEBT_AUTO_REPAYMENT })

        val early = api.repayDebt("early", 500)
        assertTrue(early is FinancialOperationResult.Applied)
        assertEquals(0L, api.getState().debtRub)
        assertEquals(100L, api.getState().availableRub)
        assertEquals(RejectionReason.NO_ACTIVE_DEBT, (api.repayDebt("again", 1) as FinancialOperationResult.Rejected).reason)
    }

    @Test fun savingsTransfersConserveMoneyAndDriveGoalProgress() = runBlocking {
        val api = api()
        val total = api.initialize().totalMoneyRub
        assertTrue(api.transferToSavings("save", 300) is FinancialOperationResult.Applied)
        assertEquals(total, api.getState().totalMoneyRub)
        assertEquals(300L, api.getState().savingsRub)
        assertEquals(RejectionReason.INSUFFICIENT_AVAILABLE_FUNDS, (api.transferToSavings("save-more", 201) as FinancialOperationResult.Rejected).reason)
        assertEquals(RejectionReason.INSUFFICIENT_SAVINGS, (api.transferFromSavings("withdraw-too-much", 301) as FinancialOperationResult.Rejected).reason)
        api.transferFromSavings("withdraw", 50)
        assertEquals(total, api.getState().totalMoneyRub)

        api.saveGoal(SavingsGoal("bike", "Велосипед", 300, "roomItem=bike"))
        var progress = api.getGoalProgress("bike")!!
        assertEquals(50L, progress.remainingRub)
        assertFalse(progress.isReached)
        api.transferToSavings("finish", 50)
        progress = api.getGoalProgress("bike")!!
        assertTrue(progress.isReached)
        assertEquals(0L, progress.remainingRub)
        assertEquals("bike", api.getActiveGoal()?.id)
        assertTrue(api.deleteGoal("bike"))
        assertNull(api.getGoalProgress("bike"))
    }

    @Test fun historyFiltersAndSummaryExcludeInternalTransfersAndDebtFromIncome() = runBlocking {
        val api = api()
        now = 10
        api.credit("income", 100)
        now = 20
        api.debit("expense", 40)
        now = 30
        api.transferToSavings("saving", 20)
        now = 40
        api.createDebt("loan", 50)

        assertEquals(listOf("expense", "saving"), api.getHistory(HistoryFilter(15, 35)).map { it.id })
        assertEquals(listOf("expense"), api.getHistory(HistoryFilter(types = setOf(FinancialOperationType.DEBIT))).map { it.id })
        assertEquals(listOf("income"), api.getIncomeHistory().map { it.id })
        assertEquals(listOf("expense"), api.getExpenseHistory().map { it.id })
        assertEquals(listOf("saving"), api.getSavingsHistory().map { it.id })
        assertEquals(listOf("loan"), api.getDebtHistory().map { it.id })
        val summary = api.getSummary()
        assertEquals(100L, summary.totalIncomeRub)
        assertEquals(40L, summary.totalExpensesRub)
        assertEquals(90L, summary.change.availableDeltaRub)
        assertEquals(20L, summary.change.savingsDeltaRub)
        assertEquals(50L, summary.change.debtDeltaRub)
        assertEquals(60L, summary.change.netWorthDeltaRub)
    }

    @Test fun periodicConfigurationCanBeChanged() = runBlocking {
        val api = api()
        val updated = api.configurePeriodicIncome(PeriodicIncome(75, 1_000, 5_000))
        assertEquals(PeriodicIncome(75, 1_000, 5_000), updated.periodicIncome)
        assertFalse(api.isPeriodicIncomeDue(4_999))
        assertTrue(api.isPeriodicIncomeDue(5_000))
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { api.configurePeriodicIncome(PeriodicIncome(0, 1, 1)) }
        }
    }

    private fun openDatabase() = Room.databaseBuilder(context, EconomyTestDatabase::class.java, databaseName).build()

    private fun api(config: EconomyConfig = EconomyConfig()): EconomyApi {
        val db = database
        return EconomyModule(object : EconomyDependencies {
            override fun economyDao() = db.dao()
            override fun transactionRunner() = RoomTransactionRunner(db)
            override fun config() = config
            override fun currentTimeMillis() = now
        }).api
    }
}
