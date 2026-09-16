package github.detrig.products

/** Устойчивый идентификатор продукта для магазина, запасов и рецептов. */
@JvmInline
value class ProductId(val value: String) {
    init { require(value.isNotBlank()) }
}

enum class ProductKind { GROATS, CARROT, APPLE, BERRIES, CRACKERS, MILK, YOGURT, READY_MEAL }

data class Product(val id: ProductId, val kind: ProductKind, val unitPriceRub: Int) {
    init { require(unitPriceRub >= 0) }
}

data class ProductQuantity(val productId: ProductId, val quantity: Int) {
    init { require(quantity > 0) }
}

object ProductIds {
    val Groats = ProductId("groats_bag")
    val Carrot = ProductId("carrot")
    val Apple = ProductId("apple")
    val Berries = ProductId("berries")
    val Crackers = ProductId("crackers")
    val Milk = ProductId("milk")
    val Yogurt = ProductId("yogurt")
    val ReadyMeal = ProductId("ready_meal")
}
