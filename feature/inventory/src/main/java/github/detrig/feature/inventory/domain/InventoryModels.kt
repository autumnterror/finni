package github.detrig.feature.inventory.domain

import github.detrig.products.ProductId

data class StockItem(
    val productId: ProductId,
    val quantity: Int,
) {
    init {
        require(quantity > 0) { "Stock quantity must be positive" }
    }
}

/**
 * One concrete portion moved from the fridge to the table.
 *
 * Keeping portions as separate records preserves their order and leaves room for
 * expiry metadata without changing the stock model later.
 */
data class StagedFoodItem(
    val id: String,
    val productId: ProductId,
) {
    init {
        require(id.isNotBlank()) { "Staged food id must not be blank" }
    }
}

sealed interface InventoryStageResult {
    data object Staged : InventoryStageResult
    data object InsufficientStock : InventoryStageResult
}

sealed interface TableFoodConsumptionResult {
    data class Consumed(val item: StagedFoodItem) : TableFoodConsumptionResult
    data object NotFound : TableFoodConsumptionResult
}
