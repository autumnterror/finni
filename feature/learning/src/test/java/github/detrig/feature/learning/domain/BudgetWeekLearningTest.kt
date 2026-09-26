package github.detrig.feature.learning.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetWeekLearningTest {
    private val base = BudgetWeekSnapshot(
        planAdequate = true,
        availableRub = 500,
        plannedMandatoryRub = 200,
        plannedWantsRub = 150,
        plannedSavingsRub = 100,
        plannedReserveRub = 50,
        actualMandatoryRub = 200,
        actualWantsRub = 150,
        actualSavingsRub = 100,
    )

    @Test fun weakPlanCanIntroduceComparisonButCannotCountAsFollowing() {
        val actions = BudgetWeekLearning.actionsForCompletedWeek("current", 1, base.copy(planAdequate = false))
        assertEquals(listOf(BudgetWeekLearning.WEEK_REVIEWED), actions.map { it.type })
        val changed = BudgetWeekLearning.actionsForCompletedWeek(
            "current", 2,
            base.copy(planAdequate = false, actualMandatoryRub = 280, actualWantsRub = 70),
        )
        assertEquals(listOf(BudgetWeekLearning.WEEK_REVIEWED, BudgetWeekLearning.CHANGE_SEEN), changed.map { it.type })
    }

    @Test fun goodWeeksRequireThreeEligiblePeriodsEvenAfterPoorReview() {
        val rule = BudgetWeekLearning.rules().first { it.metricId == LearningMetricIds.BUDGET_FOLLOW_PLAN }
        val engine = LearningRuleEngine(listOf(rule))
        val reviewed = listOf(1L, 2L, 3L, 4L)
        assertEquals(1, engine.evaluate(rule, reviewed, mapOf(2 to listOf(2L, 3L))).progressSteps)
        assertEquals(2, engine.evaluate(rule, reviewed, mapOf(2 to listOf(2L, 3L, 4L))).progressSteps)
    }

    @Test fun mandatoryOverrunIntroducesChangeAndMeasuredAdjustmentCanLearn() {
        val compensated = base.copy(actualMandatoryRub = 280, actualWantsRub = 70)
        val overspent = base.copy(actualMandatoryRub = 280, actualWantsRub = 150)
        assertEquals(
            listOf(BudgetWeekLearning.WEEK_FOLLOWED, BudgetWeekLearning.CHANGE_ADAPTED),
            BudgetWeekLearning.actionsForCompletedWeek("current", 1, compensated).map { it.type },
        )
        assertEquals(
            listOf(BudgetWeekLearning.WEEK_REVIEWED, BudgetWeekLearning.CHANGE_SEEN),
            BudgetWeekLearning.actionsForCompletedWeek("current", 2, overspent).map { it.type },
        )
        val rule = BudgetWeekLearning.rules().first { it.metricId == LearningMetricIds.BUDGET_ADAPT_TO_CHANGE }
        val engine = LearningRuleEngine(listOf(rule))
        assertEquals(1, engine.evaluate(rule, listOf(1L, 2L), mapOf(2 to listOf(1L))).progressSteps)
        assertEquals(2, engine.evaluate(rule, listOf(1L, 2L, 3L), mapOf(2 to listOf(1L, 3L))).progressSteps)
    }

    @Test fun earlyFinishOnlyIntroducesComparison() {
        val actions = BudgetWeekLearning.actionsForCompletedWeek(
            "current", 1, base.copy(actualMandatoryRub = 280, actualWantsRub = 70), earlyFinish = true,
        )
        assertEquals(listOf(BudgetWeekLearning.WEEK_REVIEWED), actions.map { it.type })
        assertTrue(actions.all { it.actionId.startsWith("budget-week:1:") })
        assertFalse(actions.any { it.type == BudgetWeekLearning.CHANGE_ADAPTED })
    }
}
