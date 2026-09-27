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

    @Test fun underspendingWithinThePlanIsReportedAsOnTrack() {
        val plan = WeeklyPlan(weekNumber = 2, availableRub = 500, percentages = PlanPercentages.DEFAULT)
        val progress = PlanningCalculator.progress(plan, mapOf(
            PlanCategory.MANDATORY to 0,
            PlanCategory.WANTS to 0,
            PlanCategory.SAVINGS to 0,
        ))

        PlanCategory.entries.forEach { category ->
            assertEquals(PlanProgressTone.ON_TRACK, progress.category(category).tone)
        }
    }

    @Test fun exactSpendingMatchesAreReportedAsFollowingThePlan() {
        val plan = WeeklyPlan(weekNumber = 2, availableRub = 500, percentages = PlanPercentages.DEFAULT)
        val progress = PlanningCalculator.progress(plan, PlanCategory.entries.associateWith(plan::plannedRub))

        PlanCategory.entries.forEach { category ->
            assertEquals(PlanProgressTone.ON_TRACK, progress.category(category).tone)
        }
    }

    @Test fun planPercentagesMayLeaveAReserveButCannotExceedOneHundred() {
        assertEquals(10, PlanPercentages(50, 20, 20).reserve)
        assertThrows(IllegalArgumentException::class.java) { PlanPercentages(50, 40, 20) }
        assertThrows(IllegalArgumentException::class.java) { PlanPercentages(50, 60, -10) }
    }

    @Test fun assessmentExplainsTheFirstUnsafePartOfThePlan() {
        val config = PlanningConfig()
        assertEquals(ReserveLevel.ALMOST_NONE, config.reserveLevel(49))
        assertEquals(ReserveLevel.SMALL, config.reserveLevel(50))
        assertEquals(ReserveLevel.ADEQUATE, config.reserveLevel(100))
        assertEquals(ReserveLevel.STRONG, config.reserveLevel(150))
        fun plan(
            availableRub: Long,
            mandatory: Int,
            wants: Int,
            savings: Int,
            knownExpenseRub: Long = 0,
            hasGoal: Boolean = false,
        ) = WeeklyPlan(
            1, availableRub, PlanPercentages(mandatory, wants, savings),
            PlanWeekContext(knownExpenseRub, hasGoal),
        )

        assertEquals(
            PlanAssessment.NeedsChanges(PlanAdjustmentReason.MANDATORY_TOO_LOW, 400),
            PlanningCalculator.assess(plan(900, 30, 30, 20), config),
        )
        assertEquals(
            PlanAssessment.NeedsChanges(PlanAdjustmentReason.RESERVE_TOO_LOW, 100),
            PlanningCalculator.assess(plan(900, 50, 30, 20), config),
        )
        assertEquals(
            PlanAssessment.Adequate,
            PlanningCalculator.assess(plan(900, 45, 20, 0), config),
        )
        assertEquals(
            PlanAssessment.Adequate,
            PlanningCalculator.assess(plan(900, 45, 20, 20, hasGoal = true), config),
        )
        assertEquals(
            PlanAssessment.NeedsChanges(PlanAdjustmentReason.MANDATORY_TOO_LOW, 480),
            PlanningCalculator.assess(plan(900, 45, 20, 20, knownExpenseRub = 80), config),
        )
        assertEquals(
            PlanAssessment.NeedsChanges(PlanAdjustmentReason.SAVINGS_TOO_LOW, 1),
            PlanningCalculator.assess(plan(900, 50, 20, 0, hasGoal = true), config),
        )
        assertEquals(
            PlanAssessment.Adequate,
            PlanningCalculator.assess(plan(2_200, 20, 70, 5), config),
        )
    }
}
