package github.detrig.feature.learning.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class FinancialSecurityLearningTest {

    private val engine = LearningRuleEngine(FinancialSecurityLearning.rules())

    @Test
    fun firstSafeResponseUnlocksIntroductionStep() {
        val rule = engine.rulesFor(FinancialSecurityLearning.CONFIRMATION_CODE_SAFE_RESPONSE).single()

        val progress = engine.evaluate(rule, qualifyingPeriods = listOf(2))

        assertEquals(1, progress.progressSteps)
    }

    @Test
    fun threeSafeResponsesOnDifferentDaysUnlockLearnedStep() {
        val rule = engine.rulesFor(FinancialSecurityLearning.UNKNOWN_LINK_SAFE_RESPONSE).single()

        val progress = engine.evaluate(rule, qualifyingPeriods = listOf(2, 4, 7))

        assertEquals(3, progress.progressSteps)
    }

    @Test
    fun repeatedDeliveryForOneDayDoesNotUnlockLearnedStep() {
        val rule = engine.rulesFor(FinancialSecurityLearning.UNKNOWN_LINK_SAFE_RESPONSE).single()

        val progress = engine.evaluate(rule, qualifyingPeriods = listOf(2, 2, 2))

        assertEquals(1, progress.progressSteps)
    }

    @Test
    fun codeAndLinkProduceIndependentAchievementActions() {
        val code = FinancialSecurityLearning.safeResponseAction(
            actionId = "code",
            profileId = "profile",
            absoluteDay = 1,
            eventId = "event-code",
            scenario = SecurityScenario.CONFIRMATION_CODE,
        )
        val link = FinancialSecurityLearning.safeResponseAction(
            actionId = "link",
            profileId = "profile",
            absoluteDay = 2,
            eventId = "event-link",
            scenario = SecurityScenario.UNKNOWN_LINK,
        )

        assertEquals(FinancialSecurityLearning.CONFIRMATION_CODE_SAFE_RESPONSE, code.type)
        assertEquals(FinancialSecurityLearning.UNKNOWN_LINK_SAFE_RESPONSE, link.type)
    }
}
