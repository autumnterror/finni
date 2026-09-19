package github.detrig.products

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GroceryCatalogTest {

    @Test
    fun sharedCartAccumulatesQuantitiesWithoutChargingAnything() {
        val cart = StoreCart.Empty
            .add(GroceryItemIds.Apple)
            .add(GroceryItemIds.Apple)
            .add(GroceryItemIds.Soup)

        assertEquals(2, cart.quantityOf(GroceryItemIds.Apple))
        assertEquals(1, cart.quantityOf(GroceryItemIds.Soup))
        assertEquals(3, cart.totalQuantity)
        assertEquals(75L, GroceryCatalog().quote(cart.lines).totalRub)

        val decremented = cart.removeOne(GroceryItemIds.Apple)
        assertEquals(1, decremented.quantityOf(GroceryItemIds.Apple))
        assertEquals(0, decremented.removeOne(GroceryItemIds.Apple).quantityOf(GroceryItemIds.Apple))
        assertTrue(decremented.clear().isEmpty)
    }
    private val catalog = GroceryCatalog()

    @Test
    fun catalogContainsThirtyNineUniqueNamespacedItemsInExpectedCategories() {
        val items = catalog.storefront.items

        assertEquals(39, items.size)
        assertEquals(39, items.map { it.id }.distinct().size)
        assertEquals(39, items.map { it.imageKey }.distinct().size)
        assertTrue(items.all { it.id.value.startsWith("store.grocery.item.") })
        assertTrue(items.all { it.imageKey.value.startsWith("store.grocery.image.") })
        assertEquals(20, catalog.storefront.itemsIn(GroceryCategoryIds.Products).size)
        assertEquals(10, catalog.storefront.itemsIn(GroceryCategoryIds.Meals).size)
        assertEquals(9, catalog.storefront.itemsIn(GroceryCategoryIds.Drinks).size)
        assertEquals(items, catalog.storefront.itemsIn(categoryId = null))
    }

    @Test
    fun allCatalogValuesAreValidAndEveryItemBelongsToDeclaredCategory() {
        val categoryIds = catalog.storefront.categories.map { it.id }.toSet()

        catalog.storefront.items.forEach { item ->
            assertTrue(item.title.isNotBlank())
            assertTrue(item.priceRub > 0L)
            assertTrue(item.categoryId in categoryIds)
            assertTrue(item.effects.satietyPercent in 0..100)
            assertTrue(item.effects.happinessPoints in 0..100)
            assertTrue(
                item.effects.satietyPercent > 0 ||
                    item.effects.happinessPoints > 0,
            )
        }
    }

    @Test
    fun quoteUsesCurrentPricesAndExactLongArithmetic() {
        val quote = catalog.quote(
            listOf(
                StoreCartLine(GroceryItemIds.Apple, 2),
                StoreCartLine(GroceryItemIds.Soup, 1),
                StoreCartLine(GroceryItemIds.Tea, 3),
            ),
        )

        assertEquals(listOf(30L, 45L, 45L), quote.lines.map { it.totalRub })
        assertEquals(120L, quote.totalRub)
    }

    @Test
    fun quoteCombinesDuplicateItemLines() {
        val quote = catalog.quote(
            listOf(
                StoreCartLine(GroceryItemIds.Apple, 1),
                StoreCartLine(GroceryItemIds.Apple, 2),
            ),
        )

        assertEquals(1, quote.lines.size)
        assertEquals(3, quote.lines.single().quantity)
        assertEquals(45L, quote.totalRub)
    }

    @Test
    fun quoteRejectsUnknownItemsAndArithmeticOverflow() {
        assertThrows(IllegalArgumentException::class.java) {
            catalog.quote(listOf(StoreCartLine(ProductId("store.grocery.item.unknown"), 1)))
        }

        val expensiveItem = object : SellableItem {
            override val id = ProductId("test.item.expensive")
            override val title = "Test"
            override val imageKey = ProductImageKey("test.image.expensive")
            override val priceRub = Long.MAX_VALUE
            override val categoryId = StoreCategoryId("test.category")
        }
        val expensiveCatalog = object : SellableCatalog<SellableItem> {
            override val storefront = StorefrontDefinition(
                storeId = StoreId("test.store"),
                title = "Test",
                allItemsLabel = "All",
                categories = listOf(StoreCategory(expensiveItem.categoryId, "Test")),
                items = listOf(expensiveItem),
            )
        }

        assertThrows(ArithmeticException::class.java) {
            expensiveCatalog.quote(listOf(StoreCartLine(expensiveItem.id, 2)))
        }
        assertThrows(ArithmeticException::class.java) {
            expensiveCatalog.quote(
                listOf(
                    StoreCartLine(expensiveItem.id, 1),
                    StoreCartLine(expensiveItem.id, 1),
                ),
            )
        }
    }

    @Test
    fun invalidDefinitionsAndValuesFailFast() {
        assertThrows(IllegalArgumentException::class.java) { StoreCartLine(GroceryItemIds.Apple, 0) }
        assertThrows(IllegalArgumentException::class.java) { StoreGridLayout(columns = 0) }
        assertThrows(IllegalArgumentException::class.java) {
            FoodEffects(satietyPercent = 0, happinessPoints = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            catalog.storefront.itemsIn(StoreCategoryId("store.grocery.category.unknown"))
        }
    }
}
