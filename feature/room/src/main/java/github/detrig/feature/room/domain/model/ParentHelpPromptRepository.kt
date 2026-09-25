package github.detrig.feature.room.domain.model

internal interface ParentHelpPromptRepository {
    fun wasShownInWeek(weekNumber: Long): Boolean
    fun markShownInWeek(weekNumber: Long)
}
