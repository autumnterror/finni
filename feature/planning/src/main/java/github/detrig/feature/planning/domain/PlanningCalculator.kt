package github.detrig.feature.planning.domain

internal object PlanningCalculator {
    private const val WARNING_MULTIPLIER = 1.25

    fun assess(percentages: PlanPercentages, config: PlanningConfig): PlanAssessment = when {
        percentages.mandatory < config.minimumMandatoryPercent -> PlanAssessment.NeedsChanges(
            reason = PlanAdjustmentReason.MANDATORY_TOO_LOW,
            recommendedPercent = config.minimumMandatoryPercent,
        )
        percentages.reserve < config.minimumReservePercent -> PlanAssessment.NeedsChanges(
            reason = PlanAdjustmentReason.RESERVE_TOO_LOW,
            recommendedPercent = config.minimumReservePercent,
        )
        else -> PlanAssessment.Adequate
    }

    fun progress(plan: WeeklyPlan, actuals: Map<PlanCategory, Long>): WeeklyPlanProgress {
        val planned = mapOf(
            PlanCategory.MANDATORY to plan.plannedRub(PlanCategory.MANDATORY),
            PlanCategory.WANTS to plan.plannedRub(PlanCategory.WANTS),
            PlanCategory.SAVINGS to plan.plannedRub(PlanCategory.SAVINGS),
        )
        return WeeklyPlanProgress(plan, PlanCategory.entries.map { category ->
            val expected = checkNotNull(planned[category])
            val actual = actuals[category] ?: 0L
            CategoryPlanProgress(category, expected, actual, tone(expected, actual))
        })
    }

    private fun tone(plannedRub: Long, actualRub: Long): PlanProgressTone = when {
        actualRub == plannedRub -> PlanProgressTone.ON_TRACK
        plannedRub == 0L || actualRub > (plannedRub * WARNING_MULTIPLIER).toLong() -> PlanProgressTone.OVER_LIMIT
        else -> PlanProgressTone.WARNING
    }
}
