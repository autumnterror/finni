package github.detrig.feature.fridge.presentation

import github.detrig.core.mvvm.CoreViewModel
import github.detrig.core.mvvm.ExceptionConsumer
import github.detrig.feature.fridge.navigation.FridgeRouter
import github.detrig.feature.gamestate.api.GameStateApi
import github.detrig.feature.gamestate.domain.model.PetFeedingCompletion
import github.detrig.feature.inventory.api.InventoryApi
import github.detrig.feature.inventory.domain.StagedFoodItem
import github.detrig.products.GroceryCatalog
import github.detrig.products.ProductId
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

private const val MOUTH_OPEN_MILLIS = 100L
private const val CHEW_A_MILLIS = 120L
private const val CHEW_B_MILLIS = 160L

internal class FeedingViewModel(
    private val inventoryApi: InventoryApi,
    private val gameStateApi: GameStateApi,
    private val router: FridgeRouter,
) : CoreViewModel<FeedingViewState, FeedingViewEvent>(FeedingViewState()) {
    private var tableObservation: Job? = null
    private var petObservation: Job? = null
    private var consumptionJob: Job? = null
    private var tableReady = false
    private var petReady = false

    override fun perform(viewEvent: FeedingViewEvent) {
        when (viewEvent) {
            FeedingViewEvent.Load -> load()
            FeedingViewEvent.Back -> close()
            FeedingViewEvent.PreviousPage -> changePage(-1)
            FeedingViewEvent.NextPage -> changePage(1)
            is FeedingViewEvent.FoodDroppedIntoMouth -> startConsumption(viewEvent.productId)
        }
    }

    private fun load() {
        if (tableObservation?.isActive != true) {
            tableObservation = launchCoroutine(
                handleAction = ExceptionConsumer {
                    updateState { copy(loading = false, message = "Не удалось загрузить еду") }
                    true
                },
            ) {
                inventoryApi.observeTable().collect { tableItems ->
                    tableReady = true
                    updateTable(tableItems)
                }
            }
        }
        if (petObservation?.isActive != true) {
            petObservation = launchCoroutine(
                handleAction = ExceptionConsumer {
                    updateState { copy(loading = false, message = "Не удалось загрузить питомца") }
                    true
                },
            ) {
                val initial = gameStateApi.initialize()
                petReady = true
                updateState {
                    copy(hunger = initial.pet.hunger, loading = !(tableReady && petReady))
                }
                gameStateApi.observeState().collect { gameState ->
                    if (gameState != null) {
                        petReady = true
                        updateState {
                            copy(hunger = gameState.pet.hunger, loading = !(tableReady && petReady))
                        }
                    }
                }
            }
        }
    }

    private fun updateTable(items: List<StagedFoodItem>) {
        val stacks = items.toFoodStacks()
        updateState {
            val maxPage = stacks.lastFeedingPageIndex()
            copy(
                foodStacks = stacks,
                page = page.coerceIn(0, maxPage),
                loading = !(tableReady && petReady),
            )
        }
    }

    private fun changePage(delta: Int) {
        if (stateData.activePortion != null) return
        updateState {
            copy(page = (page + delta).coerceIn(0, foodStacks.lastFeedingPageIndex()))
        }
    }

    private fun startConsumption(productId: ProductId) {
        if (consumptionJob?.isActive == true || stateData.activePortion != null || stateData.loading) return
        val stack = stateData.foodStacks.firstOrNull { it.productId == productId } ?: return
        val portion = FeedingFoodPortion(id = stack.portionIds.first(), productId = productId)
        val food = GroceryCatalog().find(productId) ?: return
        updateState {
            copy(
                activePortion = portion,
                animation = FeedingAnimation.MouthOpen,
                message = null,
            )
        }
        consumptionJob = launchCoroutine(
            handleAction = ExceptionConsumer {
                updateState {
                    copy(
                        activePortion = null,
                        animation = FeedingAnimation.Idle,
                        message = "Не удалось покормить питомца",
                    )
                }
                true
            },
        ) {
            delay(MOUTH_OPEN_MILLIS)
            updateState { copy(animation = FeedingAnimation.ChewA) }
            delay(CHEW_A_MILLIS)
            updateState { copy(animation = FeedingAnimation.ChewB) }
            delay(CHEW_B_MILLIS)
            updateState { copy(animation = FeedingAnimation.ChewA) }
            delay(CHEW_A_MILLIS)
            updateState { copy(animation = FeedingAnimation.ChewB) }
            delay(CHEW_B_MILLIS)

            // The table portion id is the durable operation id. A restored callback
            // therefore returns the already applied pet state instead of feeding twice.
            val result = gameStateApi.feedPet(
                PetFeedingCompletion(
                    operationId = portion.id,
                    satietyPercent = food.effects.satietyPercent,
                    happinessPoints = food.effects.happinessPoints,
                ),
            )
            inventoryApi.consumeTableItem(portion.id)
            updateState {
                copy(
                    hunger = result.hunger,
                    activePortion = null,
                    animation = FeedingAnimation.Idle,
                )
            }
        }
    }

    private fun close() {
        consumptionJob?.cancel()
        tableObservation?.cancel()
        petObservation?.cancel()
        router.back()
    }
}

private fun List<StagedFoodItem>.toFoodStacks(): List<FeedingFoodStack> {
    val portionIdsByProduct = linkedMapOf<ProductId, MutableList<String>>()
    forEach { item -> portionIdsByProduct.getOrPut(item.productId, ::mutableListOf).add(item.id) }
    return portionIdsByProduct.map { (productId, portionIds) ->
        FeedingFoodStack(productId = productId, portionIds = portionIds)
    }
}
