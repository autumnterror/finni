package github.detrig.feature.shop.presentation

import github.detrig.feature.shop.domain.ShopDecisionEvent
import github.detrig.feature.shop.domain.ShopDecisionEventType
import github.detrig.feature.shop.domain.ShopPromotionKind
import github.detrig.products.GroceryCatalog
import github.detrig.products.StoreCart
import org.junit.Assert.assertEquals
import org.junit.Test

class ShopCartViewStateTest {
    private val storefront = GroceryCatalog().storefront

    @Test
    fun buyTwoGetOneUsesDiscountedLineTotalInCart() {
        val item = storefront.items.first()
        val event = ShopDecisionEvent(
            eventId = "shop-event:test:bundle",
            type = ShopDecisionEventType.PROMOTION,
            gamePeriod = 1,
            eventPeriod = 1,
            storeId = storefront.storeId,
            productId = item.id,
            productTitle = item.title,
            regularPriceRub = item.priceRub,
            offeredPriceRub = item.priceRub,
            promotionKind = ShopPromotionKind.BUY_TWO_GET_ONE_FREE,
        )
        val state = ShopCartViewState(
            storefront = storefront,
            cart = StoreCart.Empty.add(item.id, quantity = 3),
            balanceRub = 100,
            loading = false,
            decisionEvent = event,
        )

        assertEquals(item.priceRub * 2, state.totalRub)
        assertEquals(1, state.lines.single().freeQuantity)
        assertEquals(item.priceRub, state.lines.single().savingRub)
    }

    @Test
    fun percentageDiscountUsesOfferedPriceForEveryUnit() {
        val item = storefront.items.first()
        val offeredPrice = item.priceRub - 5
        val event = ShopDecisionEvent(
            eventId = "shop-event:test:discount",
            type = ShopDecisionEventType.PROMOTION,
            gamePeriod = 1,
            eventPeriod = 1,
            storeId = storefront.storeId,
            productId = item.id,
            productTitle = item.title,
            regularPriceRub = item.priceRub,
            offeredPriceRub = offeredPrice,
            promotionKind = ShopPromotionKind.PERCENT_DISCOUNT,
        )
        val state = ShopCartViewState(
            storefront = storefront,
            cart = StoreCart.Empty.add(item.id, quantity = 2),
            balanceRub = 100,
            loading = false,
            decisionEvent = event,
        )

        assertEquals(offeredPrice * 2, state.totalRub)
        assertEquals(10L, state.lines.single().savingRub)
    }
}
