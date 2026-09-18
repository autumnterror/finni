package github.detrig.feature.planning.domain

internal object PlanningCalculator {
    private const val WARNING_MULTIPLIER = 1.25

    fun progress(plan: WeeklyPlan, actuals: Map<PlanCategory, Long>): WeeklyPlanProgress {
        val mandatory = (plan.availableRub * plan.percentages.mandatory) / PlanPercentages.TOTAL_PERCENT
        val wants = (plan.availableRub * plan.percentages.wants) / PlanPercentages.TOTAL_PERCENT
        val savings = plan.availableRub - mandatory - wants
        val planned = mapOf(
            PlanCategory.MANDATORY to mandatory,
            PlanCategory.WANTS to wants,
            PlanCategory.SAVINGS to savings,
        )
        return WeeklyPlanProgress(plan, PlanCategory.entries.map { category ->
            val expected = checkNotNull(planned[category])
            val actual = actuals[category] ?: 0L
            CategoryPlanProgress(category, expected, actual, tone(expected, actual))
        })
    }

    private fun tone(plannedRub: Long, actualRub: Long): PlanProgressTone = when {
        actualRub <= plannedRub -> PlanProgressTone.ON_TRACK
        plannedRub == 0L || actualRub > (plannedRub * WARNING_MULTIPLIER).toLong() -> PlanProgressTone.OVER_LIMIT
        else -> PlanProgressTone.WARNING
    }
}
