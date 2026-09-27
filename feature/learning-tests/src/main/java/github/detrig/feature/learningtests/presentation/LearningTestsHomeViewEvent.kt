package github.detrig.feature.learningtests.presentation

import github.detrig.core.mvvm.CoreViewEvent

internal sealed interface LearningTestsHomeViewEvent : CoreViewEvent {
    data object Load : LearningTestsHomeViewEvent
    data object Refresh : LearningTestsHomeViewEvent
    data object Close : LearningTestsHomeViewEvent
    data class OpenTest(val testId: String) : LearningTestsHomeViewEvent
}
