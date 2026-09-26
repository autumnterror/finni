package github.detrig.feature.wardrobe.presentation

import github.detrig.core.mvvm.CoreViewState
import github.detrig.feature.pet.api.ClothingItem
import github.detrig.feature.pet.domain.model.PetProfile

internal enum class WardrobeTab { SHOP, OWNED }

internal data class WardrobeViewState(
    val loading: Boolean = true,
    val profile: PetProfile? = null,
    val items: List<ClothingItem> = emptyList(),
    val balanceRub: Long = 0,
    val tab: WardrobeTab = WardrobeTab.SHOP,
    val category: String? = null,
    val selectedId: String? = null,
    val confirmingPurchase: Boolean = false,
    val purchasing: Boolean = false,
    val message: String? = null,
) : CoreViewState {
    val selectedItem: ClothingItem? get() = items.firstOrNull { it.id == selectedId }

    val visibleItems: List<ClothingItem>
        get() = items.filter { item ->
            (tab == WardrobeTab.SHOP || item.id in profile?.clothing?.ownedIds.orEmpty()) &&
                (category == null || category == item.slot)
        }

    val previewOutfit: Map<String, String>
        get() = profile?.clothing?.equippedBySlot.orEmpty().let { equipped ->
            selectedItem?.let { equipped + (it.slot to it.id) } ?: equipped
        }

    val isTrial: Boolean
        get() = selectedItem?.let { previewOutfit[it.slot] != profile?.clothing?.equippedBySlot?.get(it.slot) }
            ?: false
}
