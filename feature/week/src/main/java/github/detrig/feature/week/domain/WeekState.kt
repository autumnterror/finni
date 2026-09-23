package github.detrig.feature.week.domain

/** День 1 — понедельник первой недели. Игровое время не зависит от часов устройства. */
data class WeekState(val absoluteDay: Long) {
    init { require(absoluteDay >= 1) }

    val weekNumber: Long get() = (absoluteDay - 1) / DAYS_PER_WEEK + 1
    val dayOfWeek: Int get() = ((absoluteDay - 1) % DAYS_PER_WEEK + 1).toInt()
    val daysUntilAllowance: Int get() = (DAYS_PER_WEEK - dayOfWeek + 1).toInt()

    companion object { const val DAYS_PER_WEEK = 7L }
}

sealed interface EndDayResult {
    val state: WeekState

    data class Advanced(
        override val state: WeekState,
        val allowanceReceivedRub: Long,
        val allowanceGrossRub: Long = 0,
        val parentHelpRepaidRub: Long = 0,
    ) : EndDayResult
    data class AlreadyAdvanced(override val state: WeekState) : EndDayResult
}

sealed interface EarlyWeekEndResult {
    val state: WeekState

    data class Completed(
        override val state: WeekState,
        val skippedDays: Int,
        val parentHelpRub: Long,
        val allowanceReceivedRub: Long,
        val allowanceGrossRub: Long,
        val parentHelpRepaidRub: Long,
    ) : EarlyWeekEndResult

    data class AlreadyCompleted(override val state: WeekState) : EarlyWeekEndResult
    data class NotNeeded(override val state: WeekState) : EarlyWeekEndResult
}
