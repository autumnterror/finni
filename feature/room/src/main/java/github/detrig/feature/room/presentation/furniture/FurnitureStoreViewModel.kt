package github.detrig.feature.room.presentation.furniture

import github.detrig.core.mvvm.CoreViewEvent
import github.detrig.core.mvvm.CoreViewModel
import github.detrig.core.mvvm.CoreViewState
import github.detrig.core.mvvm.ExceptionConsumer
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.room.domain.furniture.FurnitureCatalog
import github.detrig.feature.room.domain.furniture.FurnitureOwnership
import github.detrig.feature.room.domain.furniture.FurniturePurchaseResult
import github.detrig.feature.room.domain.furniture.FurnitureStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine

internal enum class FurnitureMode { SHOP, OWNED }

internal data class FurniturePurchaseReceipt(val itemName: String, val priceRub: Long, val balanceRub: Long)

internal data class FurnitureStoreViewState(
    val roomId: String = "bedroom",
    val slotId: String = "room_lamp",
    val mode: FurnitureMode = FurnitureMode.SHOP,
    val selectedVariantId: String? = null,
    val originalSelected: Boolean = false,
    val ownership: FurnitureOwnership = FurnitureOwnership(),
    val balanceRub: Long = 0L,
    val loading: Boolean = true,
    val busy: Boolean = false,
    val confirmationVisible: Boolean = false,
    val receipt: FurniturePurchaseReceipt? = null,
    val message: String? = null,
) : CoreViewState

internal sealed interface FurnitureStoreViewEvent : CoreViewEvent {
    data object Load : FurnitureStoreViewEvent
    data class RoomSelected(val id: String) : FurnitureStoreViewEvent
    data class SlotSelected(val id: String) : FurnitureStoreViewEvent
    data class ModeSelected(val mode: FurnitureMode) : FurnitureStoreViewEvent
    data class VariantSelected(val id: String?) : FurnitureStoreViewEvent
    data object OriginalSelected : FurnitureStoreViewEvent
    data object ActionPressed : FurnitureStoreViewEvent
    data object PurchaseConfirmed : FurnitureStoreViewEvent
    data object PurchaseDismissed : FurnitureStoreViewEvent
    data object ReceiptDismissed : FurnitureStoreViewEvent
    data object MessageDismissed : FurnitureStoreViewEvent
}

internal class FurnitureStoreViewModel(
    private val catalog: FurnitureCatalog,
    private val store: FurnitureStore,
    private val economy: EconomyApi,
) : CoreViewModel<FurnitureStoreViewState, FurnitureStoreViewEvent>(
    FurnitureStoreViewState(ownership = store.state.value),
) {
    private var observationJob: Job? = null

    override fun perform(viewEvent: FurnitureStoreViewEvent) {
        when (viewEvent) {
            FurnitureStoreViewEvent.Load -> load()
            is FurnitureStoreViewEvent.RoomSelected -> selectRoom(viewEvent.id)
            is FurnitureStoreViewEvent.SlotSelected -> selectSlot(viewEvent.id)
            is FurnitureStoreViewEvent.ModeSelected -> updateState {
                copy(mode = viewEvent.mode, selectedVariantId = null, originalSelected = false)
            }
            is FurnitureStoreViewEvent.VariantSelected -> {
                if (viewEvent.id == null || catalog.byId[viewEvent.id]?.slotId == stateData.slotId) {
                    updateState { copy(selectedVariantId = viewEvent.id, originalSelected = false) }
                }
            }
            FurnitureStoreViewEvent.OriginalSelected -> updateState {
                copy(selectedVariantId = null, originalSelected = true)
            }
            FurnitureStoreViewEvent.ActionPressed -> action()
            FurnitureStoreViewEvent.PurchaseConfirmed -> purchase()
            FurnitureStoreViewEvent.PurchaseDismissed -> updateState { copy(confirmationVisible = false) }
            FurnitureStoreViewEvent.ReceiptDismissed -> updateState { copy(receipt = null) }
            FurnitureStoreViewEvent.MessageDismissed -> updateState { copy(message = null) }
        }
    }

    private fun load() {
        if (observationJob?.isActive == true) return
        observationJob = launchCoroutine(
            handleAction = ExceptionConsumer { exception ->
                updateState { copy(loading = false, message = exception.message ?: "Не удалось открыть интерьер") }
                true
            },
        ) {
            economy.initialize()
            store.reconcilePending()
            combine(economy.observeState(), store.state) { money, ownership -> money.availableRub to ownership }
                .collect { (balance, ownership) ->
                    updateState { copy(balanceRub = balance, ownership = ownership, loading = false) }
                }
        }
    }

    private fun selectRoom(id: String) {
        val firstSlot = catalog.slots.firstOrNull { it.roomId == id } ?: return
        updateState {
            copy(roomId = id, slotId = firstSlot.id, selectedVariantId = null, originalSelected = false)
        }
    }

    private fun selectSlot(id: String) {
        val slot = catalog.slots.firstOrNull { it.id == id && it.roomId == stateData.roomId } ?: return
        updateState { copy(slotId = slot.id, selectedVariantId = null, originalSelected = false) }
    }

    private fun action() {
        val id = stateData.selectedVariantId
        if (stateData.busy) return
        if (id == null && stateData.originalSelected) {
            val slotId = stateData.slotId
            launchCoroutine { store.equip(slotId, null) }
            return
        }
        if (id == null) return
        if (stateData.ownership.owns(id)) {
            launchCoroutine(
                handleAction = ExceptionConsumer { exception ->
                    updateState { copy(message = exception.message ?: "Не удалось установить мебель") }
                    true
                },
            ) { store.equip(stateData.slotId, id) }
        } else {
            updateState { copy(confirmationVisible = true) }
        }
    }

    private fun purchase() {
        val id = stateData.selectedVariantId ?: return
        if (stateData.busy) return
        updateState { copy(confirmationVisible = false, busy = true, message = null) }
        launchCoroutine(
            handleAction = ExceptionConsumer { exception ->
                updateState { copy(busy = false, message = exception.message ?: "Не удалось купить мебель") }
                true
            },
        ) {
            val result = store.purchase(id)
            updateState {
                copy(busy = false, receipt = (result as? FurniturePurchaseResult.Purchased)?.let {
                    FurniturePurchaseReceipt(catalog.byId.getValue(id).name, catalog.byId.getValue(id).priceRub, it.balanceRub)
                }, message = when (result) {
                    is FurniturePurchaseResult.Purchased -> null
                    FurniturePurchaseResult.AlreadyOwned -> "Этот вариант уже куплен"
                    FurniturePurchaseResult.InsufficientFunds -> "Не хватает монет для покупки"
                    FurniturePurchaseResult.Failed -> "Покупка не выполнена"
                })
            }
        }
    }
}
