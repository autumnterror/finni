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
import github.detrig.feature.economy.domain.RejectionReason
import github.detrig.feature.economy.domain.canOfferParentHelp
import github.detrig.feature.phone.data.MessagesRepository
import github.detrig.feature.week.api.WeekApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class MessagesCoordinator(
    private val repository: MessagesRepository,
    private val weekApi: WeekApi,
    private val learningApi: LearningApi,
    private val economyApi: EconomyApi,
    private val minimumHelpBalanceRub: Long,
    private val eventConfig: SecurityEventConfig,
) {
    private var observationJob: Job? = null
    private val parentHelpMutex = Mutex()

    fun start(scope: CoroutineScope) {
        if (observationJob?.isActive == true) return
        observationJob = scope.launch {
            retryPendingLearningActions()
            retryPendingPenalties()
            processDay(weekApi.initialize().absoluteDay)
            combine(
                weekApi.observeState().map { it.absoluteDay },
                economyApi.observeState().map { Triple(it.availableRub, it.savingsRub, it.debtRub) },
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
        val activeHelp = economyApi.getParentHelp()
        return if (canOfferParentHelp(
                availableRub = economy.availableRub,
                savingsRub = economy.savingsRub,
                debtRub = economy.debtRub,
                hasActiveParentHelp = activeHelp != null,
                minimumRequiredBalanceRub = minimumHelpBalanceRub,
            )
        ) {
            economyApi.parentHelpOffers()
        } else {
            emptyList()
        }
    }

    suspend fun parentHelpDialogData(): ParentHelpDialogData {
        val economy = economyApi.getState()
        val activeHelp = economyApi.getParentHelp()
        val offers = if (canOfferParentHelp(
                availableRub = economy.availableRub,
                savingsRub = economy.savingsRub,
                debtRub = economy.debtRub,
                hasActiveParentHelp = activeHelp != null,
                minimumRequiredBalanceRub = minimumHelpBalanceRub,
            )
        ) economyApi.parentHelpOffers() else emptyList()
        return ParentHelpDialogData(
            offers = offers,
            activeHelp = activeHelp,
            availableRub = economy.availableRub,
            savingsRub = economy.savingsRub,
            debtRub = economy.debtRub,
            minimumRequiredBalanceRub = minimumHelpBalanceRub,
        )
    }

    suspend fun requestParentHelp(offerId: String): ParentHelpRequestResult = parentHelpMutex.withLock {
        if (parentHelpOffers().none { it.id == offerId } && economyApi.getParentHelp() == null) {
            return@withLock ParentHelpRequestResult.Rejected(
                RejectionReason.PARENT_HELP_NOT_AVAILABLE,
                economyApi.getState(),
            )
        }
        val week = weekApi.initialize()
        val result = economyApi.requestParentHelp(
            operationId = "phone-parent-help:${week.weekNumber}:$offerId",
            offerId = offerId,
        )
        val help = when (result) {
            is ParentHelpRequestResult.Accepted -> result.help
            is ParentHelpRequestResult.AlreadyActive -> result.help
            is ParentHelpRequestResult.Rejected -> null
        }
        if (help != null) {
            repository.upsertParentHelpMessage(week.absoluteDay, help)
        }
        result
    }

    suspend fun settleParentHelpInFull(): FinancialOperationResult = parentHelpMutex.withLock {
        val help = economyApi.getParentHelp()
            ?: return@withLock FinancialOperationResult.Rejected(
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
            repository.upsertParentHelpMessage(weekApi.initialize().absoluteDay, activeHelp = null)
        }
        result
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

    private suspend fun ensureParentHelpReminder(absoluteDay: Long) = parentHelpMutex.withLock {
        val activeHelp = economyApi.getParentHelp()
        repository.upsertParentHelpMessage(absoluteDay, activeHelp)
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
