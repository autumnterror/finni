package github.detrig.feature.productmarket.domain

import github.detrig.products.ProductId
import github.detrig.products.ProductIds
import github.detrig.products.ProductQuantity

enum class MarketDepartment { PRODUCE, BREAKFAST, FRESH, GROATS, REMINDER, BEFORE_CHECKOUT }

data class MarketShelf(val department: MarketDepartment, val products: List<ProductId>)

data class MarketSlot(
    val shelfIndex: Int,
    val index: Int,
    val productId: ProductId,
    val worldX: Double,
    val width: Double,
) {
    val row: Int get() = index / 4
    fun instanceId(lap: Int): String = "$lap:$shelfIndex:$index"
}

data class MarketConfiguration(
    val routeVersion: Int = 3,
    val speed: Double = 70.0,
    val bayWidth: Double = 352.0,
    val shelfWidth: Double = 300.0,
    val shelfInset: Double = 26.0,
    val slotInset: Double = 14.0,
    val slotWidth: Double = 65.0,
    val slotGap: Double = 4.0,
    val checkoutOffset: Double = 72.0,
    val brakingDistance: Double = 96.0,
    val arrivalPauseSeconds: Double = .7,
    val maximumQuantity: Int = 12,
    val requested: List<ProductQuantity> = listOf(
        ProductQuantity(ProductIds.Groats, 1),
        ProductQuantity(ProductIds.Carrot, 2),
        ProductQuantity(ProductIds.Apple, 1),
    ),
    val shelves: List<MarketShelf> = defaultShelves(),
) {
    init {
        require(speed > 0 && speed.isFinite())
        require(bayWidth >= shelfWidth && shelfWidth > 0)
        require(shelves.isNotEmpty() && shelves.all { it.products.size == 12 })
        require(slotWidth >= 48 && brakingDistance > 0 && arrivalPauseSeconds >= 0)
        require(maximumQuantity > 0 && requested.map { it.productId }.distinct().size == requested.size)
        require(requested.all { it.quantity <= maximumQuantity })
    }
    val checkoutWorldX: Double get() = shelves.size * bayWidth
    val endDistance: Double get() = checkoutWorldX + checkoutOffset
    val slots: List<MarketSlot> = shelves.flatMapIndexed { shelf, data ->
        data.products.mapIndexed { index, id ->
            MarketSlot(shelf, index, id,
                shelf * bayWidth + shelfInset + slotInset + (index % 4) * (slotWidth + slotGap), slotWidth)
        }
    }
}

private fun defaultShelves(): List<MarketShelf> {
    val c = ProductIds.Carrot; val a = ProductIds.Apple; val b = ProductIds.Berries
    val g = ProductIds.Groats; val m = ProductIds.Milk; val y = ProductIds.Yogurt
    val r = ProductIds.ReadyMeal; val k = ProductIds.Crackers
    return listOf(
        MarketShelf(MarketDepartment.PRODUCE, listOf(c,a,b,a,c,b,a,c,b,a,c,b)),
        MarketShelf(MarketDepartment.BREAKFAST, listOf(g,k,m,y,g,r,m,g,k,y,r,m)),
        MarketShelf(MarketDepartment.FRESH, listOf(c,b,a,c,a,y,b,a,c,b,a,y)),
        MarketShelf(MarketDepartment.GROATS, listOf(g,m,k,y,r,g,m,y,g,k,m,r)),
        MarketShelf(MarketDepartment.PRODUCE, listOf(a,c,b,a,c,a,b,c,a,b,c,a)),
        MarketShelf(MarketDepartment.BREAKFAST, listOf(m,g,r,k,y,g,m,k,g,y,m,r)),
        MarketShelf(MarketDepartment.REMINDER, listOf(c,a,g,c,a,g,c,a,g,c,a,g)),
        MarketShelf(MarketDepartment.BEFORE_CHECKOUT, listOf(g,c,a,c,k,b,a,g,c,a,g,b)),
    )
}
