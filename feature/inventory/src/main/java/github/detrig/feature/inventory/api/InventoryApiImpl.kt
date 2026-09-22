package github.detrig.feature.inventory.api

import github.detrig.feature.inventory.domain.InventoryRepository
import github.detrig.feature.inventory.domain.InventoryStageResult
import github.detrig.feature.inventory.domain.StockItem
import github.detrig.feature.inventory.domain.StagedFoodItem
import github.detrig.feature.inventory.domain.TableFoodConsumptionResult
import github.detrig.products.ProductId
import github.detrig.products.ProductQuantity
import kotlinx.coroutines.flow.Flow

internal class InventoryApiImpl(
    private val repository: InventoryRepository,
) : InventoryApi {
    override fun observeStock(): Flow<List<StockItem>> = repository.observeStock()

    override fun observeTable(): Flow<List<StagedFoodItem>> = repository.observeTable()

    override suspend fun deliver(operationId: String, items: List<ProductQuantity>): DeliveryResult =
        repository.deliver(operationId, items)

    override suspend fun stageForTable(productId: ProductId): InventoryStageResult =
        repository.stageForTable(productId)

    override suspend fun consumeTableItem(itemId: String): TableFoodConsumptionResult =
        repository.consumeTableItem(itemId)
}
