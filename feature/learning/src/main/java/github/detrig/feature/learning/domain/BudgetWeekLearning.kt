package github.detrig.feature.learning.domain

import kotlin.math.abs
import kotlin.math.max

/** The week feature reports committed figures; Learning decides what they teach. */
object BudgetWeekLearning {
    val WEEK_REVIEWED = LearningActionType("budget.week_reviewed")
    val WEEK_FOLLOWED = LearningActionType("budget.week_followed")
    val CHANGE_SEEN = LearningActionType("budget.change_seen")
    val CHANGE_ADAPTED = LearningActionType("budget.change_adapted")

    fun rules(): List<MetricRuleDefinition> = listOf(
        MetricRuleDefinition(
            metricId = LearningMetricIds.BUDGET_FOLLOW_PLAN,
            actionTypes = setOf(WEEK_REVIEWED, WEEK_FOLLOWED),
            milestones = listOf(
                ProgressMilestone(progressSteps = 1, requiredActions = 1),
                ProgressMilestone(
                    progressSteps = 2,
                    requiredActions = 3,
                    requiredDistinctPeriods = 3,
                    qualifyingActionTypes = setOf(WEEK_FOLLOWED),
                ),
            ),
        ),
        MetricRuleDefinition(
            metricId = LearningMetricIds.BUDGET_ADAPT_TO_CHANGE,
            actionTypes = setOf(CHANGE_SEEN, CHANGE_ADAPTED),
            milestones = listOf(
                ProgressMilestone(progressSteps = 1, requiredActions = 1),
                ProgressMilestone(
                    progressSteps = 2,
                    requiredActions = 2,
                    requiredDistinctPeriods = 2,
                    qualifyingActionTypes = setOf(CHANGE_ADAPTED),
                ),
            ),
        ),
    )

    fun actionsForCompletedWeek(
        profileId: String,
        weekNumber: Long,
        snapshot: BudgetWeekSnapshot,
        earlyFinish: Boolean = false,
        config: BudgetWeekAssessmentConfig = BudgetWeekAssessmentConfig(),
    ): List<LearningAction> {
        val controlledCostChanged = snapshot.meaningfulMandatoryOverrun(config) > 0
        val eventChanged = snapshot.unexpectedMandatoryRub > 0
        val changed = !earlyFinish && (controlledCostChanged || eventChanged)
        val adapted = changed && snapshot.planAdequate && snapshot.adaptedToMandatoryOverrun() &&
            (controlledCostChanged || snapshot.actualWantsRub < snapshot.plannedWantsRub)
        val followed = !earlyFinish && snapshot.planAdequate &&
            (snapshot.closeToPlan(config) || adapted)
        return buildList {
            add(action(
                profileId, weekNumber, "follow",
                if (followed) WEEK_FOLLOWED else WEEK_REVIEWED,
                snapshot,
            ))
            if (changed) {
                add(action(
                    profileId, weekNumber, "adapt",
                    if (adapted) CHANGE_ADAPTED else CHANGE_SEEN,
                    snapshot,
                ))
            }
        }
    }

    private fun action(
        profileId: String,
        weekNumber: Long,
        suffix: String,
        type: LearningActionType,
        snapshot: BudgetWeekSnapshot,
    ) = LearningAction(
        actionId = "budget-week:$weekNumber:$suffix",
        profileId = profileId,
        gamePeriod = weekNumber,
        type = type,
        context = snapshot,
        sourceOperationId = "week-completed:$weekNumber",
    )
}

data class BudgetWeekAssessmentConfig(
    val minimumChangeRub: Long = 10,
    val maximumCloseDeviationPercent: Int = 25,
) {
    init {
        require(minimumChangeRub > 0)
        require(maximumCloseDeviationPercent in 0..100)
    }
}

data class BudgetWeekSnapshot(
    val planAdequate: Boolean,
    val availableRub: Long,
    val plannedMandatoryRub: Long,
    val plannedWantsRub: Long,
    val plannedSavingsRub: Long,
    val plannedReserveRub: Long,
    val actualMandatoryRub: Long,
    val actualWantsRub: Long,
    val actualSavingsRub: Long,
    val unexpectedMandatoryRub: Long = 0,
) : LearningActionContext {
    init {
        require(availableRub >= 0)
        require(plannedMandatoryRub >= 0 && plannedWantsRub >= 0 && plannedSavingsRub >= 0 && plannedReserveRub >= 0)
        require(actualMandatoryRub >= 0 && actualWantsRub >= 0)
        require(unexpectedMandatoryRub >= 0)
        require(plannedMandatoryRub + plannedWantsRub + plannedSavingsRub + plannedReserveRub == availableRub)
    }

    private val actualReserveRub: Long
        get() = (availableRub - actualMandatoryRub - actualWantsRub - actualSavingsRub).coerceAtLeast(0)

    override val fingerprint: String = listOf(
        planAdequate, availableRub, plannedMandatoryRub, plannedWantsRub,
        plannedSavingsRub, plannedReserveRub, actualMandatoryRub, actualWantsRub,
        actualSavingsRub, unexpectedMandatoryRub,
    ).joinToString(";")

    internal fun meaningfulMandatoryOverrun(config: BudgetWeekAssessmentConfig): Long {
        val overrun = actualMandatoryRub - plannedMandatoryRub
        val threshold = max(config.minimumChangeRub, plannedMandatoryRub * config.maximumCloseDeviationPercent / 100)
        return overrun.takeIf { it > threshold } ?: 0
    }

    internal fun adaptedToMandatoryOverrun(): Boolean =
        actualWantsRub <= plannedWantsRub &&
            actualMandatoryRub + actualWantsRub + actualSavingsRub + unexpectedMandatoryRub <= availableRub

    internal fun closeToPlan(config: BudgetWeekAssessmentConfig): Boolean =
        close(plannedMandatoryRub, actualMandatoryRub, config) &&
            close(plannedWantsRub, actualWantsRub, config) &&
            close(plannedSavingsRub, actualSavingsRub, config) &&
            close(plannedReserveRub, actualReserveRub, config)

    private fun close(planned: Long, actual: Long, config: BudgetWeekAssessmentConfig): Boolean =
        abs(actual - planned) <= max(config.minimumChangeRub, planned * config.maximumCloseDeviationPercent / 100)
}
