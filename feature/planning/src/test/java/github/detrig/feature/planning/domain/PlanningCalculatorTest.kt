package github.detrig.feature.planning.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PlanningCalculatorTest {
    @Test fun preservesBudgetAndMarksGreenYellowAndRedProgress() {
        val plan = WeeklyPlan(weekNumber = 2, availableRub = 1_000, percentages = PlanPercentages.DEFAULT)
        val progress = PlanningCalculator.progress(plan, mapOf(
            PlanCategory.MANDATORY to 240,
            PlanCategory.WANTS to 275,
            PlanCategory.SAVINGS to 310,
        ))

        assertEquals(400L, progress.category(PlanCategory.MANDATORY).plannedRub)
        assertEquals(250L, progress.category(PlanCategory.WANTS).plannedRub)
        assertEquals(200L, progress.category(PlanCategory.SAVINGS).plannedRub)
        assertEquals(150L, progress.plan.reserveRub)
        assertEquals(PlanProgressTone.ON_TRACK, progress.category(PlanCategory.MANDATORY).tone)
        assertEquals(PlanProgressTone.WARNING, progress.category(PlanCategory.WANTS).tone)
        assertEquals(PlanProgressTone.OVER_LIMIT, progress.category(PlanCategory.SAVINGS).tone)
    }

    @Test fun planPercentagesMayLeaveAReserveButCannotExceedOneHundred() {
        assertEquals(10, PlanPercentages(50, 20, 20).reserve)
        assertThrows(IllegalArgumentException::class.java) { PlanPercentages(50, 40, 20) }
        assertThrows(IllegalArgumentException::class.java) { PlanPercentages(50, 60, -10) }
    }

    @Test fun assessmentExplainsTheFirstUnsafePartOfThePlan() {
        val config = PlanningConfig(seedDemoProgress = false)

        assertEquals(
            PlanAssessment.NeedsChanges(PlanAdjustmentReason.MANDATORY_TOO_LOW, 40),
            PlanningCalculator.assess(PlanPercentages(30, 30, 20), config),
        )
        assertEquals(
            PlanAssessment.NeedsChanges(PlanAdjustmentReason.RESERVE_TOO_LOW, 10),
            PlanningCalculator.assess(PlanPercentages(50, 30, 20), config),
        )
        assertEquals(
            PlanAssessment.NeedsChanges(PlanAdjustmentReason.SAVINGS_TOO_LOW, 10),
            PlanningCalculator.assess(PlanPercentages(50, 20, 5), config),
        )
        assertEquals(
            PlanAssessment.Adequate,
            PlanningCalculator.assess(PlanPercentages.DEFAULT, config),
        )
    }
}
