package github.detrig.products

interface ProductCatalog {
    val products: List<Product>
    fun find(id: ProductId): Product? = products.firstOrNull { it.id == id }

    fun quote(items: List<ProductQuantity>): ProductQuote = ProductQuote(items.map { item ->
        ProductQuoteLine(requireNotNull(find(item.productId)) { "Unknown product: ${item.productId.value}" }, item.quantity)
    })
}

/** Каталог не зависит от UI магазина, кошелька или способа хранения запасов. */
class DefaultProductCatalog : ProductCatalog {
    override val products = listOf(
        Product(ProductIds.Groats, ProductKind.GROATS, unitPriceRub = 40),
        Product(ProductIds.Carrot, ProductKind.CARROT, unitPriceRub = 10),
        Product(ProductIds.Apple, ProductKind.APPLE, unitPriceRub = 20),
        Product(ProductIds.Berries, ProductKind.BERRIES, unitPriceRub = 35),
        Product(ProductIds.Crackers, ProductKind.CRACKERS, unitPriceRub = 25),
        Product(ProductIds.Milk, ProductKind.MILK, unitPriceRub = 50),
        Product(ProductIds.Yogurt, ProductKind.YOGURT, unitPriceRub = 30),
        Product(ProductIds.ReadyMeal, ProductKind.READY_MEAL, unitPriceRub = 80),
    )
}
