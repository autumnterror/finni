package github.detrig.feature.learningtests.presentation

import github.detrig.core.mvvm.CoreViewState
import github.detrig.feature.learningtests.domain.DailyLearningTests

internal data class LearningTestsHomeViewState(
    val isLoading: Boolean = true,
    val dailyTests: DailyLearningTests? = null,
    val hasError: Boolean = false,
    val showDailyTestsIntroduction: Boolean = false,
) : CoreViewState
