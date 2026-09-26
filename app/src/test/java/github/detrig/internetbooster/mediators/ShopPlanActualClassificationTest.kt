package github.detrig.internetbooster.mediators

import org.junit.Assert.assertEquals
import org.junit.Test

class ShopPlanActualClassificationTest {
    @Test
    fun ordinaryFoodPurchaseIsMandatory() {
        val result = classifyShopPlanActuals(
            lines = listOf(
                ShopPlanLine(restoresSatiety = true, totalRub = 30),
                ShopPlanLine(restoresSatiety = true, totalRub = 60),
            ),
        )

        assertEquals(90L, result.mandatoryRub)
        assertEquals(0L, result.wantsRub)
    }

    @Test
    fun foodWithoutSatietyIsAWantWhileCareFoodStaysMandatory() {
        val result = classifyShopPlanActuals(
            lines = listOf(
                ShopPlanLine(restoresSatiety = true, totalRub = 30),
                ShopPlanLine(restoresSatiety = false, totalRub = 65),
            ),
        )

        assertEquals(30L, result.mandatoryRub)
        assertEquals(65L, result.wantsRub)
    }

    @Test
    fun discountedMixedBasketUsesPaidLineTotals() {
        val result = classifyShopPlanActuals(
            lines = listOf(
                ShopPlanLine(restoresSatiety = true, totalRub = 42),
                ShopPlanLine(restoresSatiety = false, totalRub = 65),
            ),
        )

        assertEquals(42L, result.mandatoryRub)
        assertEquals(65L, result.wantsRub)
    }
}
