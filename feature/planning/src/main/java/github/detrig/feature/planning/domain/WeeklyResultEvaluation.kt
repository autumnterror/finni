package github.detrig.feature.planning.domain

import kotlin.math.abs
import kotlin.math.max

/** Нейтральная проверка близости плана и факта без школьной оценки. */
fun WeeklyPlanProgress.isGoodWeeklyResult(): Boolean {
    val categoriesMatch = categories.all { category ->
        val toleranceRub = max(MINIMUM_TOLERANCE_RUB, category.plannedRub / 4)
        abs(category.actualRub - category.plannedRub) <= toleranceRub
    }
    val plannedReserve = plan.reserveRub
    val actualReserve = (plan.availableRub - categories.sumOf { it.actualRub }).coerceAtLeast(0)
    val reserveToleranceRub = max(MINIMUM_TOLERANCE_RUB, plannedReserve / 4)
    return categoriesMatch && abs(actualReserve - plannedReserve) <= reserveToleranceRub
}

private const val MINIMUM_TOLERANCE_RUB = 10L
