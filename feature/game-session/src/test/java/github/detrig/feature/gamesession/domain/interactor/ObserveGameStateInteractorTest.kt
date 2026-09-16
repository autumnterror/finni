package github.detrig.feature.gamesession.domain.interactor

import github.detrig.feature.gamestate.api.GameStateApi
import github.detrig.feature.gamestate.domain.GameState
import github.detrig.feature.gamestate.domain.PetState
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.economy.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveGameStateInteractorTest {

    private val initial = GameState(PetState(20, 80, 70, 100), 1)
    private val economy = EconomyConfig().initialState(0)

    @Test
    fun initializesBeforeObservingAndForwardsSubsequentChanges() = runBlocking {
        val updated = initial.copy(playerLevel = 2)
        var initialized = false
        val api = object : ObserveOnlyApi() {
            override suspend fun initialize(): GameState {
                initialized = true
                return initial
            }

            override fun observeState(): Flow<GameState?> {
                check(initialized)
                return flowOf(initial, updated)
            }
        }

        assertEquals(
            listOf(GameSessionState(initial, economy), GameSessionState(updated, economy)),
            ObserveGameStateInteractor(api, economyApi()).invoke().toList(),
        )
    }

    @Test
    fun initializationFailureReachesTheScreenInsteadOfEmittingDefaults() = runBlocking {
        val failure = IllegalStateException("Storage unavailable")
        val api = object : ObserveOnlyApi() {
            override suspend fun initialize(): GameState = throw failure
            override fun observeState(): Flow<GameState?> = error("Must not observe after failure")
        }

        assertEquals(failure, runCatching { ObserveGameStateInteractor(api, economyApi())().toList() }.exceptionOrNull())
    }

    @Test
    fun missingStateAfterInitializationIsAnError() = runBlocking {
        val api = object : ObserveOnlyApi() {
            override suspend fun initialize(): GameState = initial
            override fun observeState(): Flow<GameState?> = flowOf(null)
        }

        assertTrue(runCatching { ObserveGameStateInteractor(api, economyApi())().toList() }.isFailure)
    }

    private fun economyApi() = object : EconomyApi {
        override suspend fun initialize() = economy
        override suspend fun getState() = economy
        override fun observeState() = flowOf(economy)
        override suspend fun canDebit(amountRub: Long) = true
        override suspend fun credit(operationId: String, amountRub: Long, context: OperationContext): FinancialOperationResult = error("unused")
        override suspend fun debit(operationId: String, amountRub: Long, context: OperationContext): FinancialOperationResult = error("unused")
        override suspend fun createDebt(operationId: String, amountRub: Long, context: OperationContext): FinancialOperationResult = error("unused")
        override suspend fun repayDebt(operationId: String, amountRub: Long, context: OperationContext): FinancialOperationResult = error("unused")
        override suspend fun transferToSavings(operationId: String, amountRub: Long, context: OperationContext): FinancialOperationResult = error("unused")
        override suspend fun transferFromSavings(operationId: String, amountRub: Long, context: OperationContext): FinancialOperationResult = error("unused")
        override suspend fun configurePeriodicIncome(periodicIncome: PeriodicIncome) = economy
        override suspend fun isPeriodicIncomeDue(atMillis: Long) = false
        override suspend fun processPeriodicIncome(atMillis: Long) = PeriodicIncomeResult(0, 0, 0, 0, emptyList(), economy)
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
    private abstract class ObserveOnlyApi : GameStateApi {
        override suspend fun buyZone(offer: github.detrig.feature.gamestate.domain.model.ZoneOffer): github.detrig.feature.gamestate.domain.model.ZoneBuyResult = error("Not used by observation")
        override suspend fun completePetPlay(completion: github.detrig.feature.gamestate.domain.model.PetPlayCompletion): Int = error("Not used by observation")
    }
}
