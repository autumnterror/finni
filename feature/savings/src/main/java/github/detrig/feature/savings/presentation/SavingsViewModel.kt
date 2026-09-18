package github.detrig.feature.savings.presentation

import github.detrig.core.mvvm.CoreViewModel
import github.detrig.core.mvvm.ExceptionConsumer
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.economy.domain.FinancialOperationResult
import github.detrig.feature.economy.domain.RejectionReason
import github.detrig.feature.savings.api.SavingsGoalDraft
import github.detrig.feature.savings.domain.CreateSavingsGoalInteractor
import github.detrig.feature.savings.domain.SavingsConfiguration
import github.detrig.feature.savings.domain.TransferFromSavingsInteractor
import github.detrig.feature.savings.domain.TransferToSavingsInteractor
import github.detrig.feature.savings.navigation.SavingsRouter
import kotlinx.coroutines.Job
import java.util.UUID

internal class SavingsViewModel(
    private val economy: EconomyApi,
    configuration: SavingsConfiguration,
    private val createGoal: CreateSavingsGoalInteractor,
    private val transferTo: TransferToSavingsInteractor,
    private val transferFrom: TransferFromSavingsInteractor,
    private val router: SavingsRouter,
) : CoreViewModel<SavingsViewState, SavingsViewEvent>(SavingsViewState(starterGoals = configuration.starterGoals)) {
    private var loadingJob: Job? = null
    private var actionJob: Job? = null

    override fun perform(viewEvent: SavingsViewEvent) {
        when (viewEvent) {
            SavingsViewEvent.Load -> load()
            SavingsViewEvent.Back -> router.back()
            is SavingsViewEvent.GoalSelected -> selectGoal(viewEvent.goal)
            is SavingsViewEvent.TransferOpened -> updateState { copy(transferDirection = viewEvent.direction, notice = null) }
            SavingsViewEvent.TransferDismissed -> if (!stateData.busy) updateState { copy(transferDirection = null) }
            is SavingsViewEvent.TransferConfirmed -> transfer(viewEvent.amountRub)
            SavingsViewEvent.NoticeDismissed -> updateState { copy(notice = null) }
        }
    }

    private fun load() {
        if (loadingJob?.isActive == true) return
        loadingJob = launchCoroutine(handleAction = ExceptionConsumer {
            updateState { copy(loading = false, busy = false) }
            true
        }) {
            economy.initialize()
            refreshGoal()
            economy.observeState().collect { state ->
                updateState { copy(economy = state, loading = false) }
                refreshGoal()
            }
        }
    }

    private suspend fun refreshGoal() {
        val goal = economy.getActiveGoal()?.let { economy.getGoalProgress(it.id) }
        updateState { copy(goal = goal, loading = false) }
    }

    private fun selectGoal(draft: SavingsGoalDraft) {
        if (actionJob?.isActive == true) return
        updateState { copy(busy = true, notice = null) }
        actionJob = launchCoroutine(handleAction = actionFailure()) {
            createGoal(draft)
            refreshGoal()
            updateState { copy(busy = false, notice = SavingsNotice.GoalSaved) }
        }
    }

    private fun transfer(amountRub: Long) {
        if (actionJob?.isActive == true || amountRub <= 0) return
        val direction = stateData.transferDirection ?: return
        val goal = stateData.goal?.goal ?: return
        updateState { copy(busy = true, notice = null) }
        val operationId = "savings:${direction.name.lowercase()}:${goal.id}:${UUID.randomUUID()}"
        actionJob = launchCoroutine(handleAction = actionFailure()) {
            val result = when (direction) {
                SavingsTransferDirection.DEPOSIT -> transferTo(operationId, goal.id, amountRub)
                SavingsTransferDirection.WITHDRAW -> transferFrom(operationId, goal.id, amountRub)
            }
            when (result) {
                is FinancialOperationResult.Applied,
                is FinancialOperationResult.AlreadyApplied -> updateState {
                    copy(
                        busy = false,
                        transferDirection = null,
                        notice = SavingsNotice.TransferCompleted(direction, amountRub),
                    )
                }
                is FinancialOperationResult.Rejected -> updateState {
                    copy(
                        busy = false,
                        transferDirection = null,
                        notice = SavingsNotice.Rejected(result.reason, missingRub(result.reason, amountRub, result.state)),
                    )
                }
            }
        }
    }

    private fun missingRub(reason: RejectionReason, amountRub: Long, state: github.detrig.feature.economy.domain.EconomyState): Long = when (reason) {
        RejectionReason.INSUFFICIENT_AVAILABLE_FUNDS -> amountRub - state.availableRub
        RejectionReason.INSUFFICIENT_SAVINGS -> amountRub - state.savingsRub
        else -> 0
    }.coerceAtLeast(0)

    private fun actionFailure() = ExceptionConsumer {
        updateState { copy(busy = false, transferDirection = null) }
        true
    }
}
