package github.detrig.feature.room.presentation

import github.detrig.feature.planning.domain.CategoryPlanProgress
import github.detrig.feature.planning.domain.PlanCategory
import github.detrig.feature.planning.domain.PlanPercentages
import github.detrig.feature.planning.domain.PlanProgressTone
import github.detrig.feature.planning.domain.WeeklyPlan
import github.detrig.feature.planning.domain.WeeklyPlanProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WeekPlanAssessmentTest {

    @Test
    fun `all categories and reserve match`() {
        val result = progress(mandatory = 200, wants = 125, savings = 100).assessWeek()

        assertEquals(WeekPlanOutcome.ALL_MATCHED, result.outcome)
        assertEquals(75L, result.actualReserveRub)
        assertEquals(WeekPlanItem.entries.toSet(), result.matchedItems)
    }

    @Test
    fun `underspending mandatory and wants does not count as following the plan`() {
        val result = progress(mandatory = 0, wants = 0, savings = 100).assessWeek()

        assertFalse(result.matches(PlanCategory.MANDATORY))
        assertFalse(result.matches(PlanCategory.WANTS))
        assertEquals(WeekPlanOutcome.PARTIALLY_MATCHED, result.outcome)
    }

    @Test
    fun `overspending mandatory and wants does not count as following the plan`() {
        val result = progress(mandatory = 201, wants = 126, savings = 100).assessWeek()

        assertFalse(result.matches(PlanCategory.MANDATORY))
        assertFalse(result.matches(PlanCategory.WANTS))
        assertEquals(WeekPlanOutcome.PARTIALLY_MATCHED, result.outcome)
    }

    @Test
    fun `oversaving does not claim that the exact plan was followed`() {
        val result = progress(mandatory = 200, wants = 125, savings = 101).assessWeek()

        assertFalse(result.matches(PlanCategory.SAVINGS))
        assertFalse(WeekPlanItem.RESERVE in result.matchedItems)
    }

    @Test
    fun `partial result lists only missed items`() {
        val result = progress(mandatory = 250, wants = 125, savings = 80).assessWeek()

        assertEquals(WeekPlanOutcome.PARTIALLY_MATCHED, result.outcome)
        assertEquals(setOf(WeekPlanItem.WANTS), result.matchedItems)
        assertEquals(
            setOf(WeekPlanItem.MANDATORY, WeekPlanItem.SAVINGS, WeekPlanItem.RESERVE),
            result.missedItems,
        )
    }

    @Test
    fun `no matching items produces supportive retry outcome`() {
        val result = progress(mandatory = 220, wants = 140, savings = 80).assessWeek()

        assertEquals(WeekPlanOutcome.TRY_AGAIN, result.outcome)
        assertEquals(emptySet<WeekPlanItem>(), result.matchedItems)
    }

    private fun progress(
        mandatory: Long,
        wants: Long,
        savings: Long,
    ): WeeklyPlanProgress = WeeklyPlanProgress(
        plan = WeeklyPlan(
            weekNumber = 1,
            availableRub = 500,
            percentages = PlanPercentages(mandatory = 40, wants = 25, savings = 20),
        ),
        categories = listOf(
            CategoryPlanProgress(PlanCategory.MANDATORY, 200, mandatory, PlanProgressTone.ON_TRACK),
            CategoryPlanProgress(PlanCategory.WANTS, 125, wants, PlanProgressTone.ON_TRACK),
            CategoryPlanProgress(PlanCategory.SAVINGS, 100, savings, PlanProgressTone.ON_TRACK),
        ),
    )
}
