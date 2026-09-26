package github.detrig.feature.planning.domain

import kotlin.math.abs
import kotlin.math.max

/** Нейтральная проверка близости плана и факта без школьной оценки. */
fun WeeklyPlanProgress.isGoodWeeklyResult(): Boolean {
    val categoriesMatch = categories.all { category ->
        val expectedRub = category.plannedRub + when (category.category) {
            PlanCategory.WANTS -> extraWantsRub
            PlanCategory.SAVINGS -> extraSavingsRub
            PlanCategory.MANDATORY -> 0L
        }
        val toleranceRub = max(MINIMUM_TOLERANCE_RUB, expectedRub / 4)
        abs(category.actualRub - expectedRub) <= toleranceRub
    }
    val plannedReserve = plan.reserveRub + extraReserveRub
    val actualReserve = controlledReserveRub
    val reserveToleranceRub = max(MINIMUM_TOLERANCE_RUB, plannedReserve / 4)
    return categoriesMatch && abs(actualReserve - plannedReserve) <= reserveToleranceRub
}

private const val MINIMUM_TOLERANCE_RUB = 10L
