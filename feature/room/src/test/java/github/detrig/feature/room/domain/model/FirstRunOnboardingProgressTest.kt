package github.detrig.feature.room.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import github.detrig.feature.room.api.FirstRunOnboardingStep

class FirstRunOnboardingProgressTest {
    @Test fun chaptersAreCompletedInDocumentOrder() {
        var progress = FirstRunOnboardingProgress()

        assertEquals(FirstRunOnboardingStep.INTRODUCTION, progress.firstStep)
        FirstRunOnboardingChapter.entries.forEachIndexed { index, chapter ->
            assertEquals(chapter, progress.currentChapter)
            progress = progress.complete(chapter)
            assertEquals(index + 1, progress.completedChapters.size)
        }

        assertTrue(progress.isCompleted)
        assertEquals(FirstRunOnboardingStep.COMPLETED, progress.firstStep)
    }

    @Test fun interruptedChapterRestartsFromItsFirstStep() {
        val restored = FirstRunOnboardingProgress(
            completedChapters = setOf(
                FirstRunOnboardingChapter.INTRODUCTION_AND_FIRST_MONEY,
                FirstRunOnboardingChapter.BUDGET_PLANNING,
            ),
        )

        assertEquals(FirstRunOnboardingChapter.MINI_GAMES_DISCOVERY, restored.currentChapter)
        assertEquals(FirstRunOnboardingStep.GAME_DISCOVERY, restored.firstStep)
        assertFalse(restored.isCompleted)
    }

    @Test fun staleOrRepeatedCompletionCannotSkipAChapter() {
        val progress = FirstRunOnboardingProgress()
        val skipped = progress.complete(FirstRunOnboardingChapter.BUDGET_PLANNING)
        val completedOnce = progress.complete(FirstRunOnboardingChapter.INTRODUCTION_AND_FIRST_MONEY)
        val completedTwice = completedOnce.complete(FirstRunOnboardingChapter.INTRODUCTION_AND_FIRST_MONEY)

        assertEquals(progress, skipped)
        assertEquals(completedOnce, completedTwice)
        assertEquals(FirstRunOnboardingChapter.BUDGET_PLANNING, completedTwice.currentChapter)
    }

    @Test fun legacyCompletedStepContinuesWithTheNewCareChapter() {
        val migrated = FirstRunOnboardingProgress(
            FirstRunOnboardingStep.COMPLETED.completedChaptersForMigration(),
        )

        assertFalse(migrated.isCompleted)
        assertEquals(FirstRunOnboardingStep.WAITING_FOR_HUNGER, migrated.firstStep)
    }

    @Test fun legacyMiddleStepRestartsItsChapter() {
        val migrated = FirstRunOnboardingProgress(
            FirstRunOnboardingStep.GAME_SELECTED.completedChaptersForMigration(),
        )

        assertEquals(FirstRunOnboardingStep.GAME_DISCOVERY, migrated.firstStep)
    }
}
