package github.detrig.feature.phone.presentation

import github.detrig.core.mvvm.CoreViewEvent
import github.detrig.core.mvvm.CoreViewModel
import github.detrig.core.mvvm.CoreViewState
import github.detrig.core.mvvm.ExceptionConsumer
import github.detrig.feature.phone.data.MessagesRepository
import github.detrig.feature.phone.domain.MessageSenderId
import github.detrig.feature.phone.domain.MessagesCoordinator
import github.detrig.feature.phone.domain.MessagesInbox
import github.detrig.feature.phone.domain.SecurityMessageEvent
import github.detrig.feature.phone.domain.SecurityMessageScenario
import github.detrig.feature.phone.domain.SecurityResponseChoice
import github.detrig.feature.room.presentation.ParentHelpDialogState
import github.detrig.feature.economy.domain.ParentHelpRequestResult
import github.detrig.feature.economy.domain.FinancialOperationResult
import kotlinx.coroutines.Job

internal data class MessagesViewState(
    val inbox: MessagesInbox = MessagesInbox.Empty,
    val selectedSenderId: MessageSenderId? = null,
    val dialogueEventId: String? = null,
    val isResponding: Boolean = false,
    val parentHelpDialog: ParentHelpDialogState? = null,
    val errorMessage: String? = null,
) : CoreViewState

internal sealed interface MessagesViewEvent : CoreViewEvent {
    data object Load : MessagesViewEvent
    data object AppOpened : MessagesViewEvent
    data class ThreadOpened(val senderId: MessageSenderId) : MessagesViewEvent
    data object ThreadClosed : MessagesViewEvent
    data class GuidanceDismissed(val eventId: String) : MessagesViewEvent
    data class FeedbackDismissed(val eventId: String) : MessagesViewEvent
    data class SuspiciousInteraction(
        val eventId: String,
        val choice: SecurityResponseChoice,
    ) : MessagesViewEvent
    data object ParentHelpOfferOpened : MessagesViewEvent
    data class ParentHelpAccepted(val offerId: String) : MessagesViewEvent
    data object ParentHelpPaidOff : MessagesViewEvent
    data object ParentHelpDismissed : MessagesViewEvent
}

internal class MessagesViewModel(
    private val repository: MessagesRepository,
    private val coordinator: MessagesCoordinator,
) : CoreViewModel<MessagesViewState, MessagesViewEvent>(MessagesViewState()) {

    private var observationJob: Job? = null
    private var actionJob: Job? = null

    override fun perform(viewEvent: MessagesViewEvent) {
        when (viewEvent) {
            MessagesViewEvent.Load -> observeInbox()
            MessagesViewEvent.AppOpened -> launchAction { coordinator.consumeFirstRoomPrompt() }
            is MessagesViewEvent.ThreadOpened -> openThread(viewEvent.senderId)
            MessagesViewEvent.ThreadClosed -> closeThread()
            is MessagesViewEvent.GuidanceDismissed -> launchAction {
                coordinator.acknowledgeGuidance(viewEvent.eventId)
            }
            is MessagesViewEvent.FeedbackDismissed -> launchAction {
                coordinator.acknowledgeFeedback(viewEvent.eventId)
                updateState { copy(dialogueEventId = null) }
            }
            is MessagesViewEvent.SuspiciousInteraction -> respond(viewEvent.eventId, viewEvent.choice)
            MessagesViewEvent.ParentHelpOfferOpened -> openParentHelp()
            is MessagesViewEvent.ParentHelpAccepted -> acceptParentHelp(viewEvent.offerId)
            MessagesViewEvent.ParentHelpPaidOff -> payOffParentHelp()
            MessagesViewEvent.ParentHelpDismissed -> updateState { copy(parentHelpDialog = null) }
        }
    }

    private fun openParentHelp() {
        if (actionJob?.isActive == true) return
        actionJob = launchCoroutine(handleAction = errorHandler()) {
            val data = coordinator.parentHelpDialogData()
            updateState {
                copy(parentHelpDialog = ParentHelpDialogState(
                    offers = data.offers,
                    activeHelp = data.activeHelp,
                    availableRub = data.availableRub,
                    savingsRub = data.savingsRub,
                    debtRub = data.debtRub,
                    minimumRequiredBalanceRub = data.minimumRequiredBalanceRub,
                ))
            }
            actionJob = null
        }
    }

    private fun acceptParentHelp(offerId: String) {
        if (actionJob?.isActive == true || stateData.parentHelpDialog == null) return
        updateState { copy(parentHelpDialog = parentHelpDialog?.copy(isSubmitting = true)) }
        actionJob = launchCoroutine(handleAction = errorHandler()) {
            when (coordinator.requestParentHelp(offerId)) {
                is ParentHelpRequestResult.Accepted,
                is ParentHelpRequestResult.AlreadyActive,
                -> updateState { copy(parentHelpDialog = null) }
                is ParentHelpRequestResult.Rejected -> updateState {
                    copy(parentHelpDialog = parentHelpDialog?.copy(isSubmitting = false))
                }
            }
            actionJob = null
        }
    }

    private fun payOffParentHelp() {
        val currentHelp = stateData.parentHelpDialog?.activeHelp ?: return
        if (actionJob?.isActive == true || stateData.parentHelpDialog?.availableRub?.let {
                it >= currentHelp.remainingRub
            } != true
        ) return
        updateState { copy(parentHelpDialog = parentHelpDialog?.copy(isSubmitting = true), errorMessage = null) }
        actionJob = launchCoroutine(handleAction = errorHandler()) {
            when (coordinator.settleParentHelpInFull()) {
                is FinancialOperationResult.Applied,
                is FinancialOperationResult.AlreadyApplied,
                -> updateState { copy(parentHelpDialog = null) }
                is FinancialOperationResult.Rejected -> updateState {
                    copy(
                        parentHelpDialog = parentHelpDialog?.copy(isSubmitting = false),
                        errorMessage = "Не получилось выполнить выплату",
                    )
                }
            }
            actionJob = null
        }
    }

    private fun observeInbox() {
        if (observationJob?.isActive == true) return
        observationJob = launchCoroutine(handleAction = errorHandler()) {
            repository.observeInbox().collect { inbox ->
                updateState {
                    copy(
                        inbox = inbox,
                        dialogueEventId = dialogueEventId ?: inbox.pendingFeedbackEvent()?.id,
                        errorMessage = null,
                    )
                }
            }
        }
    }

    private fun openThread(senderId: MessageSenderId) {
        updateState { copy(selectedSenderId = senderId, errorMessage = null) }
        launchAction {
            repository.markThreadRead(senderId)
            repository.observeInbox().value.threads
                .firstOrNull { it.senderId == senderId }
                ?.latestEvent
                ?.let { coordinator.revealFirstGuidance(it) }
        }
    }

    private fun respond(eventId: String, choice: SecurityResponseChoice) {
        if (actionJob?.isActive == true) return
        updateState {
            copy(isResponding = true, dialogueEventId = eventId, errorMessage = null)
        }
        actionJob = launchCoroutine(handleAction = errorHandler()) {
            val event = repository.respond(eventId, choice)
            if (event != null) coordinator.deliverOutcome(event)
            updateState { copy(isResponding = false) }
            actionJob = null
        }
    }

    private fun closeThread() {
        val current = state().value ?: return
        val event = current.selectedSenderId
            ?.let { selected -> current.inbox.threads.firstOrNull { it.senderId == selected } }
            ?.latestEvent
        if (event == null || event.response != null || actionJob?.isActive == true) {
            updateState { copy(selectedSenderId = null) }
            return
        }
        updateState {
            copy(
                selectedSenderId = null,
                dialogueEventId = event.id,
                isResponding = true,
                errorMessage = null,
            )
        }
        actionJob = launchCoroutine(handleAction = errorHandler()) {
            val resolved = repository.respond(event.id, event.safeChoice())
            if (resolved != null) coordinator.deliverOutcome(resolved)
            updateState { copy(isResponding = false) }
            actionJob = null
        }
    }

    private fun launchAction(block: suspend () -> Unit) {
        launchCoroutine(handleAction = errorHandler()) { block() }
    }

    private fun errorHandler() = ExceptionConsumer {
        updateState {
            copy(
                isResponding = false,
                parentHelpDialog = parentHelpDialog?.copy(isSubmitting = false),
                errorMessage = "Не удалось обновить сообщения",
            )
        }
        actionJob = null
        true
    }
}

private fun SecurityMessageEvent.safeChoice(): SecurityResponseChoice = when (scenario) {
    SecurityMessageScenario.CONFIRMATION_CODE -> SecurityResponseChoice.KEEP_CODE_SECRET
    SecurityMessageScenario.UNKNOWN_LINK -> SecurityResponseChoice.IGNORE_LINK
}

private fun MessagesInbox.pendingFeedbackEvent(): SecurityMessageEvent? = threads
    .mapNotNull { it.latestEvent }
    .filter { event ->
        event.response != null &&
            !event.feedbackAcknowledged &&
            (event.response.isSafe || event.penaltyApplied)
    }
    .maxByOrNull { it.absoluteDay }
