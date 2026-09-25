package github.detrig.feature.phone.data

import github.detrig.feature.phone.domain.MessageKind
import github.detrig.feature.phone.domain.MessageSenderId
import github.detrig.feature.phone.domain.SecurityEventConfig
import github.detrig.feature.phone.domain.SecurityResponseChoice
import github.detrig.feature.phone.domain.StoredMessagesState
import github.detrig.feature.phone.domain.securityPenaltyRub
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersistentMessagesRepositoryTest {

    @Test
    fun schedulesAtMostOneEventForTheSameDay() = runTest {
        val repository = PersistentMessagesRepository(InMemoryMessagesStore())
        val config = SecurityEventConfig(dailyProbability = 1.0)

        repository.ensureEventForDay(1, config)
        repository.ensureEventForDay(1, config)

        val inbox = repository.observeInbox().value
        assertEquals(2, inbox.threads.sumOf { it.messages.size })
        assertEquals(2, inbox.unreadCount)
    }

    @Test
    fun zeroProbabilityStillMarksDayAsProcessed() = runTest {
        val repository = PersistentMessagesRepository(InMemoryMessagesStore())

        repository.ensureEventForDay(1, SecurityEventConfig(dailyProbability = 0.0))
        repository.ensureEventForDay(1, SecurityEventConfig(dailyProbability = 1.0))

        assertEquals(0, repository.observeInbox().value.unreadCount)
    }

    @Test
    fun firstRoomPromptDoesNotReturnForLaterEvents() = runTest {
        val repository = PersistentMessagesRepository(InMemoryMessagesStore())
        val config = SecurityEventConfig(dailyProbability = 1.0)

        repository.ensureEventForDay(1, config)
        assertTrue(repository.observeInbox().value.firstRoomPromptPending)
        repository.consumeFirstRoomPrompt()
        repository.ensureEventForDay(2, config)

        assertFalse(repository.observeInbox().value.firstRoomPromptPending)
    }

    @Test
    fun scammerMessagesAreClearedWhenTheNextDayIsProcessed() = runTest {
        val repository = PersistentMessagesRepository(InMemoryMessagesStore())

        repository.ensureEventForDay(1, SecurityEventConfig(dailyProbability = 1.0))
        repository.ensureEventForDay(2, SecurityEventConfig(dailyProbability = 0.0))

        val inbox = repository.observeInbox().value
        assertEquals(1, inbox.threads.first { it.senderId == MessageSenderId.BANK }.messages.size)
        assertTrue(inbox.threads.first { it.senderId == MessageSenderId.UNKNOWN_1 }.messages.isEmpty())
        assertTrue(inbox.threads.first { it.senderId == MessageSenderId.UNKNOWN_2 }.messages.isEmpty())
    }

    @Test
    fun dailyCleanupKeepsOnlyTheCurrentScammerMessage() = runTest {
        val repository = PersistentMessagesRepository(InMemoryMessagesStore())
        val config = SecurityEventConfig(dailyProbability = 1.0)

        repository.ensureEventForDay(1, config)
        repository.ensureEventForDay(2, config)

        val scammerMessages = repository.observeInbox().value.threads
            .filter { it.senderId != MessageSenderId.BANK }
            .flatMap { it.messages }
        assertEquals(1, scammerMessages.size)
        assertEquals(2L, scammerMessages.single().absoluteDay)
    }

    @Test
    fun safeResponseStaysPendingUntilLearningDeliveryIsConfirmed() = runTest {
        val repository = PersistentMessagesRepository(InMemoryMessagesStore())
        repository.ensureEventForDay(1, SecurityEventConfig(dailyProbability = 1.0))
        val event = repository.observeInbox().value.threads
            .first { it.senderId != MessageSenderId.BANK && it.latestEvent != null }
            .latestEvent!!

        repository.respond(event.id, SecurityResponseChoice.KEEP_CODE_SECRET)
        repository.respond(event.id, SecurityResponseChoice.SHARE_CODE)

        val pending = repository.pendingSafeResponses()
        assertEquals(listOf(event.id), pending.map { it.id })
        repository.markLearningDelivered(event.id)
        assertTrue(repository.pendingSafeResponses().isEmpty())
    }

    @Test
    fun unsafeResponseStaysPendingUntilPenaltyIsApplied() = runTest {
        val repository = PersistentMessagesRepository(InMemoryMessagesStore())
        repository.ensureEventForDay(1, SecurityEventConfig(dailyProbability = 1.0))
        val event = repository.observeInbox().value.threads
            .first { it.senderId != MessageSenderId.BANK && it.latestEvent != null }
            .latestEvent!!

        repository.respond(event.id, SecurityResponseChoice.SHARE_CODE)
        assertEquals(listOf(event.id), repository.pendingUnsafeResponses().map { it.id })

        val prepared = repository.preparePenalty(event.id, amountRub = 80)
        assertEquals(80L, prepared?.penaltyRub)
        repository.markPenaltyApplied(event.id)

        assertTrue(repository.pendingUnsafeResponses().isEmpty())
        val resolved = repository.observeInbox().value.threads
            .first { it.senderId == event.senderId }
            .latestEvent!!
        assertTrue(resolved.penaltyApplied)
        assertEquals(80L, resolved.penaltyRub)
    }

    @Test
    fun penaltyIsTwentyPercentOfAvailableBalance() {
        assertEquals(0L, securityPenaltyRub(0))
        assertEquals(80L, securityPenaltyRub(400))
        assertEquals(99L, securityPenaltyRub(499))
    }

    @Test
    fun parentHelpReminderComesFromMomAndIsRemovedAfterHelpIsAccepted() = runTest {
        val repository = PersistentMessagesRepository(InMemoryMessagesStore())

        repository.addParentHelpReminder(absoluteDay = 7)
        repository.addParentHelpReminder(absoluteDay = 8)

        val reminder = repository.observeInbox().value.threads
            .single { it.senderId == MessageSenderId.MOM }
            .messages.single()
        assertEquals(MessageSenderId.MOM, reminder.senderId)
        assertEquals(7L, reminder.absoluteDay)

        repository.removeParentHelpReminder()

        assertTrue(repository.observeInbox().value.threads
            .single { it.senderId == MessageSenderId.MOM }
            .messages.isEmpty())

        repository.addParentHelpReminder(absoluteDay = 9)
        val renewedReminder = repository.observeInbox().value.threads
            .single { it.senderId == MessageSenderId.MOM }
            .messages.single()
        assertEquals(MessageKind.PARENT_HELP_OFFER, renewedReminder.kind)
        assertEquals(9L, renewedReminder.absoluteDay)
    }

    @Test
    fun parentHelpReminderSurvivesDailyMessageCleanup() = runTest {
        val repository = PersistentMessagesRepository(InMemoryMessagesStore())
        repository.addParentHelpReminder(absoluteDay = 7)

        repository.ensureEventForDay(8, SecurityEventConfig(dailyProbability = 0.0))

        val momThread = repository.observeInbox().value.threads
            .single { it.senderId == MessageSenderId.MOM }
        assertEquals(1, momThread.messages.size)
        assertEquals(7L, momThread.messages.single().absoluteDay)
    }

    @Test
    fun activeParentHelpMessageSurvivesUntilRepaymentIsClosed() = runTest {
        val repository = PersistentMessagesRepository(InMemoryMessagesStore())
        repository.addParentHelpReminder(absoluteDay = 7)
        repository.removeParentHelpReminder()
        repository.addParentHelpRepaymentMessage(absoluteDay = 7)
        repository.addParentHelpRepaymentMessage(absoluteDay = 8)

        repository.ensureEventForDay(8, SecurityEventConfig(dailyProbability = 0.0))

        val message = repository.observeInbox().value.threads
            .single { it.senderId == MessageSenderId.MOM }
            .messages.single()
        assertEquals(MessageKind.PARENT_HELP_REPAYMENT, message.kind)
        assertEquals(7L, message.absoluteDay)

        repository.removeParentHelpRepaymentMessage()
        assertTrue(repository.observeInbox().value.threads
            .single { it.senderId == MessageSenderId.MOM }
            .messages.isEmpty())
    }

    private class InMemoryMessagesStore : MessagesStore {
        private var state = StoredMessagesState()

        override fun load(): StoredMessagesState = state

        override fun save(state: StoredMessagesState) {
            this.state = state
        }
    }
}
