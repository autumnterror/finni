package github.detrig.feature.phone.domain

import kotlinx.serialization.Serializable
import github.detrig.feature.economy.domain.ParentHelpOffer
import github.detrig.feature.economy.domain.ParentHelpState

@Serializable
internal enum class MessageSenderId {
    BANK,
    MOM,
    UNKNOWN_1,
    UNKNOWN_2,
}

@Serializable
internal enum class SecurityMessageScenario {
    CONFIRMATION_CODE,
    UNKNOWN_LINK,
}

@Serializable
internal enum class MessageKind {
    BANK_CONFIRMATION_CODE,
    REQUEST_CONFIRMATION_CODE,
    UNKNOWN_LINK,
    PARENT_HELP_OFFER,
    PARENT_HELP_REPAYMENT,
}

@Serializable
internal enum class SecurityResponseChoice {
    KEEP_CODE_SECRET,
    SHARE_CODE,
    IGNORE_LINK,
    OPEN_LINK,
    ;

    val isSafe: Boolean
        get() = this == KEEP_CODE_SECRET || this == IGNORE_LINK
}

@Serializable
internal data class PhoneMessage(
    val id: String,
    val eventId: String,
    val senderId: MessageSenderId,
    val absoluteDay: Long,
    val kind: MessageKind,
    val payload: String = "",
    val isRead: Boolean = false,
)

@Serializable
internal data class SecurityMessageEvent(
    val id: String,
    val absoluteDay: Long,
    val scenario: SecurityMessageScenario,
    val senderId: MessageSenderId,
    val response: SecurityResponseChoice? = null,
    val guidanceVisible: Boolean = false,
    val guidanceAcknowledged: Boolean = false,
    val learningDelivered: Boolean = false,
    val penaltyRub: Long? = null,
    val penaltyApplied: Boolean = false,
    val feedbackAcknowledged: Boolean = false,
)

@Serializable
internal data class StoredMessagesState(
    val lastProcessedAbsoluteDay: Long = 0,
    val firstRoomPromptPending: Boolean = false,
    val parentHelpReminderCreated: Boolean = false,
    val messages: List<PhoneMessage> = emptyList(),
    val events: List<SecurityMessageEvent> = emptyList(),
)

internal data class MessageThread(
    val senderId: MessageSenderId,
    val messages: List<PhoneMessage>,
    val unreadCount: Int,
    val latestEvent: SecurityMessageEvent?,
)

internal data class MessagesInbox(
    val threads: List<MessageThread>,
    val unreadCount: Int,
    val firstRoomPromptPending: Boolean,
) {
    companion object {
        val Empty = MessagesInbox(
            threads = MessageSenderId.entries.map { sender ->
                MessageThread(sender, emptyList(), unreadCount = 0, latestEvent = null)
            },
            unreadCount = 0,
            firstRoomPromptPending = false,
        )
    }
}

internal data class ParentHelpDialogData(
    val offers: List<ParentHelpOffer>,
    val activeHelp: ParentHelpState?,
    val availableRub: Long,
)

internal data class SecurityEventConfig(
    val dailyProbability: Double,
    val randomSeed: Int = 0x51A7E,
) {
    init {
        require(dailyProbability in 0.0..1.0)
    }
}

internal fun securityPenaltyRub(availableRub: Long): Long {
    require(availableRub >= 0)
    return availableRub / SECURITY_PENALTY_DIVISOR
}

private const val SECURITY_PENALTY_DIVISOR = 5L

internal fun StoredMessagesState.toInbox(): MessagesInbox {
    val threads = MessageSenderId.entries.map { senderId ->
        val senderMessages = messages
            .filter { it.senderId == senderId }
            .sortedBy { it.absoluteDay }
        MessageThread(
            senderId = senderId,
            messages = senderMessages,
            unreadCount = senderMessages.count { !it.isRead },
            latestEvent = events
                .asSequence()
                .filter { event ->
                    event.senderId == senderId && senderMessages.any { message ->
                        message.eventId == event.id
                    }
                }
                .maxByOrNull { it.absoluteDay },
        )
    }
    return MessagesInbox(
        threads = threads,
        unreadCount = messages.count { !it.isRead },
        firstRoomPromptPending = firstRoomPromptPending,
    )
}
