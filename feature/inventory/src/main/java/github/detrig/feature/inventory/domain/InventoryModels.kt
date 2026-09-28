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
 * Separate records preserve table order and each portion’s real placement time.
 */
data class StagedFoodItem(
    val id: String,
    val productId: ProductId,
    val stagedAtMillis: Long = 0L,
) {
    init {
        require(id.isNotBlank()) { "Staged food id must not be blank" }
    }
}

object TableFoodExpiry {
    const val MAX_AGE_MILLIS = 3L * 24 * 60 * 60 * 1_000

    fun isExpired(item: StagedFoodItem, nowMillis: Long): Boolean =
        nowMillis > item.stagedAtMillis && nowMillis - item.stagedAtMillis > MAX_AGE_MILLIS
}

sealed interface InventoryStageResult {
    data object Staged : InventoryStageResult
    data object InsufficientStock : InventoryStageResult
}

sealed interface TableFoodConsumptionResult {
    data class Consumed(val item: StagedFoodItem) : TableFoodConsumptionResult
    data object NotFound : TableFoodConsumptionResult
}
