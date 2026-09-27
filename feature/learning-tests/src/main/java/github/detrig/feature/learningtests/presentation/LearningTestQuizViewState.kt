package github.detrig.feature.learningtests.presentation

import github.detrig.core.mvvm.CoreViewState
import github.detrig.feature.learningtests.domain.LearningTestSession

internal data class LearningTestQuizViewState(
    val isLoading: Boolean = true,
    val isBusy: Boolean = false,
    val session: LearningTestSession? = null,
    val selectedOptionIndex: Int? = null,
    val hasError: Boolean = false,
    val unavailable: Boolean = false,
) : CoreViewState
