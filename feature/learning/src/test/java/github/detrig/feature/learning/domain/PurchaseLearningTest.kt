package github.detrig.feature.learning.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchaseLearningTest {
    @Test
    fun reasonablePurchaseNeedsTwoTripsThenThreeConsecutiveWeeks() {
        val rule = PurchaseLearning.rules().first {
            it.metricId == LearningMetricIds.PURCHASE_REASONABLE
        }
        val engine = LearningRuleEngine(listOf(rule))

        assertEquals(0, engine.evaluate(rule, listOf(1)).progressSteps)
        assertEquals(1, engine.evaluate(rule, listOf(1, 1)).progressSteps)
        assertEquals(1, engine.evaluate(rule, listOf(1, 3, 4)).progressSteps)
        assertEquals(2, engine.evaluate(rule, listOf(2, 3, 4)).progressSteps)
    }

    @Test
    fun promotionAndImpulseKeepIntroductionAndLearnedProgressSteps() {
        PurchaseLearning.rules()
            .filter { it.metricId != LearningMetricIds.PURCHASE_REASONABLE }
            .forEach { rule ->
                val engine = LearningRuleEngine(listOf(rule))

                assertEquals(1, engine.evaluate(rule, listOf(1)).progressSteps)
                assertEquals(1, engine.evaluate(rule, listOf(1, 1)).progressSteps)
                assertEquals(2, engine.evaluate(rule, listOf(1, 2)).progressSteps)
            }
    }

    @Test
    fun reasonablePurchaseAllowsSmallExtrasWhenFoodAndMandatoryMoneyAreCovered() {
        val actions = PurchaseLearning.qualifyingActions(
            profileId = "current",
            gamePeriod = 1,
            sourceOperationId = "checkout-1",
            context = baseContext(
                scenario = PurchaseScenario.STANDARD_PURCHASE,
                decision = PurchaseDecision.PURCHASED,
                mandatoryFoodNeeded = true,
                foodUnitsPurchased = 1,
                requiredFoodCostRub = 20,
                extraUnitsPurchased = 2,
                optionalPurchaseRub = 35,
                balanceAfterPurchaseRub = 425,
                purchaseTotalRub = 55,
            ),
        )

        assertEquals(listOf(PurchaseLearning.REASONABLE_DECISION), actions.map { it.type })
    }

    @Test
    fun purchaseWithoutNeededFoodDoesNotQualify() {
        val context = baseContext(
            scenario = PurchaseScenario.STANDARD_PURCHASE,
            decision = PurchaseDecision.PURCHASED,
            mandatoryFoodNeeded = true,
            foodUnitsPurchased = 0,
            extraUnitsPurchased = 1,
            optionalPurchaseRub = 50,
            balanceAfterPurchaseRub = 430,
            purchaseTotalRub = 50,
        )
        val actions = PurchaseLearning.qualifyingActions(
            profileId = "current",
            gamePeriod = 1,
            sourceOperationId = "checkout-2",
            context = context,
        )

        assertTrue(actions.isEmpty())
        assertEquals(PurchaseProblem.REQUIRED_FOOD_MISSING, context.purchaseProblem(PurchaseAssessmentConfig()))
    }

    @Test
    fun promotionAndImpulseJudgeTheDecisionInsteadOfBuyVersusDeclineAlone() {
        val promotionDecline = PurchaseLearning.qualifyingActions(
            profileId = "current",
            gamePeriod = 2,
            sourceOperationId = "event-promotion",
            context = baseContext(
                scenario = PurchaseScenario.PROMOTION,
                decision = PurchaseDecision.DECLINED,
                eventTargetNeeded = false,
                eventTargetPriceRub = 40,
                promotionSavingRub = 10,
            ),
        )
        val impulsePurchase = PurchaseLearning.qualifyingActions(
            profileId = "current",
            gamePeriod = 2,
            sourceOperationId = "event-impulse",
            context = baseContext(
                scenario = PurchaseScenario.IMPULSE_WISH,
                decision = PurchaseDecision.PURCHASED,
                eventTargetPurchased = true,
                eventTargetPriceRub = 40,
                discretionaryRub = 100,
            ),
        )

        assertEquals(listOf(PurchaseLearning.PROMOTION_DECISION), promotionDecline.map { it.type })
        assertEquals(listOf(PurchaseLearning.IMPULSE_DECISION), impulsePurchase.map { it.type })
    }

    @Test
    fun decliningNeededAffordablePromotionDoesNotQualify() {
        val actions = PurchaseLearning.qualifyingActions(
            profileId = "current",
            gamePeriod = 2,
            sourceOperationId = "needed-promotion",
            context = baseContext(
                scenario = PurchaseScenario.PROMOTION,
                decision = PurchaseDecision.DECLINED,
                mandatoryFoodNeeded = true,
                eventTargetNeeded = true,
                eventTargetPriceRub = 40,
                promotionSavingRub = 10,
            ),
        )

        assertTrue(actions.isEmpty())
    }

    @Test
    fun purchaseProblemExplainsMandatoryReserveAndExcessExtras() {
        val reserveProblem = baseContext(
            scenario = PurchaseScenario.STANDARD_PURCHASE,
            decision = PurchaseDecision.PURCHASED,
            balanceAfterPurchaseRub = 80,
            purchaseTotalRub = 400,
        )
        val extrasProblem = baseContext(
            scenario = PurchaseScenario.STANDARD_PURCHASE,
            decision = PurchaseDecision.PURCHASED,
            balanceAfterPurchaseRub = 300,
            purchaseTotalRub = 180,
            extraUnitsPurchased = 3,
            optionalPurchaseRub = 180,
            discretionaryRub = 100,
        )

        assertEquals(
            PurchaseProblem.MANDATORY_MONEY_AT_RISK,
            reserveProblem.purchaseProblem(PurchaseAssessmentConfig()),
        )
        assertEquals(
            PurchaseProblem.TOO_MANY_EXTRAS,
            extrasProblem.purchaseProblem(PurchaseAssessmentConfig()),
        )
    }

    @Test
    fun excessivePromotionQuantityIsUnreasonableEvenWhenItFitsTheBudget() {
        val context = baseContext(
            scenario = PurchaseScenario.PROMOTION,
            decision = PurchaseDecision.PURCHASED,
            foodUnitsPurchased = 6,
            extraUnitsPurchased = 5,
            optionalPurchaseRub = 60,
            balanceAfterPurchaseRub = 420,
            purchaseTotalRub = 60,
            discretionaryRub = 200,
            eventTargetPurchased = true,
            eventTargetNeeded = true,
            eventTargetPriceRub = 60,
            promotionSavingRub = 30,
            eventTargetQuantity = 6,
            eventTargetMinimumQuantity = 3,
        )

        assertEquals(
            PurchaseProblem.PROMOTION_OVERBUY,
            context.purchaseProblem(PurchaseAssessmentConfig()),
        )
        assertTrue(
            PurchaseLearning.qualifyingActions(
                profileId = "current",
                gamePeriod = 1,
                sourceOperationId = "promotion-overbuy",
                context = context,
            ).isEmpty(),
        )
    }

    @Test
    fun promotionQuantityAtConfiguredLimitCanStillQualify() {
        val context = baseContext(
            scenario = PurchaseScenario.PROMOTION,
            decision = PurchaseDecision.PURCHASED,
            foodUnitsPurchased = 5,
            extraUnitsPurchased = 4,
            optionalPurchaseRub = 50,
            balanceAfterPurchaseRub = 430,
            purchaseTotalRub = 50,
            discretionaryRub = 200,
            eventTargetPurchased = true,
            eventTargetNeeded = true,
            eventTargetPriceRub = 50,
            promotionSavingRub = 20,
            eventTargetQuantity = 5,
            eventTargetMinimumQuantity = 3,
        )

        assertEquals(null, context.purchaseProblem(PurchaseAssessmentConfig()))
        assertEquals(
            listOf(PurchaseLearning.PROMOTION_DECISION),
            PurchaseLearning.qualifyingActions(
                profileId = "current",
                gamePeriod = 1,
                sourceOperationId = "promotion-limit",
                context = context,
            ).map { it.type },
        )
    }

    private fun baseContext(
        scenario: PurchaseScenario,
        decision: PurchaseDecision,
        mandatoryFoodNeeded: Boolean = false,
        foodUnitsPurchased: Int = 0,
        requiredFoodCostRub: Long = 0,
        extraUnitsPurchased: Int = 0,
        optionalPurchaseRub: Long = 0,
        balanceAfterPurchaseRub: Long = 480,
        purchaseTotalRub: Long = 0,
        discretionaryRub: Long = 120,
        eventTargetPurchased: Boolean = false,
        eventTargetNeeded: Boolean = false,
        eventTargetPriceRub: Long = 0,
        promotionSavingRub: Long = 0,
        eventTargetQuantity: Int = 0,
        eventTargetMinimumQuantity: Int = 0,
    ) = PurchaseDecisionContext(
        scenario = scenario,
        decision = decision,
        balanceBeforeRub = 480,
        balanceAfterPurchaseRub = balanceAfterPurchaseRub,
        purchaseTotalRub = purchaseTotalRub,
        futureMandatoryRub = 120,
        discretionaryRub = discretionaryRub,
        mandatoryFoodNeeded = mandatoryFoodNeeded,
        foodUnitsPurchased = foodUnitsPurchased,
        requiredFoodCostRub = requiredFoodCostRub,
        extraUnitsPurchased = extraUnitsPurchased,
        optionalPurchaseRub = optionalPurchaseRub,
        eventTargetPurchased = eventTargetPurchased,
        eventTargetNeeded = eventTargetNeeded,
        eventTargetPriceRub = eventTargetPriceRub,
        promotionSavingRub = promotionSavingRub,
        eventTargetQuantity = eventTargetQuantity,
        eventTargetMinimumQuantity = eventTargetMinimumQuantity,
    )
}
