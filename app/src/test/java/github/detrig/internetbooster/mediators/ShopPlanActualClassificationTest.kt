package github.detrig.internetbooster.mediators

import github.detrig.products.GroceryItemIds
import org.junit.Assert.assertEquals
import org.junit.Test

class ShopPlanActualClassificationTest {
    @Test
    fun ordinaryFoodPurchaseIsMandatory() {
        val result = classifyShopPlanActuals(
            lines = listOf(
                ShopPlanLine(GroceryItemIds.Apple, isFood = true, totalRub = 30),
                ShopPlanLine(GroceryItemIds.Soup, isFood = true, totalRub = 45),
            ),
            impulseWishProductId = null,
        )

        assertEquals(75L, result.mandatoryRub)
        assertEquals(0L, result.wantsRub)
    }

    @Test
    fun wishedFoodIsAWantWhileOtherFoodStaysMandatory() {
        val result = classifyShopPlanActuals(
            lines = listOf(
                ShopPlanLine(GroceryItemIds.Apple, isFood = true, totalRub = 30),
                ShopPlanLine(GroceryItemIds.Soup, isFood = true, totalRub = 45),
            ),
            impulseWishProductId = GroceryItemIds.Soup,
        )

        assertEquals(30L, result.mandatoryRub)
        assertEquals(45L, result.wantsRub)
    }
}
