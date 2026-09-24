package github.detrig.feature.room.presentation

import github.detrig.feature.planning.domain.PlanCategory
import github.detrig.feature.planning.domain.WeeklyPlanProgress

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

internal data class WeekPlanAssessment(
    val matchedItems: Set<WeekPlanItem>,
    val missedItems: Set<WeekPlanItem>,
    val actualReserveRub: Long,
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
    val actualReserveRub = (plan.availableRub - categories.sumOf { it.actualRub }).coerceAtLeast(0)
    val matched = buildSet {
        if (category(PlanCategory.MANDATORY).let { it.actualRub <= it.plannedRub }) {
            add(WeekPlanItem.MANDATORY)
        }
        if (category(PlanCategory.WANTS).let { it.actualRub <= it.plannedRub }) {
            add(WeekPlanItem.WANTS)
        }
        if (category(PlanCategory.SAVINGS).let { it.actualRub >= it.plannedRub }) {
            add(WeekPlanItem.SAVINGS)
        }
        if (actualReserveRub >= plan.reserveRub) add(WeekPlanItem.RESERVE)
    }
    return WeekPlanAssessment(
        matchedItems = matched,
        missedItems = WeekPlanItem.entries.toSet() - matched,
        actualReserveRub = actualReserveRub,
    )
}
