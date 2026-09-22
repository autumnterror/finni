package github.detrig.feature.inventory.data

import github.detrig.core.infrastructure.preferences.SharedStorage
import github.detrig.feature.inventory.api.DeliveryResult
import github.detrig.feature.inventory.domain.InventoryRepository
import github.detrig.feature.inventory.domain.InventoryStageResult
import github.detrig.feature.inventory.domain.StockItem
import github.detrig.feature.inventory.domain.StagedFoodItem
import github.detrig.feature.inventory.domain.TableFoodConsumptionResult
import github.detrig.products.ProductId
import github.detrig.products.ProductQuantity
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class InventoryRepositoryImpl(
    private val storage: SharedStorage,
) : InventoryRepository {
    private val mutex = Mutex()
    private val stock = MutableStateFlow(readStock())
    private val table = MutableStateFlow(readTable())
    private val deliveredOperations = ConcurrentHashMap(readDeliveredOperations())

    override fun observeStock(): Flow<List<StockItem>> = stock.asStateFlow()

    override fun observeTable(): Flow<List<StagedFoodItem>> = table.asStateFlow()

    override suspend fun deliver(
        operationId: String,
        items: List<ProductQuantity>,
    ): DeliveryResult = mutex.withLock {
        require(operationId.isNotBlank()) { "Inventory operation id must not be blank" }
        val normalized = items
            .groupingBy { it.productId.value }
            .fold(0) { total, item -> Math.addExact(total, item.quantity) }
            .toList()
            .sortedBy { it.first }
        if (normalized.isEmpty()) return@withLock DeliveryResult.Delivered(operationId)

        val fingerprint = normalized.joinToString(separator = ";") { "${it.first}=${it.second}" }
        val previous = deliveredOperations[operationId]
        if (previous != null) {
            return@withLock if (previous == fingerprint) {
                DeliveryResult.AlreadyDelivered(operationId)
            } else {
                DeliveryResult.OperationIdConflict(operationId)
            }
        }

        val updated = LinkedHashMap(stock.value.associate { it.productId.value to it.quantity })
        normalized.forEach { (productId, quantity) ->
            updated[productId] = Math.addExact(updated[productId] ?: 0, quantity)
        }
        val nextStock = updated.entries
            .filter { it.value > 0 }
            .map { StockItem(ProductId(it.key), it.value) }
        deliveredOperations[operationId] = fingerprint
        persist(nextStock, table.value, deliveredOperations)
        stock.value = nextStock
        DeliveryResult.Delivered(operationId)
    }

    override suspend fun stageForTable(productId: ProductId): InventoryStageResult = mutex.withLock {
        val current = stock.value.firstOrNull { it.productId == productId }
            ?: return@withLock InventoryStageResult.InsufficientStock
        val nextStock = stock.value.mapNotNull { item ->
            if (item.productId != productId) item
            // StockItem does not permit a zero quantity. Remove the last portion
            // directly instead of constructing an invalid intermediate item.
            else if (item.quantity == 1) null else item.copy(quantity = item.quantity - 1)
        }
        val nextTable = table.value + StagedFoodItem(
            id = UUID.randomUUID().toString(),
            productId = productId,
        )
        persist(nextStock, nextTable, deliveredOperations)
        stock.value = nextStock
        table.value = nextTable
        InventoryStageResult.Staged
    }

    override suspend fun consumeTableItem(itemId: String): TableFoodConsumptionResult = mutex.withLock {
        require(itemId.isNotBlank()) { "Table item id must not be blank" }
        val item = table.value.firstOrNull { it.id == itemId }
            ?: return@withLock TableFoodConsumptionResult.NotFound
        val nextTable = table.value.filterNot { it.id == itemId }
        persist(stock.value, nextTable, deliveredOperations)
        table.value = nextTable
        TableFoodConsumptionResult.Consumed(item)
    }

    private fun readStock(): List<StockItem> = storage.readStringList(STOCK_KEY).mapNotNull { encoded ->
        val (id, quantity) = encoded.split('=', limit = 2).takeIf { it.size == 2 } ?: return@mapNotNull null
        quantity.toIntOrNull()?.takeIf { it > 0 }?.let { StockItem(ProductId(id), it) }
    }

    private fun readTable(): List<StagedFoodItem> = storage.readStringList(TABLE_KEY)
        .mapIndexedNotNull { index, encoded ->
            if (encoded.isBlank()) return@mapIndexedNotNull null
            val separator = encoded.indexOf(TABLE_ITEM_SEPARATOR)
            if (separator <= 0 || separator == encoded.lastIndex) {
                // Keep locally staged food from previous development builds.
                return@mapIndexedNotNull StagedFoodItem(
                    id = "legacy-$index-$encoded",
                    productId = ProductId(encoded),
                )
            }
            StagedFoodItem(
                id = encoded.substring(0, separator),
                productId = ProductId(encoded.substring(separator + 1)),
            )
        }

    private fun readDeliveredOperations(): Map<String, String> = storage.readStringList(DELIVERED_KEY)
        .mapNotNull { encoded ->
            val separator = encoded.indexOf('|')
            if (separator <= 0) null else encoded.substring(0, separator) to encoded.substring(separator + 1)
        }
        .toMap()

    private fun persist(
        nextStock: List<StockItem>,
        nextTable: List<StagedFoodItem>,
        operations: Map<String, String>,
    ) {
        storage.saveStringList(STOCK_KEY, nextStock.map { "${it.productId.value}=${it.quantity}" })
        storage.saveStringList(
            TABLE_KEY,
            nextTable.map { "${it.id}$TABLE_ITEM_SEPARATOR${it.productId.value}" },
        )
        storage.saveStringList(DELIVERED_KEY, operations.entries.map { "${it.key}|${it.value}" })
    }

    private companion object {
        const val STOCK_KEY = "inventory_food_stock"
        const val TABLE_KEY = "inventory_food_table"
        const val DELIVERED_KEY = "inventory_food_delivered_operations"
        const val TABLE_ITEM_SEPARATOR = '|'
    }
}
