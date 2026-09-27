package github.detrig.feature.learningtests.presentation

import github.detrig.core.mvvm.CoreViewModel
import github.detrig.core.mvvm.ExceptionConsumer
import github.detrig.feature.learningtests.domain.LearningTestsInteractor
import github.detrig.feature.learningtests.navigation.LearningTestsRouter

internal class LearningTestQuizViewModel(
    private val testId: String,
    private val interactor: LearningTestsInteractor,
    private val router: LearningTestsRouter,
) : CoreViewModel<LearningTestQuizViewState, LearningTestQuizViewEvent>(LearningTestQuizViewState()) {
    private var loaded = false

    override fun perform(viewEvent: LearningTestQuizViewEvent) {
        when (viewEvent) {
            LearningTestQuizViewEvent.Load -> if (!loaded || stateData.hasError) load()
            LearningTestQuizViewEvent.Close -> if (!stateData.isBusy) router.back()
            is LearningTestQuizViewEvent.SelectOption -> {
                val session = stateData.session ?: return
                if (!stateData.isBusy && session.answer == null &&
                    viewEvent.optionIndex in session.question?.options.orEmpty().indices
                ) {
                    updateState { copy(selectedOptionIndex = viewEvent.optionIndex) }
                }
            }
            LearningTestQuizViewEvent.SubmitAnswer -> submitAnswer()
            LearningTestQuizViewEvent.Continue -> continueTest()
        }
    }

    private fun load() = launchCoroutine(
        handleAction = ExceptionConsumer {
            updateState { copy(isLoading = false, hasError = true) }
            true
        },
    ) {
        updateState { copy(isLoading = true, hasError = false) }
        val session = interactor.startTest(testId)
        loaded = true
        updateState {
            copy(
                isLoading = false,
                session = session,
                selectedOptionIndex = session?.answer?.selectedOptionIndex,
                unavailable = session == null,
            )
        }
    }

    private fun submitAnswer() {
        val current = stateData.session ?: return
        val selected = stateData.selectedOptionIndex ?: return
        if (stateData.isBusy || current.answer != null || current.attempt.isComplete) return
        runAction {
            val updated = interactor.submitAnswer(
                testId = testId,
                questionIndex = current.attempt.questionIndex,
                selectedOptionIndex = selected,
            )
            updateState {
                copy(
                    isBusy = false,
                    session = updated,
                    selectedOptionIndex = updated?.answer?.selectedOptionIndex,
                    hasError = false,
                    unavailable = updated == null,
                )
            }
        }
    }

    private fun continueTest() {
        val current = stateData.session ?: return
        if (stateData.isBusy || current.answer == null || current.attempt.isComplete) return
        runAction {
            val updated = interactor.continueTest(testId, current.attempt.questionIndex)
            updateState {
                copy(
                    isBusy = false,
                    session = updated,
                    selectedOptionIndex = updated?.answer?.selectedOptionIndex,
                    hasError = false,
                    unavailable = updated == null,
                )
            }
        }
    }

    private fun runAction(block: suspend () -> Unit) {
        if (stateData.isBusy) return
        updateState { copy(isBusy = true, hasError = false) }
        launchCoroutine(
            handleAction = ExceptionConsumer {
                updateState { copy(isBusy = false, hasError = true) }
                true
            },
        ) { block() }
    }
}
