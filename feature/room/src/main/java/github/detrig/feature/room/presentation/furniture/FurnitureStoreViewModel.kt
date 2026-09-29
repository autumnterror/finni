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
import github.detrig.feature.room.domain.surface.SurfaceCatalog
import github.detrig.feature.room.domain.surface.SurfaceKind
import github.detrig.feature.room.domain.surface.surfaceSlotId
import github.detrig.feature.room.presentation.model.HouseSurfaceLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine

internal enum class FurnitureMode { SHOP, OWNED }

internal data class FurnitureStoreViewState(
    val roomId: String = "bedroom",
    val slotId: String = "room_lamp",
    val surfaceKind: SurfaceKind? = null,
    val mode: FurnitureMode = FurnitureMode.SHOP,
    val selectedVariantId: String? = null,
    val selectedSurfaceId: String? = null,
    val originalSelected: Boolean = false,
    val surfaceOriginalSelected: Boolean = false,
    val ownership: FurnitureOwnership = FurnitureOwnership(),
    val balanceRub: Long = 0L,
    val loading: Boolean = true,
    val busy: Boolean = false,
    val message: String? = null,
) : CoreViewState

internal sealed interface FurnitureStoreViewEvent : CoreViewEvent {
    data object Load : FurnitureStoreViewEvent
    data class RoomSelected(val id: String) : FurnitureStoreViewEvent
    data class SlotSelected(val id: String) : FurnitureStoreViewEvent
    data class SurfaceSelected(val kind: SurfaceKind) : FurnitureStoreViewEvent
    data class ModeSelected(val mode: FurnitureMode) : FurnitureStoreViewEvent
    data class VariantSelected(val id: String?) : FurnitureStoreViewEvent
    data class SurfaceVariantSelected(val id: String) : FurnitureStoreViewEvent
    data object OriginalSelected : FurnitureStoreViewEvent
    data object ActionPressed : FurnitureStoreViewEvent
    data object MessageDismissed : FurnitureStoreViewEvent
}

internal class FurnitureStoreViewModel(
    private val catalog: FurnitureCatalog,
    private val surfaceCatalog: SurfaceCatalog,
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
            is FurnitureStoreViewEvent.SurfaceSelected -> updateState {
                copy(surfaceKind = viewEvent.kind, selectedSurfaceId = null, surfaceOriginalSelected = false,
                    selectedVariantId = null, originalSelected = false)
            }
            is FurnitureStoreViewEvent.ModeSelected -> updateState {
                copy(mode = viewEvent.mode, selectedVariantId = null, originalSelected = false,
                    selectedSurfaceId = null, surfaceOriginalSelected = false)
            }
            is FurnitureStoreViewEvent.VariantSelected -> {
                if (viewEvent.id == null || catalog.byId[viewEvent.id]?.slotId == stateData.slotId) {
                    updateState { copy(selectedVariantId = viewEvent.id, originalSelected = false) }
                }
            }
            is FurnitureStoreViewEvent.SurfaceVariantSelected -> {
                val room = HouseSurfaceLayout.Room.fromId(stateData.roomId)
                val kind = stateData.surfaceKind
                if (room != null && kind != null && surfaceCatalog.byId[viewEvent.id]?.slotId == surfaceSlotId(room.id, kind)) {
                    updateState { copy(selectedSurfaceId = viewEvent.id, surfaceOriginalSelected = false) }
                }
            }
            FurnitureStoreViewEvent.OriginalSelected -> updateState {
                if (surfaceKind == null) copy(selectedVariantId = null, originalSelected = true)
                else copy(selectedSurfaceId = null, surfaceOriginalSelected = true)
            }
            FurnitureStoreViewEvent.ActionPressed -> action()
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
        if (id != "bathroom" && HouseSurfaceLayout.Room.fromId(id) == null) return
        val firstSlot = catalog.slots.firstOrNull { it.roomId == id }
        updateState {
            copy(roomId = id, slotId = firstSlot?.id ?: "", surfaceKind = if (id == HouseSurfaceLayout.Room.PLAYROOM.id) SurfaceKind.WALL else null,
                selectedVariantId = null, originalSelected = false, selectedSurfaceId = null, surfaceOriginalSelected = false)
        }
    }

    private fun selectSlot(id: String) {
        val slot = catalog.slots.firstOrNull { it.id == id && it.roomId == stateData.roomId } ?: return
        updateState { copy(slotId = slot.id, surfaceKind = null, selectedVariantId = null,
            originalSelected = false, selectedSurfaceId = null, surfaceOriginalSelected = false) }
    }

    private fun action() {
        if (stateData.busy || stateData.loading) return
        val kind = stateData.surfaceKind
        if (kind != null) {
            val room = HouseSurfaceLayout.Room.fromId(stateData.roomId) ?: return
            val id = stateData.selectedSurfaceId
            if (stateData.surfaceOriginalSelected) {
                launchCoroutine { store.equipSurface(room.id, kind, null) }
            } else if (id != null) {
                if (stateData.ownership.ownsSurface(id)) {
                    launchCoroutine { store.equipSurface(room.id, kind, id) }
                } else purchase()
            }
            return
        }
        val id = stateData.selectedVariantId
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
            purchase()
        }
    }

    private fun purchase() {
        val surface = stateData.surfaceKind != null
        val id = if (surface) stateData.selectedSurfaceId else stateData.selectedVariantId ?: return
        if (id == null) return
        val name = if (surface) surfaceCatalog.byId[id]?.name else catalog.byId[id]?.name
        val price = if (surface) surfaceCatalog.byId[id]?.priceRub else catalog.byId[id]?.priceRub
        if (name == null || price == null) return
        if (stateData.busy) return
        updateState { copy(busy = true, message = null) }
        launchCoroutine(
            handleAction = ExceptionConsumer { exception ->
                updateState { copy(busy = false, message = exception.message ?: "Не удалось выполнить покупку") }
                true
            },
        ) {
            val result = if (surface) store.purchaseSurface(id) else store.purchase(id)
            updateState {
                copy(busy = false, message = when (result) {
                    is FurniturePurchaseResult.Purchased -> null
                    FurniturePurchaseResult.AlreadyOwned -> "Этот вариант уже куплен"
                    FurniturePurchaseResult.InsufficientFunds -> "Не хватает монет для покупки"
                    FurniturePurchaseResult.Failed -> "Покупка не выполнена"
                })
            }
        }
    }
}
