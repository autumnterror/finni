package github.detrig.internetbooster.mediators

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopPurchaseTrackingConfigTest {

    private val config = ShopPurchaseTrackingConfig(minimumFoodStockUnits = 1)

    @Test
    fun `food is required when inventory has no portions`() {
        assertTrue(config.isFoodPurchaseRequired(stockFoodUnits = 0))
    }

    @Test
    fun `food is not required when inventory already has a portion`() {
        assertFalse(config.isFoodPurchaseRequired(stockFoodUnits = 1))
    }
}
