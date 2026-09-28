package github.detrig.feature.learningtests.presentation

import github.detrig.core.mvvm.CoreViewModel
import github.detrig.core.mvvm.ExceptionConsumer
import github.detrig.feature.learningtests.domain.LearningTestsInteractor
import github.detrig.feature.learningtests.domain.LearningTestsTutorialRepository
import github.detrig.feature.learningtests.navigation.LearningTestsRouter

internal class LearningTestsHomeViewModel(
    private val interactor: LearningTestsInteractor,
    private val router: LearningTestsRouter,
    private val tutorialRepository: LearningTestsTutorialRepository,
) : CoreViewModel<LearningTestsHomeViewState, LearningTestsHomeViewEvent>(LearningTestsHomeViewState()) {
    private var loaded = false
    private var tutorialEvaluated = false

    override fun perform(viewEvent: LearningTestsHomeViewEvent) {
        when (viewEvent) {
            LearningTestsHomeViewEvent.Load -> if (!loaded) load()
            LearningTestsHomeViewEvent.Refresh -> if (loaded && !stateData.isLoading) load()
            LearningTestsHomeViewEvent.Close -> router.back()
            LearningTestsHomeViewEvent.DismissDailyTestsIntroduction -> {
                updateState { copy(showDailyTestsIntroduction = false) }
            }
            is LearningTestsHomeViewEvent.OpenTest -> {
                updateState { copy(showDailyTestsIntroduction = false) }
                router.openQuiz(viewEvent.testId)
            }
        }
    }

    private fun load() = launchCoroutine(
        handleAction = ExceptionConsumer {
            updateState { copy(isLoading = false, hasError = true) }
            true
        },
    ) {
        updateState { copy(isLoading = true, hasError = false) }
        val dailyTests = interactor.loadDailyTests()
        val showIntroduction = if (!tutorialEvaluated && dailyTests.offers.isNotEmpty()) {
            tutorialEvaluated = true
            tutorialRepository.claimDailyTestsIntroduction()
        } else {
            stateData.showDailyTestsIntroduction
        }
        loaded = true
        updateState {
            copy(
                isLoading = false,
                dailyTests = dailyTests,
                showDailyTestsIntroduction = showIntroduction,
            )
        }
    }
}
