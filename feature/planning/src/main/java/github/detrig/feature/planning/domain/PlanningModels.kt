package github.detrig.feature.planning.domain

enum class PlanCategory(val code: String) {
    MANDATORY("mandatory"),
    WANTS("wants"),
    SAVINGS("savings"),
}

data class PlanPercentages(
    val mandatory: Int,
    val wants: Int,
    val savings: Int,
) {
    init {
        require(mandatory >= 0 && wants >= 0 && savings >= 0)
        require(total <= TOTAL_PERCENT) { "The plan must not exceed 100%" }
    }

    val total: Int get() = mandatory + wants + savings
    val reserve: Int get() = TOTAL_PERCENT - total

    fun percent(category: PlanCategory): Int = when (category) {
        PlanCategory.MANDATORY -> mandatory
        PlanCategory.WANTS -> wants
        PlanCategory.SAVINGS -> savings
    }

    companion object {
        const val TOTAL_PERCENT = 100
        val DEFAULT = PlanPercentages(mandatory = 40, wants = 25, savings = 20)
    }
}

enum class PlanAdjustmentReason {
    MANDATORY_TOO_LOW,
    RESERVE_TOO_LOW,
    SAVINGS_TOO_LOW,
}

sealed interface PlanAssessment {
    data object Adequate : PlanAssessment

    data class NeedsChanges(
        val reason: PlanAdjustmentReason,
        val recommendedPercent: Int,
    ) : PlanAssessment
}

data class WeeklyPlan(
    val weekNumber: Long,
    val availableRub: Long,
    val percentages: PlanPercentages,
) {
    val reserveRub: Long
        get() = availableRub - PlanCategory.entries.sumOf { category -> plannedRub(category) }

    fun plannedRub(category: PlanCategory): Long =
        (availableRub * percentages.percent(category)) / PlanPercentages.TOTAL_PERCENT
}

enum class PlanProgressTone { ON_TRACK, WARNING, OVER_LIMIT }

data class CategoryPlanProgress(
    val category: PlanCategory,
    val plannedRub: Long,
    val actualRub: Long,
    val tone: PlanProgressTone,
) {
    val progress: Float get() = if (plannedRub == 0L) 0f else actualRub.toFloat() / plannedRub
    val actualPercent: Int get() = if (plannedRub == 0L) 0 else ((actualRub * 100) / plannedRub).toInt()
}

data class WeeklyPlanProgress(
    val plan: WeeklyPlan,
    val categories: List<CategoryPlanProgress>,
) {
    fun category(category: PlanCategory): CategoryPlanProgress = categories.first { it.category == category }
}

sealed interface SavePlanResult {
    data class Saved(val progress: WeeklyPlanProgress) : SavePlanResult
    data class AlreadySaved(val progress: WeeklyPlanProgress) : SavePlanResult
}

sealed interface RecordActualResult {
    data object Recorded : RecordActualResult
    data object AlreadyRecorded : RecordActualResult
    data object OperationIdConflict : RecordActualResult
}
