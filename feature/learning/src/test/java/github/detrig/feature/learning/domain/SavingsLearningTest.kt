package github.detrig.feature.learning.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SavingsLearningTest {
    @Test fun contributionRequiresRepetitionAcrossWeeks() {
        val rule = SavingsLearning.rules().first { it.metricId == LearningMetricIds.SAVINGS_REGULAR_CONTRIBUTION }
        val engine = LearningRuleEngine(listOf(rule))

        assertEquals(1, engine.evaluate(rule, listOf(1)).progressSteps)
        assertEquals(1, engine.evaluate(rule, listOf(1, 1, 1)).progressSteps)
        assertEquals(2, engine.evaluate(rule, listOf(1, 1, 2)).progressSteps)
    }

    @Test fun goalAndContributionAreDifferentFacts() {
        val goal = SavingsLearning.goalCreated("current", "starter:toy", 1)
        val transfer = SavingsLearning.contribution("current", "transfer-1", "starter:toy", 50, 1)

        assertEquals(SavingsLearning.GOAL_CREATED, goal.type)
        assertEquals(SavingsLearning.CONTRIBUTION, transfer.type)
        assertTrue(goal.actionId != transfer.actionId)
    }
}
