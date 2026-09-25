package github.detrig.internetbooster.mediators

import github.detrig.feature.learning.domain.PurchaseDecision
import github.detrig.feature.learning.domain.PurchaseDecisionContext
import github.detrig.feature.learning.domain.PurchaseScenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShopPurchaseLearningPayloadCodecTest {
    @Test
    fun payloadRoundTripsWithoutLosingAssessmentFacts() {
        val context = PurchaseDecisionContext(
            scenario = PurchaseScenario.STANDARD_PURCHASE,
            decision = PurchaseDecision.PURCHASED,
            balanceBeforeRub = 500,
            balanceAfterPurchaseRub = 430,
            purchaseTotalRub = 70,
            futureMandatoryRub = 150,
            discretionaryRub = 120,
            mandatoryFoodNeeded = true,
            foodUnitsPurchased = 2,
            requiredFoodCostRub = 20,
            extraUnitsPurchased = 1,
            optionalPurchaseRub = 50,
        )
        val payload = ShopPurchaseLearningPayload(
            gamePeriod = 3,
            sourceOperationId = "shop-checkout:3:test",
            standardPurchase = context,
            eventDecision = context.copy(
                scenario = PurchaseScenario.PROMOTION,
                eventTargetPurchased = true,
                eventTargetNeeded = true,
                eventTargetPriceRub = 20,
                promotionSavingRub = 5,
                eventTargetQuantity = 3,
                eventTargetMinimumQuantity = 3,
            ),
        )

        assertEquals(payload, ShopPurchaseLearningPayloadCodec.decode(ShopPurchaseLearningPayloadCodec.encode(payload)))
    }

    @Test
    fun malformedPayloadIsIgnoredDuringReconciliation() {
        assertNull(ShopPurchaseLearningPayloadCodec.decode("not-base64"))
    }
}
