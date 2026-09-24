package github.detrig.feature.shop.domain

import github.detrig.products.GroceryCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopLearningEventGeneratorTest {
    private val storefront = GroceryCatalog().storefront

    @Test
    fun probabilitiesCanDisableAllEventsIndependently() {
        val generator = ShopLearningEventGenerator(
            ShopLearningEventConfig(
                promotionEventProbability = 0.0,
                impulseWishEventProbability = 0.0,
            ),
        )

        assertNull(generator.eventFor(storefront, gamePeriod = 1, eventPeriod = 1))
    }

    @Test
    fun forcedPromotionIsStableAndUsesDiscountedPrice() {
        val generator = ShopLearningEventGenerator(
            ShopLearningEventConfig(
                forcedEventType = ShopDecisionEventType.PROMOTION,
                forcedPromotionKind = ShopPromotionKind.PERCENT_DISCOUNT,
                promotionDiscountPercent = 25,
                randomSeed = 42,
            ),
        )

        val first = generator.eventFor(storefront, gamePeriod = 2, eventPeriod = 9)
        val restored = generator.eventFor(storefront, gamePeriod = 2, eventPeriod = 9)

        assertNotNull(first)
        assertEquals(first, restored)
        assertEquals(ShopPromotionKind.PERCENT_DISCOUNT, requireNotNull(first).promotionKind)
        assertTrue(requireNotNull(first).offeredPriceRub < first.regularPriceRub)
    }

    @Test
    fun buyTwoGetOneChargesTwoUnitsForEveryThree() {
        val generator = ShopLearningEventGenerator(
            ShopLearningEventConfig(
                forcedEventType = ShopDecisionEventType.PROMOTION,
                forcedPromotionKind = ShopPromotionKind.BUY_TWO_GET_ONE_FREE,
                firstPromotionDelayDays = 0,
                randomSeed = 7,
            ),
        )

        val event = requireNotNull(generator.eventFor(storefront, gamePeriod = 1, eventPeriod = 1))
        val price = event.priceLine(event.productId, event.regularPriceRub, quantity = 6)

        assertEquals(ShopPromotionKind.BUY_TWO_GET_ONE_FREE, event.promotionKind)
        assertEquals(event.regularPriceRub, event.offeredPriceRub)
        assertEquals(4 * event.regularPriceRub, price.chargedTotalRub)
        assertEquals(2 * event.regularPriceRub, price.savingRub)
        assertEquals(2, price.freeQuantity)
    }

    @Test
    fun buyTwoGetOneDoesNotDiscountAnIncompleteSet() {
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

        val price = event.priceLine(item.id, item.priceRub, quantity = 2)

        assertEquals(2 * item.priceRub, price.chargedTotalRub)
        assertEquals(0L, price.savingRub)
        assertEquals(0, price.freeQuantity)
    }

    @Test
    fun promotionWaitsForConfiguredNumberOfDays() {
        val generator = ShopLearningEventGenerator(
            ShopLearningEventConfig(
                promotionEventProbability = 0.0,
                impulseWishEventProbability = 0.0,
                firstPromotionDelayDays = 3,
            ),
        )

        assertNull(generator.eventFor(storefront, gamePeriod = 1, eventPeriod = 3))
        assertEquals(
            ShopDecisionEventType.PROMOTION,
            generator.eventFor(storefront, gamePeriod = 1, eventPeriod = 4)?.type,
        )
    }
}
