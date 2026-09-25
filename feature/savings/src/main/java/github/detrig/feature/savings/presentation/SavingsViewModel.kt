package github.detrig.feature.savings.presentation

import github.detrig.core.mvvm.CoreViewModel
import github.detrig.core.mvvm.ExceptionConsumer
import github.detrig.core.audio.GameAudio
import github.detrig.core.audio.SilentGameAudio
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.economy.domain.FinancialOperationResult
import github.detrig.feature.economy.domain.RejectionReason
import github.detrig.feature.savings.api.SavingsGoalDraft
import github.detrig.feature.savings.domain.CreateSavingsGoalInteractor
import github.detrig.feature.savings.domain.SavingsConfiguration
import github.detrig.feature.savings.domain.TransferFromSavingsInteractor
import github.detrig.feature.savings.domain.TransferToSavingsInteractor
import github.detrig.feature.savings.domain.SavingsLearningInteractor
import github.detrig.feature.savings.navigation.SavingsRouter
import kotlinx.coroutines.Job
import java.util.UUID

internal class SavingsViewModel(
    private val economy: EconomyApi,
    configuration: SavingsConfiguration,
    private val createGoal: CreateSavingsGoalInteractor,
    private val transferTo: TransferToSavingsInteractor,
    private val transferFrom: TransferFromSavingsInteractor,
    private val learning: SavingsLearningInteractor,
    private val router: SavingsRouter,
    firstRunOnboarding: Boolean,
    suggestedGoalId: String?,
    private val gameAudio: GameAudio = SilentGameAudio,
) : CoreViewModel<SavingsViewState, SavingsViewEvent>(
    SavingsViewState(
        starterGoals = configuration.starterGoals.prioritize(suggestedGoalId),
        suggestedGoalId = suggestedGoalId,
        onboardingStep = SavingsOnboardingStep.INTRODUCTION.takeIf { firstRunOnboarding },
    ),
) {
    private var loadingJob: Job? = null
    private var actionJob: Job? = null

    override fun perform(viewEvent: SavingsViewEvent) {
        when (viewEvent) {
            SavingsViewEvent.Load -> load()
            SavingsViewEvent.Back -> if (stateData.onboardingStep == null) router.back()
            is SavingsViewEvent.GoalSelected -> {
                if (stateData.onboardingStep == SavingsOnboardingStep.SELECT_GOAL) {
                    updateState { copy(pendingGoal = viewEvent.goal, onboardingStep = SavingsOnboardingStep.CONFIRM_GOAL) }
                } else if (stateData.onboardingStep == null) selectGoal(viewEvent.goal)
            }
            SavingsViewEvent.GoalConfirmed -> stateData.pendingGoal?.let(::selectGoal)
            SavingsViewEvent.GoalChangeRequested -> updateState {
                copy(pendingGoal = null, onboardingStep = SavingsOnboardingStep.SELECT_GOAL)
            }
            is SavingsViewEvent.TransferOpened -> updateState { copy(transferDirection = viewEvent.direction, notice = null) }
            SavingsViewEvent.TransferDismissed -> if (!stateData.busy) updateState {
                copy(
                    transferDirection = null,
                    onboardingStep = if (onboardingStep == SavingsOnboardingStep.WAITING_FOR_DEPOSIT) {
                        SavingsOnboardingStep.FIRST_DEPOSIT
                    } else {
                        onboardingStep
                    },
                )
            }
            is SavingsViewEvent.TransferConfirmed -> transfer(viewEvent.amountRub)
            SavingsViewEvent.NoticeDismissed -> updateState { copy(notice = null) }
            SavingsViewEvent.OnboardingContinue -> continueOnboarding()
            is SavingsViewEvent.OnboardingDepositSelected ->
                selectOnboardingDeposit(viewEvent.depositNow)
        }
    }

    private fun load() {
        if (loadingJob?.isActive == true) return
        loadingJob = launchCoroutine(handleAction = ExceptionConsumer {
            updateState { copy(loading = false, busy = false) }
            true
        }) {
            economy.initialize()
            learning.reconcile()
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
            gameAudio.play(SavingsAudioCues.GoalSaved)
            refreshGoal()
            updateState {
                val isOnboarding = onboardingStep != null
                copy(
                    busy = false,
                    notice = SavingsNotice.GoalSaved.takeUnless { isOnboarding },
                    onboardingStep = SavingsOnboardingStep.GOAL_CREATED.takeIf { isOnboarding },
                    pendingGoal = null,
                )
            }
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
                is FinancialOperationResult.AlreadyApplied -> {
                    if (result is FinancialOperationResult.Applied) {
                        gameAudio.play(if (direction == SavingsTransferDirection.DEPOSIT)
                            SavingsAudioCues.Deposit else SavingsAudioCues.Withdrawal)
                    }
                    refreshGoal()
                    val operation = when (result) {
                        is FinancialOperationResult.Applied -> result.operation
                        is FinancialOperationResult.AlreadyApplied -> result.operation
                    }
                    val reachedNow = direction == SavingsTransferDirection.DEPOSIT &&
                        operation.before.savingsRub < goal.targetRub &&
                        operation.after.savingsRub >= goal.targetRub
                    updateState {
                        val isOnboardingDeposit = onboardingStep == SavingsOnboardingStep.WAITING_FOR_DEPOSIT
                        copy(
                            busy = false,
                            transferDirection = null,
                            notice = (if (reachedNow) SavingsNotice.GoalReached(goal.title)
                                else SavingsNotice.TransferCompleted(direction, amountRub))
                                .takeUnless { isOnboardingDeposit },
                            onboardingStep = if (isOnboardingDeposit) {
                                SavingsOnboardingStep.DEPOSIT_DONE
                            } else {
                                onboardingStep
                            },
                        )
                    }
                }
                is FinancialOperationResult.Rejected -> updateState {
                    val isOnboardingDeposit = onboardingStep == SavingsOnboardingStep.WAITING_FOR_DEPOSIT
                    copy(
                        busy = false,
                        transferDirection = null,
                        notice = SavingsNotice.Rejected(result.reason, missingRub(result.reason, amountRub, result.state)),
                        onboardingStep = if (isOnboardingDeposit) {
                            SavingsOnboardingStep.FIRST_DEPOSIT
                        } else {
                            onboardingStep
                        },
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
        updateState {
            copy(
                busy = false,
                transferDirection = null,
                onboardingStep = if (onboardingStep == SavingsOnboardingStep.WAITING_FOR_DEPOSIT) {
                    SavingsOnboardingStep.FIRST_DEPOSIT
                } else {
                    onboardingStep
                },
            )
        }
        true
    }

    private fun continueOnboarding() {
        when (stateData.onboardingStep) {
            SavingsOnboardingStep.INTRODUCTION -> updateState {
                copy(
                    onboardingStep = if (goal == null) {
                        SavingsOnboardingStep.SELECT_GOAL
                    } else {
                        SavingsOnboardingStep.GOAL_CREATED
                    },
                )
            }
            SavingsOnboardingStep.GOAL_CREATED -> updateState {
                copy(onboardingStep = if ((goal?.savedRub ?: 0) > 0) {
                    SavingsOnboardingStep.DEPOSIT_DONE
                } else {
                    SavingsOnboardingStep.FIRST_DEPOSIT
                })
            }
            SavingsOnboardingStep.DEPOSIT_DONE,
            SavingsOnboardingStep.DEPOSIT_SKIPPED,
            -> router.back()
            SavingsOnboardingStep.SELECT_GOAL,
            SavingsOnboardingStep.CONFIRM_GOAL,
            SavingsOnboardingStep.FIRST_DEPOSIT,
            SavingsOnboardingStep.WAITING_FOR_DEPOSIT,
            null,
            -> Unit
        }
    }

    private fun selectOnboardingDeposit(depositNow: Boolean) {
        if (stateData.onboardingStep != SavingsOnboardingStep.FIRST_DEPOSIT) return
        updateState {
            if (depositNow) {
                copy(
                    onboardingStep = SavingsOnboardingStep.WAITING_FOR_DEPOSIT,
                    transferDirection = SavingsTransferDirection.DEPOSIT,
                    notice = null,
                )
            } else {
                copy(onboardingStep = SavingsOnboardingStep.DEPOSIT_SKIPPED)
            }
        }
    }
}

internal fun List<SavingsGoalDraft>.prioritize(suggestedGoalId: String?): List<SavingsGoalDraft> {
    val suggested = firstOrNull { it.id == suggestedGoalId } ?: return this
    return listOf(suggested) + filterNot { it.id == suggested.id }
}
