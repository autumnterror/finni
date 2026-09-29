package github.detrig.feature.room.data.local

import android.content.SharedPreferences
import github.detrig.core.infrastructure.preferences.SharedStorage
import github.detrig.feature.room.domain.model.PetWashGuidePromptRepository

internal class PetWashGuidePromptStorage(
    private val preferences: SharedPreferences,
) : SharedStorage(preferences), PetWashGuidePromptRepository {

    override fun wasShownForCurrentDirtEpisode(): Boolean = readBoolean(SHOWN_KEY, false)

    override fun tryMarkShownForCurrentDirtEpisode(): Boolean = synchronized(preferences) {
        if (wasShownForCurrentDirtEpisode()) {
            false
        } else {
            putBoolean(SHOWN_KEY, true)
            true
        }
    }

    override fun wasBathGuidanceCompleted(): Boolean =
        readBoolean(BATH_GUIDANCE_COMPLETED_KEY, false)

    override fun markBathGuidanceCompleted() = synchronized(preferences) {
        putBoolean(BATH_GUIDANCE_COMPLETED_KEY, true)
    }

    override fun resetForCleanPet() = synchronized(preferences) {
        remove(SHOWN_KEY)
    }

    override fun resetAll() = synchronized(preferences) {
        remove(SHOWN_KEY)
        remove(BATH_GUIDANCE_COMPLETED_KEY)
    }

    private companion object {
        const val SHOWN_KEY = "pet_wash_guide_shown_for_dirty_episode_v1"
        const val BATH_GUIDANCE_COMPLETED_KEY = "pet_wash_bath_guidance_completed_v1"
    }
}
