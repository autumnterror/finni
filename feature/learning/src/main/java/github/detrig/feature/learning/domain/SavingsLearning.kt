package github.detrig.feature.learning.domain

/** Facts reported by the savings and planning features after a committed action. */
object SavingsLearning {
    val GOAL_CREATED = LearningActionType("savings.goal_created")
    val SAVINGS_PLANNED = LearningActionType("savings.planned")
    val CONTRIBUTION = LearningActionType("savings.contribution")
    val GOAL_REACHED = LearningActionType("savings.goal_reached")

    fun rules(): List<MetricRuleDefinition> = listOf(
        rule(LearningMetricIds.SAVINGS_CREATE_GOAL, GOAL_CREATED, 2, 2),
        rule(LearningMetricIds.SAVINGS_PLAN, SAVINGS_PLANNED, 3, 3),
        rule(LearningMetricIds.SAVINGS_REGULAR_CONTRIBUTION, CONTRIBUTION, 3, 2),
        rule(LearningMetricIds.SAVINGS_REACH_GOAL, GOAL_REACHED, 2, 2),
    )

    private fun rule(
        metricId: String,
        type: LearningActionType,
        learnedActions: Int,
        learnedPeriods: Int,
    ) = MetricRuleDefinition(
        metricId = metricId,
        actionTypes = setOf(type),
        milestones = listOf(
            ProgressMilestone(progressSteps = 1, requiredActions = 1),
            ProgressMilestone(
                progressSteps = 2,
                requiredActions = learnedActions,
                requiredDistinctPeriods = learnedPeriods,
            ),
        ),
    )

    fun goalCreated(profileId: String, goalId: String, week: Long) =
        action("goal:$goalId", profileId, week, GOAL_CREATED, "goal=$goalId")

    fun savingsPlanned(profileId: String, week: Long, amountRub: Long) =
        action("plan:$week", profileId, week, SAVINGS_PLANNED, "amount=$amountRub")

    fun contribution(profileId: String, operationId: String, goalId: String, amountRub: Long, week: Long) =
        action("contribution:$operationId", profileId, week, CONTRIBUTION, "goal=$goalId;amount=$amountRub")

    fun goalReached(profileId: String, goalId: String, week: Long) =
        action("reached:$goalId", profileId, week, GOAL_REACHED, "goal=$goalId")

    private fun action(
        id: String,
        profileId: String,
        week: Long,
        type: LearningActionType,
        fingerprint: String,
    ) = LearningAction(
        actionId = "savings:$id",
        profileId = profileId,
        gamePeriod = week,
        type = type,
        context = OpaqueLearningActionContext(fingerprint),
        sourceOperationId = id,
    )
}
