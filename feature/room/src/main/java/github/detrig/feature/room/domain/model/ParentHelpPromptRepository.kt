package github.detrig.feature.room.domain.model

internal interface ParentHelpPromptRepository {
    fun wasShownInWeek(weekNumber: Long): Boolean
    fun tryMarkShownInWeek(weekNumber: Long): Boolean
    fun resetAfterParentHelpSettlement()
}
