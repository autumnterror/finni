package github.detrig.feature.learningtests.data.local

import android.content.SharedPreferences
import github.detrig.core.infrastructure.preferences.SharedStorage
import github.detrig.feature.learningtests.domain.LearningTestsTutorialRepository

internal class LearningTestsTutorialStorage(preferences: SharedPreferences) :
    SharedStorage(preferences), LearningTestsTutorialRepository {

    @Synchronized
    override fun claimDailyTestsIntroduction(): Boolean {
        if (readBoolean(DAILY_TESTS_INTRODUCTION_SEEN, false)) return false
        putBoolean(DAILY_TESTS_INTRODUCTION_SEEN, true)
        return true
    }

    private companion object {
        const val DAILY_TESTS_INTRODUCTION_SEEN = "daily_tests_introduction_seen_v1"
    }
}
