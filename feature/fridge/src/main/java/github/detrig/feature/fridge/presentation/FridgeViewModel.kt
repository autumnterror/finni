package github.detrig.feature.fridge.presentation

import github.detrig.core.mvvm.CoreViewModel
import github.detrig.core.mvvm.ExceptionConsumer
import github.detrig.feature.fridge.navigation.FridgeRouter
import github.detrig.feature.inventory.api.InventoryApi
import github.detrig.feature.inventory.domain.InventoryStageResult
import github.detrig.products.ProductId
import kotlinx.coroutines.Job
import github.detrig.feature.room.api.FirstRunGuideApi
import github.detrig.feature.room.api.FirstRunOnboardingStep

internal class FridgeViewModel(
    private val inventoryApi: InventoryApi,
    private val router: FridgeRouter,
    private val firstRunGuide: FirstRunGuideApi,
) : CoreViewModel<FridgeViewState, FridgeViewEvent>(FridgeViewState()) {
    private var observationJob: Job? = null
    private var nextFlightId = 0L
    private var visitId = 0L

    override fun perform(viewEvent: FridgeViewEvent) {
        when (viewEvent) {
            FridgeViewEvent.Load -> load()
            FridgeViewEvent.Back -> close()
            is FridgeViewEvent.ProductClicked -> takeProduct(viewEvent.productId)
            is FridgeViewEvent.FlightAnimationFinished -> updateState {
                copy(flights = flights.filterNot { it.id == viewEvent.flightId })
            }
        }
    }

    private fun load() {
        if (observationJob?.isActive == true) return
        observationJob = launchCoroutine(
            handleAction = ExceptionConsumer {
                updateState { copy(loading = false, message = "Не удалось открыть холодильник") }
                true
            },
        ) {
            inventoryApi.observeStock().collect { stock ->
                val productIds = stock.asSequence()
                    .filter { it.quantity > 0 }
                    .map { it.productId }
                    .toList()
                updateState {
                    val slots = if (!sessionInitialized) {
                        productIds
                    } else {
                        sessionSlots.map { id -> id?.takeIf { it in productIds } }
                    }
                    copy(stock = stock, sessionSlots = slots, sessionInitialized = true, loading = false)
                }
            }
        }
    }

    private fun close() {
        when (firstRunGuide.step.value) {
            FirstRunOnboardingStep.WAITING_FOR_FRIDGE_CLOSE ->
                firstRunGuide.moveTo(FirstRunOnboardingStep.TABLE_GUIDANCE)
            FirstRunOnboardingStep.FRIDGE_FOUND,
            FirstRunOnboardingStep.FRIDGE_EXPLANATION,
            FirstRunOnboardingStep.FRIDGE_PICK_FOOD,
            -> firstRunGuide.moveTo(FirstRunOnboardingStep.WAITING_FOR_FRIDGE)
            else -> Unit
        }
        observationJob?.cancel()
        observationJob = null
        visitId++
        updateState {
            copy(
                sessionSlots = emptyList(),
                flights = emptyList(),
                sessionInitialized = false,
                loading = true,
            )
        }
        router.back()
    }

    private fun takeProduct(productId: ProductId) {
        if (stateData.loading) return
        if (stateData.stock.none { it.productId == productId && it.quantity > 0 }) return
        // Keep the origin even when staging the last portion empties its cell.
        val slotIndex = stateData.sessionSlots.indexOf(productId)
        if (slotIndex < 0) return
        val currentVisitId = visitId
        updateState { copy(message = null) }
        launchCoroutine(
            handleAction = ExceptionConsumer {
                updateState { copy(message = "Не получилось взять продукт") }
                true
            },
        ) {
            when (inventoryApi.stageForTable(productId)) {
                InventoryStageResult.Staged -> {
                    if (currentVisitId == visitId) {
                        val flight = FridgeFoodFlight(nextFlightId++, productId, slotIndex)
                        updateState { copy(flights = flights + flight) }
                    }
                    if (firstRunGuide.step.value == FirstRunOnboardingStep.FRIDGE_PICK_FOOD) {
                        firstRunGuide.moveTo(FirstRunOnboardingStep.WAITING_FOR_FRIDGE_CLOSE)
                    }
                }
                InventoryStageResult.InsufficientStock -> {
                    updateState { copy(message = "Этот продукт уже закончился") }
                }
            }
        }
    }

}
