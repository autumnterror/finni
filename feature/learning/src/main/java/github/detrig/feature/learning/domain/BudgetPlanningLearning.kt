package github.detrig.feature.learning.domain

object BudgetPlanningLearning {
    val PLAN_CONFIRMED = LearningActionType("budget.plan_confirmed")

    fun reasonablePlanRule(): MetricRuleDefinition = MetricRuleDefinition(
        metricId = LearningMetricIds.BUDGET_REASONABLE_PLAN,
        actionTypes = setOf(PLAN_CONFIRMED),
        milestones = listOf(
            ProgressMilestone(
                progressSteps = 1,
                requiredActions = 1,
            ),
            ProgressMilestone(
                progressSteps = 2,
                requiredActions = 3,
                requiredDistinctPeriods = 3,
            ),
        ),
    )

    fun confirmedPlanAction(
        profileId: String,
        weekNumber: Long,
        mandatoryPercent: Int,
        wantsPercent: Int,
        savingsPercent: Int,
        reservePercent: Int,
    ): LearningAction {
        val context = BudgetPlanConfirmedContext(
            mandatoryPercent = mandatoryPercent,
            wantsPercent = wantsPercent,
            savingsPercent = savingsPercent,
            reservePercent = reservePercent,
        )
        return LearningAction(
            actionId = "budget-plan:$weekNumber:confirmed",
            profileId = profileId,
            gamePeriod = weekNumber,
            type = PLAN_CONFIRMED,
            context = context,
            sourceOperationId = "weekly-plan:$weekNumber",
        )
    }
}

data class BudgetPlanConfirmedContext(
    val mandatoryPercent: Int,
    val wantsPercent: Int,
    val savingsPercent: Int,
    val reservePercent: Int,
) : LearningActionContext {
    init {
        require(mandatoryPercent >= 0)
        require(wantsPercent >= 0)
        require(savingsPercent >= 0)
        require(reservePercent >= 0)
        require(mandatoryPercent + wantsPercent + savingsPercent + reservePercent == 100)
    }

    override val fingerprint: String =
        "mandatory=$mandatoryPercent;wants=$wantsPercent;savings=$savingsPercent;reserve=$reservePercent"
}
