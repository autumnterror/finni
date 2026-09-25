package github.detrig.feature.planning.domain

enum class PlanCategory(val code: String) {
    MANDATORY("mandatory"),
    WANTS("wants"),
    SAVINGS("savings"),
}

/** Classification of a payment before it is included in the weekly plan actuals. */
enum class PaymentClassification {
    MANDATORY,
    OPTIONAL,
}

/**
 * A committed financial operation that changes the actual result of a weekly plan.
 * Payments and savings contributions are intentionally separate: putting money into
 * the piggy bank is not an expense, but it still fulfils the savings part of the plan.
 */
sealed interface PlanActualOperation {
    val operationId: String
    val weekNumber: Long
    val amountRub: Long

    data class Payment(
        override val operationId: String,
        override val weekNumber: Long,
        override val amountRub: Long,
        val classification: PaymentClassification,
    ) : PlanActualOperation

    data class SavingsContribution(
        override val operationId: String,
        override val weekNumber: Long,
        override val amountRub: Long,
    ) : PlanActualOperation

    data class SavingsWithdrawal(
        override val operationId: String,
        override val weekNumber: Long,
        override val amountRub: Long,
    ) : PlanActualOperation

    val signedAmountRub: Long
        get() = when (this) {
            is SavingsWithdrawal -> -amountRub
            is Payment, is SavingsContribution -> amountRub
        }

    val planCategory: PlanCategory
        get() = when (this) {
            is Payment -> when (classification) {
                PaymentClassification.MANDATORY -> PlanCategory.MANDATORY
                PaymentClassification.OPTIONAL -> PlanCategory.WANTS
            }
            is SavingsContribution, is SavingsWithdrawal -> PlanCategory.SAVINGS
        }
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
