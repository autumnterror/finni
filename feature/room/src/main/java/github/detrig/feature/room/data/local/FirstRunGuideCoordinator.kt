package github.detrig.feature.room.data.local

import github.detrig.feature.room.api.FirstRunGuideApi
import github.detrig.feature.room.api.FirstRunOnboardingStep
import github.detrig.feature.room.domain.model.FirstRunOnboardingRepository
import github.detrig.feature.room.domain.model.FirstRunOnboardingChapter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class FirstRunGuideCoordinator(
    private val repository: FirstRunOnboardingRepository,
) : FirstRunGuideApi {
    private val mutableStep = MutableStateFlow(repository.load().firstStep)
    private val mutableResetVersion = MutableStateFlow(0)

    override val step: StateFlow<FirstRunOnboardingStep> = mutableStep.asStateFlow()
    override val resetVersion: StateFlow<Int> = mutableResetVersion.asStateFlow()

    override fun moveTo(step: FirstRunOnboardingStep) {
        if (repository.load().isCompleted && step != FirstRunOnboardingStep.COMPLETED) return
        mutableStep.value = step
    }

    override fun completeFirstNeed() {
        val progress = repository.markChapterCompleted(FirstRunOnboardingChapter.FIRST_NEED)
        moveTo(progress.firstStep)
    }

    override fun reset(skipOnboarding: Boolean) {
        val progress = repository.reset(skipOnboarding)
        mutableStep.value = progress.firstStep
        mutableResetVersion.value += 1
    }
}
