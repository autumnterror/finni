package github.detrig.feature.phone.domain

import github.detrig.feature.learning.api.LearningApi
import github.detrig.feature.learning.domain.FinancialSecurityLearning
import github.detrig.feature.learning.domain.RecordLearningResult
import github.detrig.feature.learning.domain.SecurityScenario
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.economy.domain.FinancialOperationResult
import github.detrig.feature.economy.domain.OperationContext
import github.detrig.feature.economy.domain.ParentHelpOffer
import github.detrig.feature.economy.domain.ParentHelpState
import github.detrig.feature.economy.domain.ParentHelpRequestResult
import github.detrig.feature.economy.domain.LowBalanceRecoveryAction
import github.detrig.feature.economy.domain.lowBalanceRecoveryAction
import github.detrig.feature.phone.data.MessagesRepository
import github.detrig.feature.week.api.WeekApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal class MessagesCoordinator(
    private val repository: MessagesRepository,
    private val weekApi: WeekApi,
    private val learningApi: LearningApi,
    private val economyApi: EconomyApi,
    private val minimumHelpBalanceRub: Long,
    private val eventConfig: SecurityEventConfig,
) {
    private var observationJob: Job? = null

    fun start(scope: CoroutineScope) {
        if (observationJob?.isActive == true) return
        observationJob = scope.launch {
            retryPendingLearningActions()
            retryPendingPenalties()
            processDay(weekApi.initialize().absoluteDay)
            combine(
                weekApi.observeState().map { it.absoluteDay },
                economyApi.observeState().map { it.availableRub to it.savingsRub },
            ) { absoluteDay, balances -> absoluteDay to balances }
                .distinctUntilChanged()
                .collect { (absoluteDay, _) -> processDay(absoluteDay) }
        }
    }

    suspend fun consumeFirstRoomPrompt() {
        repository.consumeFirstRoomPrompt()
    }

    suspend fun parentHelpOffers(): List<ParentHelpOffer> {
        val economy = economyApi.getState()
        return if (
            economyApi.getParentHelp() == null &&
            lowBalanceRecoveryAction(
                availableRub = economy.availableRub,
                savingsRub = economy.savingsRub,
                minimumRequiredBalanceRub = minimumHelpBalanceRub,
            ) == LowBalanceRecoveryAction.ASK_PARENTS
        ) {
            economyApi.parentHelpOffers()
        } else {
            emptyList()
        }
    }

    suspend fun parentHelpDialogData(): ParentHelpDialogData {
        val economy = economyApi.getState()
        val activeHelp = economyApi.getParentHelp()
        val offers = if (
            activeHelp == null && lowBalanceRecoveryAction(
                availableRub = economy.availableRub,
                savingsRub = economy.savingsRub,
                minimumRequiredBalanceRub = minimumHelpBalanceRub,
            ) == LowBalanceRecoveryAction.ASK_PARENTS
        ) economyApi.parentHelpOffers() else emptyList()
        return ParentHelpDialogData(offers, activeHelp, economy.availableRub)
    }

    suspend fun requestParentHelp(offerId: String): ParentHelpRequestResult {
        val week = weekApi.initialize()
        val result = economyApi.requestParentHelp(
            operationId = "phone-parent-help:${week.weekNumber}:$offerId",
            offerId = offerId,
        )
        if (result is ParentHelpRequestResult.Accepted || result is ParentHelpRequestResult.AlreadyActive) {
            repository.removeParentHelpReminder()
            repository.addParentHelpRepaymentMessage(week.absoluteDay)
        }
        return result
    }

    suspend fun settleParentHelpInFull(): FinancialOperationResult {
        val help = economyApi.getParentHelp()
            ?: return FinancialOperationResult.Rejected(
                reason = github.detrig.feature.economy.domain.RejectionReason.NO_ACTIVE_DEBT,
                state = economyApi.getState(),
            )
        val result = economyApi.settleParentHelpInFull(
            operationId = "phone-parent-help-payoff:${help.offerId}:${help.remainingRub}",
            context = OperationContext(
                reasonId = help.offerId,
                metadata = "source=parent-help;settlement=full;amountRub=${help.remainingRub}",
            ),
        )
        if (result is FinancialOperationResult.Applied || result is FinancialOperationResult.AlreadyApplied) {
            repository.removeParentHelpReminder()
            repository.removeParentHelpRepaymentMessage()
        }
        return result
    }

    suspend fun revealFirstGuidance(event: SecurityMessageEvent) {
        if (event.guidanceVisible) return
        val claimed = learningApi.claimFirstExplanation(
            profileId = CURRENT_PROFILE_ID,
            explanationId = FIRST_SCAM_EXPLANATION_ID,
        )
        if (claimed) repository.markGuidanceVisible(event.id)
    }

    suspend fun acknowledgeGuidance(eventId: String) {
        repository.acknowledgeGuidance(eventId)
    }

    suspend fun acknowledgeFeedback(eventId: String) {
        repository.acknowledgeFeedback(eventId)
    }

    suspend fun deliverOutcome(event: SecurityMessageEvent) {
        if (event.response?.isSafe == true) {
            deliverLearningAction(event)
        } else if (event.response != null) {
            deliverPenalty(event)
        }
    }

    suspend fun deliverLearningAction(event: SecurityMessageEvent) {
        if (event.response?.isSafe != true || event.learningDelivered) return
        val result = learningApi.record(
            FinancialSecurityLearning.safeResponseAction(
                actionId = "security-response:${event.id}",
                profileId = CURRENT_PROFILE_ID,
                absoluteDay = event.absoluteDay,
                eventId = event.id,
                scenario = when (event.scenario) {
                    SecurityMessageScenario.CONFIRMATION_CODE -> SecurityScenario.CONFIRMATION_CODE
                    SecurityMessageScenario.UNKNOWN_LINK -> SecurityScenario.UNKNOWN_LINK
                },
            ),
        )
        if (result is RecordLearningResult.Processed || result is RecordLearningResult.AlreadyProcessed) {
            repository.markLearningDelivered(event.id)
        }
    }

    private suspend fun processDay(absoluteDay: Long) {
        runCatching {
            repository.ensureEventForDay(absoluteDay, eventConfig)
            ensureParentHelpReminder(absoluteDay)
            retryPendingLearningActions()
            retryPendingPenalties()
        }
    }

    private suspend fun ensureParentHelpReminder(absoluteDay: Long) {
        val economy = economyApi.getState()
        val action = lowBalanceRecoveryAction(
            availableRub = economy.availableRub,
            savingsRub = economy.savingsRub,
            minimumRequiredBalanceRub = minimumHelpBalanceRub,
        )
        if (economyApi.getParentHelp() != null) {
            repository.removeParentHelpReminder()
            repository.addParentHelpRepaymentMessage(absoluteDay)
            return
        }
        repository.removeParentHelpRepaymentMessage()
        if (action == LowBalanceRecoveryAction.ASK_PARENTS && economyApi.parentHelpOffers().isNotEmpty()) {
            repository.addParentHelpReminder(absoluteDay)
        }
    }

    private suspend fun retryPendingLearningActions() {
        repository.pendingSafeResponses().forEach { event ->
            runCatching { deliverLearningAction(event) }
        }
    }


    private suspend fun retryPendingPenalties() {
        repository.pendingUnsafeResponses().forEach { event ->
            runCatching { deliverPenalty(event) }
        }
    }

    private suspend fun deliverPenalty(event: SecurityMessageEvent) {
        if (event.response?.isSafe != false || event.penaltyApplied) return
        val prepared = if (event.penaltyRub == null) {
            val amountRub = securityPenaltyRub(economyApi.getState().availableRub)
            repository.preparePenalty(event.id, amountRub)
        } else {
            event
        } ?: return
        val amountRub = prepared.penaltyRub ?: return
        if (amountRub == 0L) {
            repository.markPenaltyApplied(event.id)
            return
        }
        when (
            economyApi.debit(
                operationId = "security-penalty:${event.id}",
                amountRub = amountRub,
                context = OperationContext(
                    reasonId = "security_message_loss",
                    metadata = event.scenario.name,
                ),
            )
        ) {
            is FinancialOperationResult.Applied,
            is FinancialOperationResult.AlreadyApplied,
            -> repository.markPenaltyApplied(event.id)
            is FinancialOperationResult.Rejected -> error("Unable to apply security event penalty")
        }
    }

    private companion object {
        const val CURRENT_PROFILE_ID = "current"
        const val FIRST_SCAM_EXPLANATION_ID = "security.messages.first_scam"
    }
}
