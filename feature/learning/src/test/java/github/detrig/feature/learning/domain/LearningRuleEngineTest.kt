package github.detrig.feature.learning.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LearningRuleEngineTest {
    private val actionType = LearningActionType("test.action")

    @Test
    fun learnedMilestoneCanRequireSeveralDistinctPeriods() {
        val rule = MetricRuleDefinition(
            metricId = LearningMetricIds.SAVINGS_REGULAR_CONTRIBUTION,
            actionTypes = setOf(actionType),
            milestones = listOf(
                ProgressMilestone(progressSteps = 1, requiredActions = 1),
                ProgressMilestone(progressSteps = 2, requiredActions = 3, requiredDistinctPeriods = 3),
            ),
        )
        val engine = LearningRuleEngine(listOf(rule))

        assertEquals(1, engine.evaluate(rule, listOf(2)).progressSteps)
        assertEquals(1, engine.evaluate(rule, listOf(2, 2, 3)).progressSteps)
        val learned = engine.evaluate(rule, listOf(2, 3, 4))
        assertEquals(2, learned.progressSteps)
        assertEquals(3, learned.currentStreak)
    }

    @Test
    fun securityRuleKeepsIndependentStepWithoutUnlockingLearnedTooEarly() {
        val rule = MetricRuleDefinition(
            metricId = LearningMetricIds.SECURITY_UNKNOWN_LINK,
            actionTypes = setOf(actionType),
            milestones = listOf(
                ProgressMilestone(progressSteps = 1, requiredActions = 1),
                ProgressMilestone(progressSteps = 2, requiredActions = 2),
                ProgressMilestone(progressSteps = 3, requiredActions = 3, requiredDistinctPeriods = 2),
            ),
        )
        val engine = LearningRuleEngine(listOf(rule))

        assertEquals(1, engine.evaluate(rule, listOf(1)).progressSteps)
        assertEquals(2, engine.evaluate(rule, listOf(1, 1)).progressSteps)
        assertEquals(3, engine.evaluate(rule, listOf(1, 1, 2)).progressSteps)
    }
}
