package github.detrig.feature.room.data.local

import android.content.SharedPreferences
import github.detrig.core.infrastructure.preferences.SharedStorage
import github.detrig.feature.room.domain.model.ParentHelpPromptRepository

internal class ParentHelpPromptStorage(private val preferences: SharedPreferences) :
    SharedStorage(preferences), ParentHelpPromptRepository {

    override fun wasShownInWeek(weekNumber: Long): Boolean =
        readLong(LAST_SHOWN_WEEK_KEY, 0) == weekNumber

    override fun tryMarkShownInWeek(weekNumber: Long): Boolean = synchronized(preferences) {
        if (wasShownInWeek(weekNumber)) {
            false
        } else {
            putLong(LAST_SHOWN_WEEK_KEY, weekNumber)
            true
        }
    }

    override fun resetAfterParentHelpSettlement() = synchronized(preferences) {
        remove(LAST_SHOWN_WEEK_KEY)
    }

    private companion object {
        // Earlier versions could mark the prompt before the modal became visible.
        const val LAST_SHOWN_WEEK_KEY = "parent_help_automatic_prompt_shown_week_v3"
    }
}
