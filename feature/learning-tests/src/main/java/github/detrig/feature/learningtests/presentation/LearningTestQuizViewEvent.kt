package github.detrig.feature.learningtests.presentation

import github.detrig.core.mvvm.CoreViewEvent

internal sealed interface LearningTestQuizViewEvent : CoreViewEvent {
    data object Load : LearningTestQuizViewEvent
    data object Close : LearningTestQuizViewEvent
    data class SelectOption(val optionIndex: Int) : LearningTestQuizViewEvent
    data object SubmitAnswer : LearningTestQuizViewEvent
    data object Continue : LearningTestQuizViewEvent
}
