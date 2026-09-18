package github.detrig.feature.planning.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PlanningCalculatorTest {
    @Test fun preservesBudgetAndMarksGreenYellowAndRedProgress() {
        val plan = WeeklyPlan(weekNumber = 2, availableRub = 1_000, percentages = PlanPercentages.DEFAULT)
        val progress = PlanningCalculator.progress(plan, mapOf(
            PlanCategory.MANDATORY to 240,
            PlanCategory.WANTS to 385,
            PlanCategory.SAVINGS to 375,
        ))

        assertEquals(400L, progress.category(PlanCategory.MANDATORY).plannedRub)
        assertEquals(350L, progress.category(PlanCategory.WANTS).plannedRub)
        assertEquals(250L, progress.category(PlanCategory.SAVINGS).plannedRub)
        assertEquals(PlanProgressTone.ON_TRACK, progress.category(PlanCategory.MANDATORY).tone)
        assertEquals(PlanProgressTone.WARNING, progress.category(PlanCategory.WANTS).tone)
        assertEquals(PlanProgressTone.OVER_LIMIT, progress.category(PlanCategory.SAVINGS).tone)
    }

    @Test fun planPercentagesMustBeExactlyOneHundred() {
        assertThrows(IllegalArgumentException::class.java) { PlanPercentages(50, 20, 20) }
        assertThrows(IllegalArgumentException::class.java) { PlanPercentages(50, 60, -10) }
    }
}
