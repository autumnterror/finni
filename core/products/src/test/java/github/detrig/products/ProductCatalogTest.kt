package github.detrig.products

import org.junit.Assert.*
import org.junit.Test

class ProductCatalogTest {
    @Test fun productsHaveStableUniqueIdentities() {
        val catalog = DefaultProductCatalog()
        assertEquals(8, catalog.products.size)
        assertEquals(catalog.products.size, catalog.products.map { it.id }.distinct().size)
        assertEquals(ProductKind.GROATS, catalog.find(ProductId("groats_bag"))?.kind)
        assertNull(catalog.find(ProductId("absent")))
        assertThrows(IllegalArgumentException::class.java) { ProductQuantity(ProductIds.Apple, 0) }
    }

    @Test fun quoteCountsEveryUnitIncludingProductsOutsideTheShoppingList() {
        val quote = DefaultProductCatalog().quote(listOf(
            ProductQuantity(ProductIds.Groats, 1),
            ProductQuantity(ProductIds.Carrot, 2),
            ProductQuantity(ProductIds.Apple, 1),
            ProductQuantity(ProductIds.Berries, 3),
        ))
        assertEquals(listOf(40L, 20L, 20L, 105L), quote.lines.map { it.totalRub })
        assertEquals(185L, quote.totalRub)
    }

    @Test fun emptyCartCostsZero() {
        assertEquals(0L, DefaultProductCatalog().quote(emptyList()).totalRub)
    }

    @Test fun quoteDoesNotOverflowAnIntegerWhenMultiplyingOrAddingPrices() {
        val catalog = object : ProductCatalog {
            override val products = listOf(Product(ProductIds.Apple, ProductKind.APPLE, Int.MAX_VALUE))
        }
        assertEquals(4_294_967_294L,
            catalog.quote(listOf(ProductQuantity(ProductIds.Apple, 2))).totalRub)
        assertEquals(4_294_967_294L, catalog.quote(listOf(
            ProductQuantity(ProductIds.Apple, 1), ProductQuantity(ProductIds.Apple, 1),
        )).totalRub)
    }

    @Test fun invalidPricesAndUnknownProductsCannotSilentlyUnderstateTheTotal() {
        assertThrows(IllegalArgumentException::class.java) {
            Product(ProductIds.Apple, ProductKind.APPLE, -1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            DefaultProductCatalog().quote(listOf(ProductQuantity(ProductId("unknown"), 1)))
        }
    }
}
