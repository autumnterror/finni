package github.detrig.feature.planning.domain

internal object PlanningCalculator {
    private const val WARNING_MULTIPLIER = 1.25

    fun assess(plan: WeeklyPlan, config: PlanningConfig): PlanAssessment {
        val requiredMandatoryRub = Math.addExact(
            config.minimumFoodBudgetRub,
            plan.context.knownMandatoryExpenseRub,
        )
        return when {
            plan.plannedRub(PlanCategory.MANDATORY) < requiredMandatoryRub -> PlanAssessment.NeedsChanges(
                reason = PlanAdjustmentReason.MANDATORY_TOO_LOW,
                requiredRub = requiredMandatoryRub,
            )
            config.reserveLevel(plan.reserveRub) < ReserveLevel.ADEQUATE -> PlanAssessment.NeedsChanges(
                reason = PlanAdjustmentReason.RESERVE_TOO_LOW,
                requiredRub = config.minimumReserveRub,
            )
            plan.context.hasActiveGoal && plan.plannedRub(PlanCategory.SAVINGS) == 0L -> PlanAssessment.NeedsChanges(
                reason = PlanAdjustmentReason.SAVINGS_TOO_LOW,
                requiredRub = 1,
            )
            else -> PlanAssessment.Adequate
        }
    }

    fun progress(
        plan: WeeklyPlan,
        actuals: Map<PlanCategory, Long>,
        config: PlanningConfig = PlanningConfig(),
    ): WeeklyPlanProgress {
        val planned = mapOf(
            PlanCategory.MANDATORY to plan.plannedRub(PlanCategory.MANDATORY),
            PlanCategory.WANTS to plan.plannedRub(PlanCategory.WANTS),
            PlanCategory.SAVINGS to plan.plannedRub(PlanCategory.SAVINGS),
        )
        return WeeklyPlanProgress(plan, PlanCategory.entries.map { category ->
            val expected = checkNotNull(planned[category])
            val actual = actuals[category] ?: 0L
            CategoryPlanProgress(category, expected, actual, tone(expected, actual))
        }, planAssessment = assess(plan, config))
    }

    private fun tone(plannedRub: Long, actualRub: Long): PlanProgressTone = when {
        actualRub <= plannedRub -> PlanProgressTone.ON_TRACK
        plannedRub == 0L || actualRub > (plannedRub * WARNING_MULTIPLIER).toLong() -> PlanProgressTone.OVER_LIMIT
        else -> PlanProgressTone.WARNING
    }
}
