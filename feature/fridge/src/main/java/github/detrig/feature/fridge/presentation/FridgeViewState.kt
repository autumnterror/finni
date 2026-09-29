package github.detrig.feature.fridge.presentation

import github.detrig.core.mvvm.CoreViewState
import github.detrig.feature.inventory.domain.StockItem
import github.detrig.products.ProductId

internal data class FridgeViewState(
    val stock: List<StockItem> = emptyList(),
    /** Product positions stay fixed for this visit; depleted types leave empty cells. */
    val sessionSlots: List<ProductId?> = emptyList(),
    val flights: List<FridgeFoodFlight> = emptyList(),
    val sessionInitialized: Boolean = false,
    val loading: Boolean = true,
    val message: String? = null,
) : CoreViewState

internal data class FridgeFoodFlight(
    val id: Long,
    val productId: ProductId,
    val slotIndex: Int,
)
