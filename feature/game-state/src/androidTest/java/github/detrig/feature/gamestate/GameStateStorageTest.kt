package github.detrig.feature.gamestate

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import github.detrig.core.database.RoomTransactionRunner
import github.detrig.feature.gamestate.api.GameStateApi
import github.detrig.feature.gamestate.data.local.GameStateDao
import github.detrig.feature.gamestate.data.local.GameStateEntity
import github.detrig.feature.gamestate.data.local.toEntity
import github.detrig.feature.gamestate.data.local.*
import github.detrig.feature.gamestate.domain.model.PetPlayCompletion
import github.detrig.feature.gamestate.di.GameStateModule
import github.detrig.feature.gamestate.domain.GameState
import github.detrig.feature.gamestate.domain.GameStateInitialConfig
import github.detrig.feature.gamestate.domain.PetState
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.economy.domain.*
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@Database(entities = [GameStateEntity::class, RoomZoneEntity::class, PetPlayEffectEntity::class], version = 8, exportSchema = false)
abstract class GameStateTestDatabase : RoomDatabase() {
    abstract fun gameStateDao(): GameStateDao
    abstract fun roomZoneDao(): RoomZoneDao
    abstract fun petPlayEffectDao(): PetPlayEffectDao
}

@RunWith(AndroidJUnit4::class)
class GameStateStorageTest {

    private lateinit var context: Context
    private lateinit var databaseName: String
    private lateinit var database: GameStateTestDatabase
    private val economyState = EconomyState(500, 0, 0, PeriodicIncome(500, 604_800_000, 123_456_789))
    private val economyApi = testEconomyApi()

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        databaseName = "game-state-test-${UUID.randomUUID()}.db"
        database = openDatabase()
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun observingEmptyStorageDoesNotCreateAGame() = runBlocking {
        val api = createApi()

        assertNull(withTimeout(5_000) { api.observeState().first() })
        assertNull(database.gameStateDao().getCurrentState())
    }

    @Test
    fun repeatedInitializationKeepsTheSameStateAndDeadline() = runBlocking {
        val api = createApi()
        val first = api.initialize()

        repeat(5) { assertEquals(first, api.initialize()) }
        assertEquals(GameStateInitialConfig().createState(), first)
    }

    @Test
    fun existingProgressIsNotReplacedByInitialSettings() = runBlocking {
        val existing = GameState(PetState(61, 47, 83, 91), 3)
        database.gameStateDao().insertInitialState(existing.toEntity())
        val api = createApi(
            config = GameStateInitialConfig(hunger = 1),
        )

        assertEquals(existing, api.initialize())
        assertEquals(existing, withTimeout(5_000) { api.observeState().first() })
    }

    @Test
    fun concurrentInitializersAgreeOnOnePersistedGame() = runBlocking {
        val results = withTimeout(10_000) {
            (1..20).map { index ->
                val api = createApi(
                    config = GameStateInitialConfig(hunger = index),
                )
                async(Dispatchers.IO) { api.initialize() }
            }.awaitAll()
        }

        assertEquals(1, results.distinct().size)
        assertEquals(results.first(), createApi().initialize())
    }

    @Test
    fun observersReceiveTheCommittedInitialState() = runBlocking {
        withTimeout(5_000) {
            val api = createApi()
            val initialEmission = CompletableDeferred<Unit>()
            val states = async(start = CoroutineStart.UNDISPATCHED) {
                api.observeState()
                    .onEach { if (it == null) initialEmission.complete(Unit) }
                    .take(2)
                    .toList()
            }
            initialEmission.await()
            val created = api.initialize()

            assertEquals(listOf(null, created), states.await())
        }
    }

    @Test
    fun reopeningTheDatabaseRestoresTheGameWithDifferentDependencies() = runBlocking {
        val saved = createApi().initialize()
        database.close()
        database = openDatabase()

        val restoredApi = createApi(
            config = GameStateInitialConfig(hunger = 0),
        )
        assertEquals(saved, restoredApi.initialize())
        assertEquals(saved, withTimeout(5_000) { restoredApi.observeState().first() })
    }

    @Test
    fun initializationFailureDoesNotCreateAPartialGame() = runBlocking {
        val failure = runCatching {
            createApi(config = GameStateInitialConfig(hunger = -1)).initialize()
        }.exceptionOrNull()

        assertNotNull(failure)
        assertNull(database.gameStateDao().getCurrentState())
        assertEquals(GameStateInitialConfig().createState(), createApi().initialize())
    }

    @Test
    fun readFailureIsPropagatedInsteadOfResettingTheGame() = runBlocking {
        val api = createApi()
        val saved = api.initialize()
        database.close()

        assertTrue(runCatching { api.initialize() }.isFailure)
        database = openDatabase()
        assertEquals(saved, createApi().initialize())
    }

    @Test
    fun concurrentRepeatedPlayRewardIsAppliedOnceWithoutChangingMoney() = runBlocking {
        val before = createApi().initialize()
        val moneyBefore = economyApi.getState()
        assertTrue(before.ownedZoneIds.isEmpty())
        val completion = PetPlayCompletion("current", "round-1", "fishing", true, 1)
        val deltas = (1..12).map { async(Dispatchers.IO) { createApi().completePetPlay(completion) } }.awaitAll()
        assertTrue(deltas.all { it == 3 })
        val after = createApi().initialize()
        assertEquals(before.pet.happiness + 3, after.pet.happiness)
        assertEquals(moneyBefore, economyApi.getState())
        assertEquals(before.ownedZoneIds, after.ownedZoneIds)
        assertTrue(economyApi.getHistory().isEmpty())
        database.close()
        database = openDatabase()
        assertEquals(3, createApi().completePetPlay(completion))
        assertEquals(after, createApi().initialize())
    }

    @Test
    fun playRewardIsCappedAndLockedGamesCannotGrantIt() = runBlocking {
        database.gameStateDao().insertInitialState(GameState(PetState(50, 50, 99, 100), 2).toEntity())
        val completion = PetPlayCompletion("current", "round-1", "football", true, 1)
        assertTrue(runCatching { createApi().completePetPlay(completion) }.isFailure)
        database.roomZoneDao().insert(RoomZoneEntity("current", "football", 1))
        assertEquals(1, createApi().completePetPlay(completion))
        assertEquals(100, createApi().initialize().pet.happiness)
        assertEquals(0, createApi().completePetPlay(completion.copy(sessionId = "round-2")))
    }

    @Test
    fun freeFishingDoesNotChargeForAStalePurchaseOfferAtLevelOne() = runBlocking {
        val before = createApi().initialize()
        assertEquals(1, before.playerLevel)
        val result = createApi().buyZone(github.detrig.feature.gamestate.domain.model.ZoneOffer("fishing", 400, 2))
        assertEquals(github.detrig.feature.gamestate.domain.model.ZoneBuyResult.AlreadyOwned, result)
        assertEquals(before, createApi().initialize())
        assertTrue(economyApi.getHistory().isEmpty())
    }

    private fun openDatabase(): GameStateTestDatabase {
        return Room.databaseBuilder(context, GameStateTestDatabase::class.java, databaseName).build()
    }

    @Test
    fun repeatedPlayEffectIsAtomicAndDoesNotChangeMoney() = runBlocking {
        val api = createApi(config = GameStateInitialConfig(happiness = 99))
        api.initialize()
        api.buyZone(github.detrig.feature.gamestate.domain.model.ZoneOffer("flight", 300, 1))
        val before = api.initialize()
        val moneyBefore = economyApi.getState()
        val completion = github.detrig.feature.gamestate.domain.model.PetPlayCompletion(
            "current", "flight-test", "flight", true, validActionCount = 20, activePlayMillis = 20_000)
        val deltas = (1..12).map { async { api.completePetPlay(completion) } }.awaitAll()
        assertTrue(deltas.all { it == 1 })
        assertEquals(100, api.initialize().pet.happiness)
        assertEquals(moneyBefore, economyApi.getState())
        database.close()
        database = openDatabase()
        assertEquals(1, createApi().completePetPlay(completion))
        assertEquals(100, createApi().initialize().pet.happiness)
        assertTrue(runCatching { createApi().completePetPlay(completion.copy(gameId = "another-game")) }.isFailure)
        assertTrue(runCatching { createApi().completePetPlay(completion.copy(validActionCount = -1)) }.isFailure)
    }

    @Test
    fun freeFlightWorksForExistingSaveWithoutPurchaseAndCannotBeCharged() = runBlocking {
        val existing = GameState(PetState(61, 47, 70, 91), 1)
        database.gameStateDao().insertInitialState(existing.toEntity())
        val api = createApi()
        assertEquals(github.detrig.feature.gamestate.domain.model.ZoneBuyResult.AlreadyOwned,
            api.buyZone(github.detrig.feature.gamestate.domain.model.ZoneOffer("flight", 300, 1)))
        assertEquals(existing, api.initialize())
        assertTrue(economyApi.getHistory().isEmpty())

        val completion = github.detrig.feature.gamestate.domain.model.PetPlayCompletion(
            "current", "free-flight", "flight", true, validActionCount = 20, activePlayMillis = 20_000)
        assertEquals(3, api.completePetPlay(completion))
        assertEquals(existing.copy(pet = existing.pet.copy(happiness = 73)), api.initialize())
        assertTrue(runCatching { api.completePetPlay(completion.copy(sessionId = "locked", gameId = "football")) }.isFailure)
        assertTrue(economyApi.getHistory().isEmpty())
    }

    private fun createApi(
        config: GameStateInitialConfig = GameStateInitialConfig(),
    ): GameStateApi {
        val testDatabase = database
        return GameStateModule(object : GameStateDependencies {
            override fun gameStateDao(): GameStateDao = testDatabase.gameStateDao()
            override fun roomZoneDao(): RoomZoneDao = testDatabase.roomZoneDao()
            override fun petPlayEffectDao(): PetPlayEffectDao = testDatabase.petPlayEffectDao()
            override fun economyApi(): EconomyApi = this@GameStateStorageTest.economyApi
            override fun transactionRunner(): RoomTransactionRunner = RoomTransactionRunner(testDatabase)
            override fun initialConfig(): GameStateInitialConfig = config
            override fun currentTimeMillis(): Long = 42L
        }).api
    }

    private fun testEconomyApi() = object : EconomyApi {
        override suspend fun initialize() = economyState
        override suspend fun getState() = economyState
        override fun observeState() = flowOf(economyState)
        override suspend fun canDebit(amountRub: Long) = economyState.availableRub >= amountRub
        override suspend fun credit(operationId: String, amountRub: Long, context: OperationContext): FinancialOperationResult = error("unused")
        override suspend fun debit(operationId: String, amountRub: Long, context: OperationContext): FinancialOperationResult = error("unexpected paid zone")
        override suspend fun createDebt(operationId: String, amountRub: Long, context: OperationContext): FinancialOperationResult = error("unused")
        override suspend fun repayDebt(operationId: String, amountRub: Long, context: OperationContext): FinancialOperationResult = error("unused")
        override suspend fun transferToSavings(operationId: String, amountRub: Long, context: OperationContext): FinancialOperationResult = error("unused")
        override suspend fun transferFromSavings(operationId: String, amountRub: Long, context: OperationContext): FinancialOperationResult = error("unused")
        override suspend fun configurePeriodicIncome(periodicIncome: PeriodicIncome) = economyState
        override suspend fun isPeriodicIncomeDue(atMillis: Long) = false
        override suspend fun processPeriodicIncome(atMillis: Long) = PeriodicIncomeResult(0, 0, 0, 0, emptyList(), economyState)
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
