package github.detrig.feature.planning.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyResultEvaluationTest {
    private val plan = WeeklyPlan(
        weekNumber = 1,
        availableRub = 500,
        percentages = PlanPercentages(mandatory = 40, wants = 30, savings = 20),
    )

    @Test
    fun closePlanAndActualIsGoodResult() {
        assertTrue(progress(mandatory = 195, wants = 150, savings = 100).isGoodWeeklyResult())
    }

    @Test
    fun largeDeviationIsNotGoodResult() {
        assertFalse(progress(mandatory = 80, wants = 250, savings = 0).isGoodWeeklyResult())
    }

    private fun progress(mandatory: Long, wants: Long, savings: Long) = WeeklyPlanProgress(
        plan = plan,
        categories = listOf(
            CategoryPlanProgress(PlanCategory.MANDATORY, plan.plannedRub(PlanCategory.MANDATORY), mandatory, PlanProgressTone.ON_TRACK),
            CategoryPlanProgress(PlanCategory.WANTS, plan.plannedRub(PlanCategory.WANTS), wants, PlanProgressTone.ON_TRACK),
            CategoryPlanProgress(PlanCategory.SAVINGS, plan.plannedRub(PlanCategory.SAVINGS), savings, PlanProgressTone.ON_TRACK),
        ),
    )
}
