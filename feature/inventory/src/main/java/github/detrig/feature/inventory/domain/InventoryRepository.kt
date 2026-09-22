package github.detrig.feature.inventory.domain

import github.detrig.feature.inventory.api.DeliveryResult
import github.detrig.products.ProductId
import github.detrig.products.ProductQuantity
import kotlinx.coroutines.flow.Flow

interface InventoryRepository {
    fun observeStock(): Flow<List<StockItem>>

    fun observeTable(): Flow<List<StagedFoodItem>>

    suspend fun deliver(operationId: String, items: List<ProductQuantity>): DeliveryResult

    suspend fun stageForTable(productId: ProductId): InventoryStageResult

    suspend fun consumeTableItem(itemId: String): TableFoodConsumptionResult
}
