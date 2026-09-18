package github.detrig.feature.savings.presentation

import github.detrig.core.mvvm.CoreViewEvent
import github.detrig.feature.savings.api.SavingsGoalDraft

internal sealed interface SavingsViewEvent : CoreViewEvent {
    data object Load : SavingsViewEvent
    data object Back : SavingsViewEvent
    data class GoalSelected(val goal: SavingsGoalDraft) : SavingsViewEvent
    data class TransferOpened(val direction: SavingsTransferDirection) : SavingsViewEvent
    data object TransferDismissed : SavingsViewEvent
    data class TransferConfirmed(val amountRub: Long) : SavingsViewEvent
    data object NoticeDismissed : SavingsViewEvent
}
