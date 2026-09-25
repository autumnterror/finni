package github.detrig.feature.phone.data

import github.detrig.feature.phone.domain.MessageKind
import github.detrig.feature.phone.domain.MessageSenderId
import github.detrig.feature.phone.domain.MessagesInbox
import github.detrig.feature.phone.domain.PhoneMessage
import github.detrig.feature.phone.domain.SecurityEventConfig
import github.detrig.feature.phone.domain.SecurityMessageEvent
import github.detrig.feature.phone.domain.SecurityMessageScenario
import github.detrig.feature.phone.domain.SecurityResponseChoice
import github.detrig.feature.phone.domain.StoredMessagesState
import github.detrig.feature.phone.domain.toInbox
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

internal interface MessagesRepository {
    fun observeInbox(): StateFlow<MessagesInbox>
    suspend fun ensureEventForDay(absoluteDay: Long, config: SecurityEventConfig)
    suspend fun markThreadRead(senderId: MessageSenderId)
    suspend fun consumeFirstRoomPrompt()
    suspend fun markGuidanceVisible(eventId: String)
    suspend fun acknowledgeGuidance(eventId: String)
    suspend fun respond(eventId: String, choice: SecurityResponseChoice): SecurityMessageEvent?
    suspend fun pendingSafeResponses(): List<SecurityMessageEvent>
    suspend fun markLearningDelivered(eventId: String)
    suspend fun pendingUnsafeResponses(): List<SecurityMessageEvent>
    suspend fun preparePenalty(eventId: String, amountRub: Long): SecurityMessageEvent?
    suspend fun markPenaltyApplied(eventId: String)
    suspend fun acknowledgeFeedback(eventId: String)
    suspend fun addParentHelpReminder(absoluteDay: Long) = Unit
    suspend fun removeParentHelpReminder() = Unit
    suspend fun addParentHelpRepaymentMessage(absoluteDay: Long) = Unit
    suspend fun removeParentHelpRepaymentMessage() = Unit
}

internal class PersistentMessagesRepository(
    private val store: MessagesStore,
) : MessagesRepository {

    private val mutex = Mutex()
    private var storedState = store.load()
    private val inbox = MutableStateFlow(storedState.toInbox())

    override fun observeInbox(): StateFlow<MessagesInbox> = inbox.asStateFlow()

    override suspend fun ensureEventForDay(
        absoluteDay: Long,
        config: SecurityEventConfig,
    ) = update { current ->
        require(absoluteDay >= 1)
        if (absoluteDay <= current.lastProcessedAbsoluteDay) return@update current

        val processed = current.copy(
            lastProcessedAbsoluteDay = absoluteDay,
            messages = current.messages.filter { message ->
                message.senderId == MessageSenderId.BANK || message.senderId == MessageSenderId.MOM
            },
        )
        if (dailyRoll(absoluteDay, config.randomSeed) >= config.dailyProbability) {
            return@update processed
        }

        val scenario = if (current.events.size % 2 == 0) {
            SecurityMessageScenario.CONFIRMATION_CODE
        } else {
            SecurityMessageScenario.UNKNOWN_LINK
        }
        val sender = if ((absoluteDay + current.events.size) % 2L == 0L) {
            MessageSenderId.UNKNOWN_1
        } else {
            MessageSenderId.UNKNOWN_2
        }
        val eventId = "security-event-$absoluteDay"
        val event = SecurityMessageEvent(
            id = eventId,
            absoluteDay = absoluteDay,
            scenario = scenario,
            senderId = sender,
        )
        val newMessages = when (scenario) {
            SecurityMessageScenario.CONFIRMATION_CODE -> {
                val code = confirmationCode(absoluteDay, config.randomSeed)
                listOf(
                    PhoneMessage(
                        id = "$eventId-bank",
                        eventId = eventId,
                        senderId = MessageSenderId.BANK,
                        absoluteDay = absoluteDay,
                        kind = MessageKind.BANK_CONFIRMATION_CODE,
                        payload = code,
                    ),
                    PhoneMessage(
                        id = "$eventId-request",
                        eventId = eventId,
                        senderId = sender,
                        absoluteDay = absoluteDay,
                        kind = MessageKind.REQUEST_CONFIRMATION_CODE,
                    ),
                )
            }
            SecurityMessageScenario.UNKNOWN_LINK -> listOf(
                PhoneMessage(
                    id = "$eventId-link",
                    eventId = eventId,
                    senderId = sender,
                    absoluteDay = absoluteDay,
                    kind = MessageKind.UNKNOWN_LINK,
                    payload = "https://gift.example/day-$absoluteDay",
                ),
            )
        }
        processed.copy(
            firstRoomPromptPending = current.events.isEmpty() || current.firstRoomPromptPending,
            messages = processed.messages + newMessages,
            events = current.events + event,
        )
    }

    override suspend fun markThreadRead(senderId: MessageSenderId) = update { current ->
        current.copy(
            messages = current.messages.map { message ->
                if (message.senderId == senderId) message.copy(isRead = true) else message
            },
        )
    }

    override suspend fun consumeFirstRoomPrompt() = update { current ->
        if (!current.firstRoomPromptPending) current else current.copy(firstRoomPromptPending = false)
    }

    override suspend fun markGuidanceVisible(eventId: String) = update { current ->
        current.copy(
            events = current.events.map { event ->
                if (event.id == eventId) event.copy(guidanceVisible = true) else event
            },
        )
    }

    override suspend fun acknowledgeGuidance(eventId: String) = update { current ->
        current.copy(
            events = current.events.map { event ->
                if (event.id == eventId) event.copy(guidanceAcknowledged = true) else event
            },
        )
    }

    override suspend fun respond(
        eventId: String,
        choice: SecurityResponseChoice,
    ): SecurityMessageEvent? {
        var resolved: SecurityMessageEvent? = null
        update { current ->
            val event = current.events.firstOrNull { it.id == eventId } ?: return@update current
            resolved = if (event.response == null) event.copy(response = choice) else event
            current.copy(
                events = current.events.map { candidate ->
                    if (candidate.id == eventId) checkNotNull(resolved) else candidate
                },
            )
        }
        return resolved
    }

    override suspend fun pendingSafeResponses(): List<SecurityMessageEvent> = mutex.withLock {
        storedState.events.filter { event ->
            event.response?.isSafe == true && !event.learningDelivered
        }
    }

    override suspend fun markLearningDelivered(eventId: String) = update { current ->
        current.copy(
            events = current.events.map { event ->
                if (event.id == eventId) event.copy(learningDelivered = true) else event
            },
        )
    }

    override suspend fun pendingUnsafeResponses(): List<SecurityMessageEvent> = mutex.withLock {
        storedState.events.filter { event ->
            event.response?.isSafe == false && !event.penaltyApplied
        }
    }

    override suspend fun preparePenalty(
        eventId: String,
        amountRub: Long,
    ): SecurityMessageEvent? {
        require(amountRub >= 0)
        var prepared: SecurityMessageEvent? = null
        update { current ->
            val event = current.events.firstOrNull { it.id == eventId } ?: return@update current
            prepared = if (event.penaltyRub == null) event.copy(penaltyRub = amountRub) else event
            current.copy(
                events = current.events.map { candidate ->
                    if (candidate.id == eventId) checkNotNull(prepared) else candidate
                },
            )
        }
        return prepared
    }

    override suspend fun markPenaltyApplied(eventId: String) = update { current ->
        current.copy(
            events = current.events.map { event ->
                if (event.id == eventId) event.copy(penaltyApplied = true) else event
            },
        )
    }

    override suspend fun acknowledgeFeedback(eventId: String) = update { current ->
        current.copy(
            events = current.events.map { event ->
                if (event.id == eventId) event.copy(feedbackAcknowledged = true) else event
            },
        )
    }

    override suspend fun addParentHelpReminder(absoluteDay: Long) = update { current ->
        if (current.messages.any { it.id == PARENT_HELP_MESSAGE_ID }) return@update current
        current.copy(
            parentHelpReminderCreated = true,
            messages = current.messages + PhoneMessage(
                id = PARENT_HELP_MESSAGE_ID,
                eventId = PARENT_HELP_MESSAGE_ID,
                senderId = MessageSenderId.MOM,
                absoluteDay = absoluteDay,
                kind = MessageKind.PARENT_HELP_OFFER,
            ),
        )
    }

    override suspend fun removeParentHelpReminder() = update { current ->
        current.copy(
            parentHelpReminderCreated = false,
            messages = current.messages.filterNot { it.id == PARENT_HELP_MESSAGE_ID },
        )
    }

    override suspend fun addParentHelpRepaymentMessage(absoluteDay: Long) = update { current ->
        if (current.messages.any { it.id == PARENT_HELP_REPAYMENT_MESSAGE_ID }) return@update current
        current.copy(
            messages = current.messages + PhoneMessage(
                id = PARENT_HELP_REPAYMENT_MESSAGE_ID,
                eventId = PARENT_HELP_REPAYMENT_MESSAGE_ID,
                senderId = MessageSenderId.MOM,
                absoluteDay = absoluteDay,
                kind = MessageKind.PARENT_HELP_REPAYMENT,
            ),
        )
    }

    override suspend fun removeParentHelpRepaymentMessage() = update { current ->
        current.copy(messages = current.messages.filterNot { it.id == PARENT_HELP_REPAYMENT_MESSAGE_ID })
    }

    private suspend fun update(
        transform: (StoredMessagesState) -> StoredMessagesState,
    ) {
        mutex.withLock {
            val next = transform(storedState)
            if (next == storedState) return
            store.save(next)
            storedState = next
            inbox.value = next.toInbox()
        }
    }

    private fun dailyRoll(absoluteDay: Long, seed: Int): Double =
        Random(seed xor absoluteDay.hashCode()).nextDouble()

    private fun confirmationCode(absoluteDay: Long, seed: Int): String =
        Random(seed xor absoluteDay.hashCode() xor CODE_SALT)
            .nextInt(from = 100_000, until = 1_000_000)
            .toString()

    private companion object {
        const val CODE_SALT = 0xC0DE
        const val PARENT_HELP_MESSAGE_ID = "parent-help-reminder"
        const val PARENT_HELP_REPAYMENT_MESSAGE_ID = "parent-help-repayment"
    }
}
