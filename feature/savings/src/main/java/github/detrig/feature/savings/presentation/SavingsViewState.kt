package github.detrig.feature.savings.presentation

import github.detrig.core.mvvm.CoreViewState
import github.detrig.feature.economy.domain.EconomyState
import github.detrig.feature.economy.domain.RejectionReason
import github.detrig.feature.economy.domain.SavingsGoalProgress
import github.detrig.feature.savings.api.SavingsGoalDraft

internal enum class SavingsTransferDirection { DEPOSIT, WITHDRAW }

internal sealed interface SavingsNotice {
    data class TransferCompleted(val direction: SavingsTransferDirection, val amountRub: Long) : SavingsNotice
    data class Rejected(val reason: RejectionReason, val missingRub: Long = 0) : SavingsNotice
    data object GoalSaved : SavingsNotice
}

internal data class SavingsViewState(
    val economy: EconomyState? = null,
    val goal: SavingsGoalProgress? = null,
    val starterGoals: List<SavingsGoalDraft> = emptyList(),
    val loading: Boolean = true,
    val busy: Boolean = false,
    val transferDirection: SavingsTransferDirection? = null,
    val notice: SavingsNotice? = null,
) : CoreViewState
