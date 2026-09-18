package github.detrig.feature.productmarket.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import github.detrig.core.exception.CoreErrorHandler
import github.detrig.core.exception.mapper.CoreExceptionMapper
import github.detrig.core.infrastructure.network.NetworkManager
import github.detrig.feature.productmarket.api.*
import github.detrig.feature.productmarket.domain.*
import github.detrig.feature.productmarket.navigation.MarketRouter
import github.detrig.products.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class MarketViewModelTest {
    @get:Rule val liveData = InstantTaskExecutorRule()
    private val dispatcher = StandardTestDispatcher()
    private val rules = MarketRules(MarketConfiguration(), DefaultProductCatalog()) { "trip" }
    private val repository = FakeRepository()
    private val host = FakeHost()
    private var backCount = 0
    private lateinit var vm: MarketViewModel

    @Before fun before() {
        Dispatchers.setMain(dispatcher)
        CoreErrorHandler.init(handler = { throw it }, exceptionMapper = CoreExceptionMapper(
            object : NetworkManager { override fun isNetworkAvailable() = true }))
        vm = MarketViewModel(rules, DefaultProductCatalog(), repository,
            RestoreMarketTripInteractor(repository, rules), ObserveMarketBalanceInteractor(host),
            CheckoutMarketTripInteractor(repository, rules, DefaultProductCatalog(), host), host, object : MarketRouter {
                override fun open() = Unit
                override fun back() { backCount++ }
            })
    }
    @After fun after() { Dispatchers.resetMain() }

    private fun start() {
        vm.perform(MarketViewEvent.Load)
        dispatcher.scheduler.runCurrent()
        vm.perform(MarketViewEvent.Viewport(360.0))
        vm.perform(MarketViewEvent.Foreground(true))
    }
    private fun state() = requireNotNull(vm.state().value)

    @Test fun cartAndBackgroundPauseTravelAndLiveBalanceComesFromHost() {
        start()
        assertEquals(420, state().balanceRub)
        vm.perform(MarketViewEvent.Frame(.1))
        val distance = state().trip!!.distance
        vm.perform(MarketViewEvent.OpenCart)
        vm.perform(MarketViewEvent.Frame(100.0))
        assertEquals(distance, state().trip!!.distance, 0.0)
        vm.perform(MarketViewEvent.CloseCart)
        vm.perform(MarketViewEvent.Foreground(false))
        vm.perform(MarketViewEvent.Frame(100.0))
        assertEquals(distance, state().trip!!.distance, 0.0)
        vm.perform(MarketViewEvent.Foreground(true))
        vm.perform(MarketViewEvent.Frame(.1))
        assertTrue(state().trip!!.distance > distance)
        host.balance.value = 500
        dispatcher.scheduler.runCurrent()
        assertEquals(500, state().balanceRub)
    }

    @Test fun finishTapIsSerializedAndChargesCartOnce() {
        repository.stored = rules.newTrip().copy(
            phase = MarketPhase.CHECKOUT,
            distance = rules.config.endDistance,
            cart = mapOf(ProductIds.Carrot to 1),
        )
        start()
        repository.saves = 0
        vm.perform(MarketViewEvent.Finish)
        vm.perform(MarketViewEvent.Finish)
        dispatcher.scheduler.runCurrent()
        assertEquals(1, repository.saves)
        assertEquals(MarketPhase.FINISHED, state().trip?.phase)
        assertEquals(410, host.balance.value)
        assertEquals(1, host.paymentCalls)
    }

    @Test fun insufficientFundsKeepCartAtCheckoutAndShowShortfall() {
        host.balance.value = 5
        repository.stored = rules.newTrip().copy(
            phase = MarketPhase.CHECKOUT,
            distance = rules.config.endDistance,
            cart = mapOf(ProductIds.Carrot to 1),
        )
        start()
        vm.perform(MarketViewEvent.Finish)
        dispatcher.scheduler.runCurrent()
        assertEquals(MarketPhase.CHECKOUT, state().trip?.phase)
        assertEquals(5L, state().paymentMissingRub)
        assertEquals(5, host.balance.value)
    }

    @Test fun cartCanBecomeSavingsGoalWithoutChargingIt() {
        repository.stored = rules.newTrip().copy(
            phase = MarketPhase.CHECKOUT,
            distance = rules.config.endDistance,
            cart = mapOf(ProductIds.Carrot to 1),
        )
        start()
        vm.perform(MarketViewEvent.SaveCartAsGoal)
        dispatcher.scheduler.runCurrent()
        assertTrue(state().goalSaved)
        assertEquals(1, host.goalCalls)
        assertEquals(420, host.balance.value)
    }

    @Test fun backClosesCartFirstAndConfirmedExitSavesBeforeNavigatingOnce() {
        start()
        vm.perform(MarketViewEvent.Pick("0:0:0"))
        vm.perform(MarketViewEvent.OpenCart)
        vm.perform(MarketViewEvent.Back)
        assertFalse(state().cartOpen)
        assertEquals(0, backCount)
        vm.perform(MarketViewEvent.Back)
        assertTrue(state().exitConfirmationOpen)
        vm.perform(MarketViewEvent.ConfirmExit)
        vm.perform(MarketViewEvent.ConfirmExit)
        assertTrue(state().busy)
        assertEquals(0, backCount)
        dispatcher.scheduler.runCurrent()
        assertEquals(1, backCount)
        assertEquals(1, repository.stored?.cart?.get(ProductIds.Carrot))
    }

    @Test fun cancelledExitKeepsCartAndPausesTravelUntilDismissed() {
        start()
        vm.perform(MarketViewEvent.ConfirmExit)
        dispatcher.scheduler.runCurrent()
        assertEquals(0, backCount)
        vm.perform(MarketViewEvent.Pick("0:0:0"))
        val before = state().trip
        vm.perform(MarketViewEvent.Back)
        vm.perform(MarketViewEvent.Frame(.1))
        vm.perform(MarketViewEvent.Pick("0:0:1"))
        assertEquals(before, state().trip)
        vm.perform(MarketViewEvent.CancelExit)
        assertFalse(state().exitConfirmationOpen)
        vm.perform(MarketViewEvent.Frame(.1))
        assertTrue(state().trip!!.distance > before!!.distance)
        assertEquals(before.cart, state().trip!!.cart)
        assertEquals(0, backCount)
    }

    @Test fun failedExitSaveKeepsPlayerInMarketAndAllowsRetry() {
        start()
        vm.perform(MarketViewEvent.Pick("0:0:0"))
        repository.failSave = true
        vm.perform(MarketViewEvent.Back)
        vm.perform(MarketViewEvent.ConfirmExit)
        dispatcher.scheduler.runCurrent()
        assertEquals(0, backCount)
        assertEquals(MarketError.SAVE, state().error)
        assertFalse(state().busy)
        assertFalse(state().exitConfirmationOpen)
        repository.failSave = false
        vm.perform(MarketViewEvent.Retry)
        dispatcher.scheduler.runCurrent()
        assertNull(state().error)
        vm.perform(MarketViewEvent.Back)
        vm.perform(MarketViewEvent.ConfirmExit)
        dispatcher.scheduler.runCurrent()
        assertEquals(1, backCount)
        assertEquals(1, repository.stored?.cart?.get(ProductIds.Carrot))
        assertFalse(state().busy)
        assertFalse(state().exitConfirmationOpen)
        vm.perform(MarketViewEvent.ConfirmExit)
        dispatcher.scheduler.runCurrent()
        assertEquals(1, backCount)
    }

    @Test fun backgroundDuringFinishCannotQueueOldCheckoutAfterResult() {
        repository.stored = rules.newTrip().copy(phase = MarketPhase.CHECKOUT, distance = rules.config.endDistance)
        start()
        val checkpoints = repository.checkpoints
        vm.perform(MarketViewEvent.Finish)
        vm.perform(MarketViewEvent.Foreground(false))
        assertEquals(checkpoints, repository.checkpoints)
        dispatcher.scheduler.runCurrent()
        assertEquals(MarketPhase.FINISHED, repository.stored?.phase)
    }

    @Test fun failedSavePausesTripAndCanBeRetried() {
        start()
        vm.perform(MarketViewEvent.Pick("0:0:0"))
        repository.failed.value = true
        dispatcher.scheduler.runCurrent()
        val before = state().trip
        vm.perform(MarketViewEvent.Frame(.1))
        assertEquals(before, state().trip)
        assertEquals(MarketError.SAVE, state().error)
        vm.perform(MarketViewEvent.Retry)
        dispatcher.scheduler.runCurrent()
        assertNull(state().error)
        assertEquals(before, repository.stored)
    }

    private class FakeHost : ProductMarketHost {
        val balance = MutableStateFlow(420)
        var paymentCalls = 0
        var goalCalls = 0
        override suspend fun preparePlayer() = Unit
        override fun observeBalanceRub(): Flow<Int> = balance
        override suspend fun payForCart(tripId: String, totalRub: Long): MarketPaymentResult {
            paymentCalls++
            return if (balance.value < totalRub) MarketPaymentResult.InsufficientFunds(totalRub - balance.value)
            else {
                balance.value = (balance.value - totalRub).toInt()
                MarketPaymentResult.Paid
            }
        }
        override suspend fun saveCartAsGoal(tripId: String, totalRub: Long): MarketSavingsGoalResult {
            goalCalls++
            return MarketSavingsGoalResult.GoalSaved
        }
    }
    private class FakeRepository : MarketTripRepository {
        var stored: MarketTrip? = null
        var saves = 0
        var checkpoints = 0
        var failSave = false
        val failed = MutableStateFlow(false)
        override val saveFailed: StateFlow<Boolean> = failed
        override suspend fun load() = stored
        override suspend fun save(trip: MarketTrip) {
            if (failSave) error("disk unavailable")
            stored = trip; saves++; failed.value = false
        }
        override fun checkpoint(trip: MarketTrip) { stored = trip; checkpoints++ }
        override fun observeLastResult(): Flow<MarketTripResult?> = flowOf(stored?.lastResult)
    }
}
