package github.detrig.internetbooster.mediators

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavingsGoalFulfillmentVisibilityTest {
    @Test
    fun `does not combine wallet with savings when checking completed goal`() {
        assertFalse(
            canShowSavingsGoalReachedFulfillment(
                sourceOperationId = "savings-goal-reached:room-zone:fishing:600",
                currentSavingsRub = 300,
            ),
        )
    }

    @Test
    fun `shows completed goal when savings alone cover target`() {
        assertTrue(
            canShowSavingsGoalReachedFulfillment(
                sourceOperationId = "savings-goal-reached:room-zone:fishing:600",
                currentSavingsRub = 600,
            ),
        )
    }

    @Test
    fun `does not affect non savings fulfillments`() {
        assertTrue(
            canShowSavingsGoalReachedFulfillment(
                sourceOperationId = "pet-wish-purchase:pastry",
                currentSavingsRub = 0,
            ),
        )
    }
}
