package github.detrig.feature.productmarket.presentation

import github.detrig.core.mvvm.CoreViewModel
import github.detrig.core.mvvm.ExceptionConsumer
import github.detrig.feature.productmarket.domain.*
import github.detrig.feature.productmarket.api.MarketSavingsGoalResult
import github.detrig.feature.productmarket.api.ProductMarketHost
import github.detrig.feature.productmarket.navigation.MarketRouter
import github.detrig.products.ProductCatalog
import kotlinx.coroutines.Job

internal class MarketViewModel(
    val rules: MarketRules,
    val catalog: ProductCatalog,
    private val repository: MarketTripRepository,
    private val restoreTrip: RestoreMarketTripInteractor,
    private val observeBalance: ObserveMarketBalanceInteractor,
    private val checkoutTrip: CheckoutMarketTripInteractor,
    private val host: ProductMarketHost,
    private val router: MarketRouter,
) : CoreViewModel<MarketViewState, MarketViewEvent>(MarketViewState()) {
    private var balanceJob: Job? = null
    private var loaded = false
    private var viewportWidth = 0.0
    private var checkpointSeconds = 0.0
    private var pickupSequence = 0L

    init {
        launchCoroutine {
            repository.saveFailed.collect { failed ->
                if (failed) updateState { copy(error = MarketError.SAVE, exitConfirmationOpen = false) }
            }
        }
    }

    override fun perform(viewEvent: MarketViewEvent) {
        when (viewEvent) {
            MarketViewEvent.Load -> if (!loaded) load()
            MarketViewEvent.Retry -> retry()
            is MarketViewEvent.Viewport -> viewportWidth = viewEvent.width
            is MarketViewEvent.Frame -> frame(viewEvent.seconds)
            is MarketViewEvent.Foreground -> {
                updateState { copy(foreground = viewEvent.active) }
                // Во время завершения уже записывается новый результат: не ставим
                // за ним старый снимок CHECKOUT при сворачивании приложения.
                if (!viewEvent.active && !stateData.busy) stateData.trip?.let(repository::checkpoint)
            }
            is MarketViewEvent.Pick -> pick(viewEvent.instanceId)
            is MarketViewEvent.Remove -> if (!stateData.busy && stateData.error == null &&
                (stateData.cartOpen || stateData.trip?.phase == MarketPhase.CHECKOUT)) {
                changeTrip { rules.remove(it, viewEvent.productId) }
            }
            MarketViewEvent.OpenCart -> if (!stateData.busy && stateData.error == null &&
                stateData.trip?.phase == MarketPhase.WALKING) updateState { copy(cartOpen = true) }
            MarketViewEvent.CloseCart -> updateState { copy(cartOpen = false) }
            MarketViewEvent.AnotherPass -> if (!stateData.busy && stateData.error == null) changeTrip(rules::anotherPass)
            MarketViewEvent.Finish -> finish()
            MarketViewEvent.SaveCartAsGoal -> saveCartAsGoal()
            MarketViewEvent.NewTrip -> if (stateData.trip?.phase == MarketPhase.FINISHED &&
                !stateData.busy && stateData.error == null) changeTrip { rules.newTrip(it.lastResult) }
            MarketViewEvent.Back -> back()
            MarketViewEvent.CancelExit -> if (!stateData.busy) updateState { copy(exitConfirmationOpen = false) }
            MarketViewEvent.ConfirmExit -> confirmExit()
            MarketViewEvent.LeaveAfterError -> if (stateData.error != null && !stateData.busy) router.back()
        }
    }

    private fun failure(kind: MarketError) = ExceptionConsumer {
        updateState { copy(loading = false, busy = false, error = kind, exitConfirmationOpen = false) }
        true
    }

    private fun load() {
        loaded = true
        updateState { copy(loading = true, error = null) }
        launchCoroutine(failure(MarketError.LOAD)) {
            val trip = restoreTrip()
            updateState { copy(trip = trip, loading = false) }
            startBalance()
        }
    }

    private fun startBalance() {
        balanceJob?.cancel()
        balanceJob = launchCoroutine(failure(MarketError.BALANCE)) {
            observeBalance().collect { value -> updateState { copy(balanceRub = value) } }
        }
    }

    private fun retry() {
        if (stateData.busy || stateData.loading) return
        when (stateData.error) {
            MarketError.LOAD -> load()
            MarketError.BALANCE -> { updateState { copy(error = null) }; startBalance() }
            MarketError.SAVE -> {
                val trip = stateData.trip ?: return
                updateState { copy(busy = true) }
                launchCoroutine(failure(MarketError.SAVE)) {
                    repository.save(trip)
                    updateState { copy(busy = false, error = null) }
                }
            }
            null -> Unit
        }
    }

    private fun frame(seconds: Double) {
        if (!stateData.canAdvance) return
        val before = stateData.trip ?: return
        val after = rules.advance(before, seconds)
        if (before == after) return
        updateState { copy(trip = after, paymentMissingRub = null, goalSaved = false) }
        checkpointSeconds += seconds.coerceIn(0.0, .1)
        if (checkpointSeconds >= 1.0 || before.phase != after.phase) {
            checkpointSeconds = 0.0
            repository.checkpoint(after)
        }
    }

    private fun pick(instanceId: String) {
        if (!stateData.canAdvance) return
        val before = stateData.trip ?: return
        val after = rules.pick(before, instanceId, viewportWidth)
        if (after == before) return
        updateState { copy(trip = after, paymentMissingRub = null, goalSaved = false) }
        repository.checkpoint(after)
        val slot = rules.config.slots.first { it.instanceId(before.lap) == instanceId }
        commands.onNext(MarketViewCommand.Pickup(++pickupSequence, slot, before.distance))
    }

    private fun changeTrip(change: (MarketTrip) -> MarketTrip) {
        val before = stateData.trip ?: return
        val after = change(before)
        if (before == after) return
        updateState { copy(trip = after, paymentMissingRub = null, goalSaved = false) }
        repository.checkpoint(after)
    }

    private fun finish() {
        val trip = stateData.trip ?: return
        if (trip.phase != MarketPhase.CHECKOUT || stateData.busy || stateData.error != null) return
        updateState { copy(busy = true, paymentMissingRub = null) }
        launchCoroutine(failure(MarketError.SAVE)) {
            when (val result = checkoutTrip(trip)) {
                is CheckoutResult.Finished -> updateState { copy(trip = result.trip, busy = false) }
                is CheckoutResult.InsufficientFunds -> updateState { copy(busy = false, paymentMissingRub = result.missingRub) }
                CheckoutResult.Rejected -> updateState { copy(busy = false, paymentMissingRub = 0) }
            }
        }
    }

    private fun saveCartAsGoal() {
        val trip = stateData.trip ?: return
        if (trip.phase != MarketPhase.CHECKOUT || stateData.busy || trip.cart.isEmpty()) return
        val totalRub = catalog.quote(trip.cart.map { (id, quantity) -> github.detrig.products.ProductQuantity(id, quantity) }).totalRub
        updateState { copy(busy = true, goalSaved = false) }
        launchCoroutine(failure(MarketError.SAVE)) {
            when (host.saveCartAsGoal(trip.id, totalRub)) {
                MarketSavingsGoalResult.GoalSaved -> updateState { copy(busy = false, goalSaved = true) }
                MarketSavingsGoalResult.Rejected -> updateState { copy(busy = false, goalSaved = false) }
            }
        }
    }

    private fun back() {
        if (stateData.busy || stateData.error != null) return
        if (stateData.cartOpen) {
            updateState { copy(cartOpen = false) }
            return
        }
        updateState { copy(exitConfirmationOpen = !exitConfirmationOpen) }
    }

    private fun confirmExit() {
        if (!stateData.exitConfirmationOpen || stateData.busy || stateData.error != null) return
        val trip = stateData.trip
        updateState { copy(busy = true) }
        launchCoroutine(failure(MarketError.SAVE)) {
            if (trip != null) repository.save(trip)
            updateState { copy(busy = false, exitConfirmationOpen = false, foreground = false) }
            router.back()
        }
    }

    override fun onCleared() {
        if (!stateData.busy) stateData.trip?.let(repository::checkpoint)
        super.onCleared()
    }
}
