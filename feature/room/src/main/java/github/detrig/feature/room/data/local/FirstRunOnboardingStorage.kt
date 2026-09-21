package github.detrig.feature.room.data.local

import android.content.SharedPreferences
import github.detrig.core.infrastructure.preferences.SharedStorage
import github.detrig.feature.room.domain.model.FirstRunOnboardingRepository
import github.detrig.feature.room.domain.model.FirstRunOnboardingStep

internal class FirstRunOnboardingStorage(preferences: SharedPreferences) :
    SharedStorage(preferences), FirstRunOnboardingRepository {

    override fun load(): FirstRunOnboardingStep {
        val stored = readString(STEP_KEY, FirstRunOnboardingStep.INTRODUCTION.name)
        return FirstRunOnboardingStep.entries.firstOrNull { it.name == stored }
            ?: FirstRunOnboardingStep.INTRODUCTION
    }

    override fun save(step: FirstRunOnboardingStep) {
        putString(STEP_KEY, step.name)
    }

    override fun loadSuggestedGoalZoneId(): String? =
        readString(SUGGESTED_GOAL_ZONE_KEY, null)

    override fun saveSuggestedGoalZoneId(zoneId: String?) {
        if (zoneId == null) {
            remove(SUGGESTED_GOAL_ZONE_KEY)
        } else {
            putString(SUGGESTED_GOAL_ZONE_KEY, zoneId)
        }
    }

    private companion object {
        const val STEP_KEY = "first_run_onboarding_step_v1"
        const val SUGGESTED_GOAL_ZONE_KEY = "first_run_onboarding_suggested_goal_zone_v1"
    }
}
