package github.detrig.feature.room.data.local

import github.detrig.feature.room.api.FirstRunOnboardingStep
import github.detrig.feature.room.domain.model.FirstRunOnboardingChapter
import github.detrig.feature.room.domain.model.FirstRunOnboardingProgress
import github.detrig.feature.room.domain.model.FirstRunOnboardingRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class FirstRunGuideCoordinatorTest {
    @Test
    fun recreationRestartsTheInterruptedChapterFromItsFirstStep() {
        val repository = FakeOnboardingRepository(
            FirstRunOnboardingProgress(
                completedChapters = FirstRunOnboardingChapter.entries
                    .takeWhile { it != FirstRunOnboardingChapter.FIRST_NEED }
                    .toSet(),
            ),
        )
        FirstRunGuideCoordinator(repository).moveTo(FirstRunOnboardingStep.FEEDING)

        assertEquals(
            FirstRunOnboardingStep.HUNGER_INTRO,
            FirstRunGuideCoordinator(repository).step.value,
        )
    }

    @Test
    fun completingAChapterPersistsOnlyItsBoundary() {
        val repository = FakeOnboardingRepository(
            FirstRunOnboardingProgress(
                completedChapters = FirstRunOnboardingChapter.entries
                    .takeWhile { it != FirstRunOnboardingChapter.FIRST_NEED }
                    .toSet(),
            ),
        )
        val coordinator = FirstRunGuideCoordinator(repository)

        coordinator.moveTo(FirstRunOnboardingStep.FEEDING_DONE)
        coordinator.completeFirstNeed()

        assertEquals(FirstRunOnboardingStep.BEDTIME_LATE, coordinator.step.value)
        assertEquals(
            FirstRunOnboardingStep.BEDTIME_LATE,
            FirstRunGuideCoordinator(repository).step.value,
        )
    }
}

private class FakeOnboardingRepository(
    private var progress: FirstRunOnboardingProgress,
) : FirstRunOnboardingRepository {
    override fun load(): FirstRunOnboardingProgress = progress

    override fun markChapterCompleted(chapter: FirstRunOnboardingChapter): FirstRunOnboardingProgress =
        progress.complete(chapter).also { progress = it }

    override fun loadSuggestedGoalZoneId(): String? = null

    override fun saveSuggestedGoalZoneId(zoneId: String?) = Unit

    override fun isFirstGamePurchaseExplained(): Boolean = false

    override fun markFirstGamePurchaseExplained() = Unit

    override fun isFirstGameReadyIntroduced(): Boolean = false

    override fun markFirstGameReadyIntroduced() = Unit
}
