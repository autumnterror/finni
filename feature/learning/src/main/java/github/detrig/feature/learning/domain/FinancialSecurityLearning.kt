package github.detrig.feature.learning.domain

object FinancialSecurityLearning {

    val CONFIRMATION_CODE_SAFE_RESPONSE = LearningActionType(
        "security.confirmation_code.safe_response",
    )
    val UNKNOWN_LINK_SAFE_RESPONSE = LearningActionType(
        "security.unknown_link.safe_response",
    )

    fun rules(): List<MetricRuleDefinition> = listOf(
        securityRule(
            metricId = LearningMetricIds.SECURITY_CONFIRMATION_CODE,
            actionType = CONFIRMATION_CODE_SAFE_RESPONSE,
        ),
        securityRule(
            metricId = LearningMetricIds.SECURITY_UNKNOWN_LINK,
            actionType = UNKNOWN_LINK_SAFE_RESPONSE,
        ),
    )

    fun safeResponseAction(
        actionId: String,
        profileId: String,
        absoluteDay: Long,
        eventId: String,
        scenario: SecurityScenario,
        sourceOperationId: String = eventId,
    ): LearningAction = LearningAction(
        actionId = actionId,
        profileId = profileId,
        gamePeriod = absoluteDay,
        type = when (scenario) {
            SecurityScenario.CONFIRMATION_CODE -> CONFIRMATION_CODE_SAFE_RESPONSE
            SecurityScenario.UNKNOWN_LINK -> UNKNOWN_LINK_SAFE_RESPONSE
        },
        context = SecurityResponseContext(
            eventId = eventId,
            scenario = scenario,
        ),
        sourceOperationId = sourceOperationId,
    )

    private fun securityRule(
        metricId: String,
        actionType: LearningActionType,
    ) = MetricRuleDefinition(
        metricId = metricId,
        actionTypes = setOf(actionType),
        milestones = listOf(
            ProgressMilestone(
                progressSteps = 1,
                requiredActions = 1,
            ),
            ProgressMilestone(
                progressSteps = 3,
                requiredActions = 3,
                requiredDistinctPeriods = 3,
            ),
        ),
    )
}

enum class SecurityScenario {
    CONFIRMATION_CODE,
    UNKNOWN_LINK,
}

data class SecurityResponseContext(
    val eventId: String,
    val scenario: SecurityScenario,
) : LearningActionContext {
    init {
        require(eventId.isNotBlank())
    }

    override val fingerprint: String = "event=$eventId|scenario=${scenario.name}|safe=true"
}
