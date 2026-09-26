package github.detrig.feature.wardrobe.presentation

import github.detrig.core.mvvm.CoreViewEvent

internal sealed interface WardrobeViewEvent : CoreViewEvent {
    data object Load : WardrobeViewEvent
    data object Back : WardrobeViewEvent
    data class TabSelected(val tab: WardrobeTab) : WardrobeViewEvent
    data class CategorySelected(val slot: String?) : WardrobeViewEvent
    data class ItemSelected(val itemId: String) : WardrobeViewEvent
    data object ClearTrial : WardrobeViewEvent
    data object PrimaryAction : WardrobeViewEvent
    data object ConfirmPurchase : WardrobeViewEvent
    data object CancelPurchase : WardrobeViewEvent
    data object DismissMessage : WardrobeViewEvent
}
