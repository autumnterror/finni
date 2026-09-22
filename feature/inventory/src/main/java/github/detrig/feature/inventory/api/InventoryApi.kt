package github.detrig.feature.inventory.api

import github.detrig.feature.inventory.domain.InventoryStageResult
import github.detrig.feature.inventory.domain.StockItem
import github.detrig.feature.inventory.domain.StagedFoodItem
import github.detrig.feature.inventory.domain.TableFoodConsumptionResult
import github.detrig.products.ProductId
import github.detrig.products.ProductQuantity
import kotlinx.coroutines.flow.Flow

interface InventoryApi {
    fun observeStock(): Flow<List<StockItem>>

    fun observeTable(): Flow<List<StagedFoodItem>>

    suspend fun deliver(operationId: String, items: List<ProductQuantity>): DeliveryResult

    suspend fun stageForTable(productId: ProductId): InventoryStageResult

    /** Removes exactly one staged food portion after its idempotent pet effect is committed. */
    suspend fun consumeTableItem(itemId: String): TableFoodConsumptionResult
}

sealed interface DeliveryResult {
    val operationId: String

    data class Delivered(override val operationId: String) : DeliveryResult
    data class AlreadyDelivered(override val operationId: String) : DeliveryResult
    data class OperationIdConflict(override val operationId: String) : DeliveryResult
}
