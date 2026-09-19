package github.detrig.products

/** Stable identifier of a storefront. It is safe to persist but has no UI meaning. */
@JvmInline
value class StoreId(val value: String) {
    init {
        require(value.isNotBlank()) { "Store id must not be blank" }
        require(value == value.trim()) { "Store id must not have surrounding whitespace" }
    }
}

/** Stable identifier of a category within a storefront. */
@JvmInline
value class StoreCategoryId(val value: String) {
    init {
        require(value.isNotBlank()) { "Store category id must not be blank" }
        require(value == value.trim()) { "Store category id must not have surrounding whitespace" }
    }
}

/**
 * Logical image identifier resolved by the application layer.
 *
 * Keeping it independent of Android drawables lets a catalog be reused by the shop, cart and
 * inventory and lets the asset manifest change without changing persisted item identities.
 */
@JvmInline
value class ProductImageKey(val value: String) {
    init {
        require(value.isNotBlank()) { "Product image key must not be blank" }
        require(value == value.trim()) { "Product image key must not have surrounding whitespace" }
    }
}

/** Minimum contract shared by food, clothes, furniture and other future shop items. */
interface SellableItem {
    val id: ProductId
    val title: String
    val imageKey: ProductImageKey
    val priceRub: Long
    val categoryId: StoreCategoryId
    val description: String? get() = null
}

data class StoreCategory(
    val id: StoreCategoryId,
    val title: String,
) {
    init {
        require(title.isNotBlank()) { "Store category title must not be blank" }
    }
}

/** Pure configuration consumed by a UI grid; it intentionally does not depend on Compose Dp. */
data class StoreGridLayout(val columns: Int = 3) {
    init {
        require(columns > 0) { "Grid must have at least one column" }
    }
}

/**
 * Complete, data-driven definition of one storefront.
 *
 * [allItemsLabel] configures the synthetic "all" filter. A `null` category passed to [itemsIn]
 * represents that filter, so a fake category never leaks into item or persistence models.
 */
data class StorefrontDefinition<out T : SellableItem>(
    val storeId: StoreId,
    val title: String,
    val allItemsLabel: String,
    val categories: List<StoreCategory>,
    val items: List<T>,
    val gridLayout: StoreGridLayout = StoreGridLayout(),
) {
    init {
        require(title.isNotBlank()) { "Storefront title must not be blank" }
        require(allItemsLabel.isNotBlank()) { "All-items label must not be blank" }
        require(categories.isNotEmpty()) { "Storefront must define at least one category" }
        require(categories.map(StoreCategory::id).distinct().size == categories.size) {
            "Storefront category ids must be unique"
        }
        require(items.map(SellableItem::id).distinct().size == items.size) {
            "Storefront item ids must be unique"
        }
        require(items.all { it.title.isNotBlank() }) {
            "Every item must have a non-blank title"
        }
        require(items.all { it.priceRub > 0L }) {
            "Every item must have a positive price"
        }
        require(items.all { it.description == null || it.description!!.isNotBlank() }) {
            "An item description must be either absent or non-blank"
        }

        val categoryIds = categories.mapTo(mutableSetOf(), StoreCategory::id)
        val unknownCategories = items.map(SellableItem::categoryId).filterNot(categoryIds::contains)
        require(unknownCategories.isEmpty()) {
            "Every item must reference a storefront category: $unknownCategories"
        }
    }

    fun itemsIn(categoryId: StoreCategoryId?): List<T> {
        if (categoryId == null) return items
        require(categories.any { it.id == categoryId }) {
            "Unknown category: ${categoryId.value}"
        }
        return items.filter { it.categoryId == categoryId }
    }
}
