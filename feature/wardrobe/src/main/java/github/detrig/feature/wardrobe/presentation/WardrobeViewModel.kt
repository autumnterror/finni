package github.detrig.feature.wardrobe.presentation

import github.detrig.core.mvvm.CoreViewModel
import github.detrig.core.mvvm.ExceptionConsumer
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.pet.api.PetApi
import github.detrig.feature.wardrobe.domain.ClothingPurchaseInteractor
import github.detrig.feature.wardrobe.domain.ClothingPurchaseResult
import github.detrig.feature.wardrobe.navigation.WardrobeRouter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine

internal class WardrobeViewModel(
    mode: WardrobeMode,
    private val pet: PetApi,
    private val economy: EconomyApi,
    private val purchase: ClothingPurchaseInteractor,
    private val router: WardrobeRouter,
) : CoreViewModel<WardrobeViewState, WardrobeViewEvent>(initialState(pet, mode)) {
    private var observation: Job? = null

    override fun perform(viewEvent: WardrobeViewEvent) {
        when (viewEvent) {
            WardrobeViewEvent.Load -> load()
            WardrobeViewEvent.Back -> router.back()
            is WardrobeViewEvent.CategorySelected -> updateState {
                copy(category = viewEvent.slot, selectedId = null)
            }
            is WardrobeViewEvent.ItemSelected -> updateState {
                copy(selectedId = viewEvent.itemId, message = null)
            }
            WardrobeViewEvent.ClearTrial -> updateState { copy(selectedId = null) }
            WardrobeViewEvent.PrimaryAction -> primaryAction()
            WardrobeViewEvent.DismissMessage -> updateState { copy(message = null) }
        }
    }

    private fun load() {
        if (observation?.isActive == true) return
        observation = launchCoroutine(
            handleAction = ExceptionConsumer {
                updateState { copy(loading = false, message = "Не удалось открыть гардероб") }
                true
            },
        ) {
            economy.initialize()
            val items = pet.clothingItems()
            purchase.reconcile()
            updateState { copy(items = items) }
            combine(pet.observeProfile(), economy.observeState()) { profile, balance ->
                profile to balance.availableRub
            }.collect { (profile, balance) ->
                updateState { copy(profile = profile, balanceRub = balance, loading = false) }
            }
        }
    }

    private fun primaryAction() {
        val item = stateData.selectedItem ?: return
        val profile = stateData.profile ?: return
        if (stateData.purchasing) return
        if (stateData.mode == WardrobeMode.SHOP) {
            if (item.id !in profile.clothing.ownedIds) {
                buySelectedItem()
            }
            return
        }
        when {
            item.id !in profile.clothing.ownedIds -> return
            profile.clothing.equippedBySlot[item.slot] == item.id -> {
                pet.equipClothing(item.slot, null)
                updateState { copy(selectedId = null) }
            }
            else -> {
                pet.equipClothing(item.slot, item.id)
                updateState { copy(selectedId = null) }
            }
        }
    }

    private fun buySelectedItem() {
        val item = stateData.selectedItem ?: return
        if (stateData.mode != WardrobeMode.SHOP || stateData.purchasing || stateData.loading) return
        updateState { copy(purchasing = true, message = null) }
        launchCoroutine(
            handleAction = ExceptionConsumer {
                updateState { copy(purchasing = false, message = "Покупку не удалось завершить") }
                true
            },
        ) {
            val result = purchase.purchase(item)
            when (result) {
                is ClothingPurchaseResult.Purchased -> {
                    pet.equipClothing(item.slot, item.id)
                    val message = if (result.happinessGained > 0) {
                        "Новая вещь надета · +${result.happinessGained} счастья"
                    } else {
                        "Новая вещь надета"
                    }
                    updateState { copy(purchasing = false, selectedId = null, message = message) }
                }
                ClothingPurchaseResult.AlreadyOwned -> {
                    pet.equipClothing(item.slot, item.id)
                    updateState { copy(purchasing = false, selectedId = null, message = "Новая вещь надета") }
                }
                is ClothingPurchaseResult.NotEnoughMoney -> updateState {
                    copy(purchasing = false, message = "Не хватает ${result.missingRub} ₽")
                }
                ClothingPurchaseResult.Failed -> updateState {
                    copy(purchasing = false, message = "Покупку не удалось завершить")
                }
            }
        }
    }
}

private fun initialState(pet: PetApi, mode: WardrobeMode): WardrobeViewState {
    val items = pet.cachedClothingItems()
    return WardrobeViewState(
        loading = items.isEmpty(),
        profile = pet.currentProfile(),
        items = items,
        mode = mode,
    )
}
