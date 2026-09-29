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
 * Separate records preserve table order and each portion’s placement game day.
 */
data class StagedFoodItem(
    val id: String,
    val productId: ProductId,
    val stagedAtAbsoluteDay: Long? = null,
) {
    init {
        require(id.isNotBlank()) { "Staged food id must not be blank" }
        require(stagedAtAbsoluteDay == null || stagedAtAbsoluteDay >= 1) { "Placement day must be positive" }
    }
}

object TableFoodExpiry {
    const val MAX_AGE_GAME_DAYS = 3L

    fun isExpired(item: StagedFoodItem, currentAbsoluteDay: Long): Boolean {
        val stagedDay = item.stagedAtAbsoluteDay ?: return false
        return currentAbsoluteDay >= stagedDay && currentAbsoluteDay - stagedDay >= MAX_AGE_GAME_DAYS
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
