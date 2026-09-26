package github.detrig.feature.room.presentation

import github.detrig.feature.planning.domain.PlanCategory
import github.detrig.feature.planning.domain.PlanAssessment
import github.detrig.feature.planning.domain.WeeklyPlanProgress
import github.detrig.feature.planning.domain.isGoodWeeklyResult

internal enum class WeekPlanItem {
    MANDATORY,
    WANTS,
    SAVINGS,
    RESERVE,
}

internal enum class WeekPlanOutcome {
    ALL_MATCHED,
    PARTIALLY_MATCHED,
    TRY_AGAIN,
}

internal enum class WeekPlanFeedbackReason {
    WEAK_PLAN,
    UNEXPECTED_EXPENSE_COVERED,
    UNEXPECTED_EXPENSE_BEYOND_RESERVE,
    ADAPTED_TO_MANDATORY_COST,
    CLOSE_TO_PLAN,
    MANDATORY_COST_INCREASED,
    EXTRA_INCOME_ALLOCATED,
    ORDINARY,
}

internal data class WeekPlanAssessment(
    val matchedItems: Set<WeekPlanItem>,
    val missedItems: Set<WeekPlanItem>,
    val actualReserveRub: Long,
    val remainingReserveRub: Long,
    val uncoveredEventRub: Long,
    val feedbackReason: WeekPlanFeedbackReason,
) {
    val outcome: WeekPlanOutcome = when (matchedItems.size) {
        WeekPlanItem.entries.size -> WeekPlanOutcome.ALL_MATCHED
        0 -> WeekPlanOutcome.TRY_AGAIN
        else -> WeekPlanOutcome.PARTIALLY_MATCHED
    }

    fun matches(category: PlanCategory): Boolean = when (category) {
        PlanCategory.MANDATORY -> WeekPlanItem.MANDATORY
        PlanCategory.WANTS -> WeekPlanItem.WANTS
        PlanCategory.SAVINGS -> WeekPlanItem.SAVINGS
    } in matchedItems
}

internal fun WeeklyPlanProgress.assessWeek(): WeekPlanAssessment {
    val categorizedActualRub = categories.sumOf { it.actualRub }
    val actualReserveRub = controlledReserveRub
    val mandatory = category(PlanCategory.MANDATORY)
    val wants = category(PlanCategory.WANTS)
    val mandatoryIncreased = mandatory.actualRub > mandatory.plannedRub
    val matched = buildSet {
        if (category(PlanCategory.MANDATORY).let { it.actualRub == it.plannedRub }) {
            add(WeekPlanItem.MANDATORY)
        }
        if (wants.actualRub == wants.plannedRub + extraWantsRub) {
            add(WeekPlanItem.WANTS)
        }
        if (category(PlanCategory.SAVINGS).let { it.actualRub == it.plannedRub + extraSavingsRub }) {
            add(WeekPlanItem.SAVINGS)
        }
        if (categorizedActualRub <= plan.availableRub + extraIncomeRub &&
            actualReserveRub == plan.reserveRub + extraReserveRub) {
            add(WeekPlanItem.RESERVE)
        }
    }
    val feedbackReason = when {
        planAssessment != PlanAssessment.Adequate -> WeekPlanFeedbackReason.WEAK_PLAN
        extraIncomeRub > 0 && isGoodWeeklyResult() && unexpectedMandatoryRub == 0L ->
            WeekPlanFeedbackReason.EXTRA_INCOME_ALLOCATED
        unexpectedMandatoryRub > 0 && isGoodWeeklyResult() ->
            if (unexpectedMandatoryRub <= actualReserveRub) {
                WeekPlanFeedbackReason.UNEXPECTED_EXPENSE_COVERED
            } else {
                WeekPlanFeedbackReason.UNEXPECTED_EXPENSE_BEYOND_RESERVE
            }
        mandatoryIncreased && wants.actualRub <= wants.plannedRub &&
            categorizedActualRub <= plan.availableRub -> WeekPlanFeedbackReason.ADAPTED_TO_MANDATORY_COST
        matched.size < WeekPlanItem.entries.size && isGoodWeeklyResult() -> WeekPlanFeedbackReason.CLOSE_TO_PLAN
        mandatoryIncreased -> WeekPlanFeedbackReason.MANDATORY_COST_INCREASED
        else -> WeekPlanFeedbackReason.ORDINARY
    }
    return WeekPlanAssessment(
        matchedItems = matched,
        missedItems = WeekPlanItem.entries.toSet() - matched,
        actualReserveRub = actualReserveRub,
        remainingReserveRub = remainingReserveRub,
        uncoveredEventRub = (unexpectedMandatoryRub - actualReserveRub).coerceAtLeast(0),
        feedbackReason = feedbackReason,
    )
}
