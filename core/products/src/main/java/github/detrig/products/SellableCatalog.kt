package github.detrig.products

data class StoreCartLine(
    val itemId: ProductId,
    val quantity: Int,
) {
    init {
        require(quantity > 0) { "Cart quantity must be positive" }
    }
}

/**
 * Store-agnostic cart shared by the catalog screen and the future cart screen.
 *
 * Adding an item only changes the cart. Charging money is deliberately a separate checkout
 * operation so a recomposition or a repeated product tap can never debit the wallet.
 */
data class StoreCart(
    private val quantities: Map<ProductId, Int> = emptyMap(),
) {
    init {
        require(quantities.values.all { it > 0 }) { "Cart quantities must be positive" }
    }

    val lines: List<StoreCartLine>
        get() = quantities.map { (itemId, quantity) -> StoreCartLine(itemId, quantity) }

    val totalQuantity: Int
        get() = quantities.values.fold(0, Math::addExact)

    val isEmpty: Boolean
        get() = quantities.isEmpty()

    fun quantityOf(itemId: ProductId): Int = quantities[itemId] ?: 0

    fun add(itemId: ProductId, quantity: Int = 1): StoreCart {
        require(quantity > 0) { "Added quantity must be positive" }
        val updated = LinkedHashMap(quantities)
        updated[itemId] = Math.addExact(quantityOf(itemId), quantity)
        return StoreCart(updated)
    }

    fun removeOne(itemId: ProductId): StoreCart {
        val current = quantityOf(itemId)
        if (current == 0) return this
        val updated = LinkedHashMap(quantities)
        if (current == 1) updated.remove(itemId) else updated[itemId] = current - 1
        return StoreCart(updated)
    }

    fun clear(): StoreCart = Empty

    companion object {
        val Empty = StoreCart()
    }
}

/** A current-price calculation only: it neither charges money nor mutates inventory. */
data class StoreQuote<out T : SellableItem>(val lines: List<StoreQuoteLine<T>>) {
    val totalRub: Long = lines.fold(0L) { total, line ->
        Math.addExact(total, line.totalRub)
    }
}

data class StoreQuoteLine<out T : SellableItem>(
    val item: T,
    val quantity: Int,
) {
    init {
        require(quantity > 0) { "Quote quantity must be positive" }
    }

    val totalRub: Long = Math.multiplyExact(item.priceRub, quantity.toLong())
}

interface SellableCatalog<out T : SellableItem> {
    val storefront: StorefrontDefinition<T>

    fun find(id: ProductId): T? = storefront.items.firstOrNull { it.id == id }

    fun quote(lines: List<StoreCartLine>): StoreQuote<T> {
        val normalizedQuantities = linkedMapOf<ProductId, Int>()
        lines.forEach { line ->
            normalizedQuantities[line.itemId] = Math.addExact(
                normalizedQuantities[line.itemId] ?: 0,
                line.quantity,
            )
        }
        return StoreQuote(
            normalizedQuantities.map { (itemId, quantity) ->
                val item = requireNotNull(find(itemId)) {
                    "Unknown item: ${itemId.value}"
                }
                StoreQuoteLine(item = item, quantity = quantity)
            },
        )
    }
}
