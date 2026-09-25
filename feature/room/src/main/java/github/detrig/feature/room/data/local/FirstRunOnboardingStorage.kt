package github.detrig.feature.room.data.local

import android.content.SharedPreferences
import github.detrig.core.infrastructure.preferences.SharedStorage
import github.detrig.feature.room.domain.model.FirstRunOnboardingChapter
import github.detrig.feature.room.domain.model.FirstRunOnboardingProgress
import github.detrig.feature.room.domain.model.FirstRunOnboardingRepository
import github.detrig.feature.room.domain.model.FirstRunOnboardingStep
import github.detrig.feature.room.domain.model.completedChaptersForMigration

internal class FirstRunOnboardingStorage(preferences: SharedPreferences) :
    SharedStorage(preferences), FirstRunOnboardingRepository {

    override fun load(): FirstRunOnboardingProgress {
        val completed = if (hasKey(COMPLETED_CHAPTERS_KEY)) {
            readStringSet(COMPLETED_CHAPTERS_KEY)
                .mapNotNullTo(linkedSetOf()) { stored ->
                    FirstRunOnboardingChapter.entries.firstOrNull { it.name == stored }
                }
        } else {
            migrateLegacyProgress()
        }
        return FirstRunOnboardingProgress(completed)
    }

    override fun markChapterCompleted(
        chapter: FirstRunOnboardingChapter,
    ): FirstRunOnboardingProgress {
        val updated = load().complete(chapter)
        putStringSet(COMPLETED_CHAPTERS_KEY, updated.completedChapters.mapTo(linkedSetOf()) { it.name })
        return updated
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

    private fun migrateLegacyProgress(): Set<FirstRunOnboardingChapter> {
        val storedStep = readString(LEGACY_STEP_KEY, null)
        if (storedStep == REMOVED_PLAN_SAVED_STEP) {
            return setOf(
                FirstRunOnboardingChapter.INTRODUCTION_AND_FIRST_MONEY,
                FirstRunOnboardingChapter.BUDGET_PLANNING,
            ).also { completed ->
                putStringSet(COMPLETED_CHAPTERS_KEY, completed.mapTo(linkedSetOf()) { it.name })
            }
        }
        val legacyStep = storedStep
            ?.let { stored -> FirstRunOnboardingStep.entries.firstOrNull { it.name == stored } }
            ?: FirstRunOnboardingStep.INTRODUCTION
        return legacyStep.completedChaptersForMigration().also { completed ->
            putStringSet(COMPLETED_CHAPTERS_KEY, completed.mapTo(linkedSetOf()) { it.name })
        }
    }

    private companion object {
        const val COMPLETED_CHAPTERS_KEY = "first_run_onboarding_completed_chapters_v1"
        const val LEGACY_STEP_KEY = "first_run_onboarding_step_v1"
        const val SUGGESTED_GOAL_ZONE_KEY = "first_run_onboarding_suggested_goal_zone_v1"
        const val REMOVED_PLAN_SAVED_STEP = "PLAN_SAVED"
    }
}
