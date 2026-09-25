package github.detrig.feature.phone.domain

import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.economy.domain.EconomyState
import github.detrig.feature.economy.domain.ParentHelpOffer
import github.detrig.feature.economy.domain.ParentHelpState
import github.detrig.feature.economy.domain.ParentHelpRequestResult
import github.detrig.feature.economy.domain.PeriodicIncome
import github.detrig.feature.learning.api.LearningApi
import github.detrig.feature.gamestate.api.ProgressionApi
import github.detrig.feature.learning.domain.LearningAction
import github.detrig.feature.learning.domain.ParentProgressRow
import github.detrig.feature.learning.domain.AchievementProgress
import github.detrig.feature.learning.domain.PendingXpReward
import github.detrig.feature.learning.domain.RecordLearningResult
import github.detrig.feature.learning.domain.XpDeliveryResult
import github.detrig.feature.phone.data.MessagesStore
import github.detrig.feature.phone.data.PersistentMessagesRepository
import github.detrig.feature.week.api.WeekApi
import github.detrig.feature.week.domain.EarlyWeekEndResult
import github.detrig.feature.week.domain.EndDayResult
import github.detrig.feature.week.domain.WeekState
import java.lang.reflect.Proxy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MessagesCoordinatorParentHelpTest {

    @Test
    fun `mom message persists when wallet crosses help threshold`() = runTest {
        val economyState = MutableStateFlow(economyState(availableRub = 500, savingsRub = 0))
        val repository = PersistentMessagesRepository(InMemoryMessagesStore())
        val coordinator = MessagesCoordinator(
            repository = repository,
            weekApi = weekApi(absoluteDay = 3),
            learningApi = learningApi(),
            progressionApi = unusedProgressionApi(),
            economyApi = economyApi(economyState),
            minimumHelpBalanceRub = 100,
            eventConfig = SecurityEventConfig(dailyProbability = 0.0),
        )

        coordinator.start(backgroundScope)
        runCurrent()
        assertEquals(1, momMessages(repository).count { it.kind == MessageKind.PARENT_HELP_OFFER })
        assertTrue(coordinator.parentHelpOffers().isEmpty())

        economyState.value = economyState(availableRub = 20, savingsRub = 0)
        runCurrent()

        val momMessages = repository.observeInbox().value.threads
            .single { it.senderId == MessageSenderId.MOM }
            .messages
        assertEquals(1, momMessages.count { it.kind == MessageKind.PARENT_HELP_OFFER })
        assertEquals(1, coordinator.parentHelpOffers().size)
    }

    @Test
    fun `mom message remains while savings make offers unavailable`() = runTest {
        val economyState = MutableStateFlow(economyState(availableRub = 20, savingsRub = 100))
        val repository = PersistentMessagesRepository(InMemoryMessagesStore())
        val coordinator = MessagesCoordinator(
            repository = repository,
            weekApi = weekApi(absoluteDay = 3),
            learningApi = learningApi(),
            progressionApi = unusedProgressionApi(),
            economyApi = economyApi(economyState),
            minimumHelpBalanceRub = 100,
            eventConfig = SecurityEventConfig(dailyProbability = 0.0),
        )

        coordinator.start(backgroundScope)
        runCurrent()

        assertEquals(1, momMessages(repository).count { it.kind == MessageKind.PARENT_HELP_OFFER })
        assertTrue(coordinator.parentHelpOffers().isEmpty())
    }

    @Test
    fun `mom message stays visible and offers help when savings are below minimum`() = runTest {
        val economyState = MutableStateFlow(economyState(availableRub = 20, savingsRub = 1))
        val repository = PersistentMessagesRepository(InMemoryMessagesStore())
        val coordinator = MessagesCoordinator(
            repository = repository,
            weekApi = weekApi(absoluteDay = 3),
            learningApi = learningApi(),
            progressionApi = unusedProgressionApi(),
            economyApi = economyApi(economyState),
            minimumHelpBalanceRub = 100,
            eventConfig = SecurityEventConfig(dailyProbability = 0.0),
        )

        coordinator.start(backgroundScope)
        runCurrent()

        assertEquals(1, momMessages(repository).count { it.kind == MessageKind.PARENT_HELP_OFFER })
        assertEquals(1, coordinator.parentHelpOffers().size)
    }

    @Test
    fun `mom message stays visible while economy has outstanding repayments`() = runTest {
        val economyState = MutableStateFlow(economyState(availableRub = 20, savingsRub = 0, debtRub = 1))
        val repository = PersistentMessagesRepository(InMemoryMessagesStore())
        val coordinator = MessagesCoordinator(
            repository = repository,
            weekApi = weekApi(absoluteDay = 3),
            learningApi = learningApi(),
            progressionApi = unusedProgressionApi(),
            economyApi = economyApi(economyState),
            minimumHelpBalanceRub = 100,
            eventConfig = SecurityEventConfig(dailyProbability = 0.0),
        )

        coordinator.start(backgroundScope)
        runCurrent()

        assertEquals(1, momMessages(repository).count { it.kind == MessageKind.PARENT_HELP_OFFER })
        assertTrue(coordinator.parentHelpOffers().isEmpty())
    }

    @Test
    fun `mom message stays in place when savings become nonzero`() = runTest {
        val economyState = MutableStateFlow(economyState(availableRub = 20, savingsRub = 0))
        val repository = PersistentMessagesRepository(InMemoryMessagesStore())
        val coordinator = MessagesCoordinator(
            repository = repository,
            weekApi = weekApi(absoluteDay = 3),
            learningApi = learningApi(),
            progressionApi = unusedProgressionApi(),
            economyApi = economyApi(economyState),
            minimumHelpBalanceRub = 100,
            eventConfig = SecurityEventConfig(dailyProbability = 0.0),
        )

        coordinator.start(backgroundScope)
        runCurrent()
        assertEquals(1, repository.observeInbox().value.threads
            .flatMap { it.messages }
            .count { it.kind == MessageKind.PARENT_HELP_OFFER })

        economyState.value = economyState(availableRub = 20, savingsRub = 100)
        runCurrent()

        assertEquals(1, repository.observeInbox().value.threads
            .flatMap { it.messages }
            .count { it.kind == MessageKind.PARENT_HELP_OFFER })
        assertTrue(coordinator.parentHelpOffers().isEmpty())
    }

    @Test
    fun `help request is allowed when savings cannot cover the important purchase`() = runTest {
        val economyState = MutableStateFlow(economyState(availableRub = 20, savingsRub = 1))
        val repository = PersistentMessagesRepository(InMemoryMessagesStore())
        val acceptedHelp = ParentHelpState(
            offerId = "offer",
            receivedRub = 100,
            totalRepaymentRub = 110,
            remainingRub = 110,
            paymentsRemaining = 2,
        )
        val coordinator = MessagesCoordinator(
            repository = repository,
            weekApi = weekApi(absoluteDay = 3),
            learningApi = learningApi(),
            progressionApi = unusedProgressionApi(),
            economyApi = economyApi(economyState, acceptedHelp = acceptedHelp),
            minimumHelpBalanceRub = 100,
            eventConfig = SecurityEventConfig(dailyProbability = 0.0),
        )

        assertTrue(coordinator.requestParentHelp("offer") is ParentHelpRequestResult.Accepted)
    }

    @Test
    fun `accepting help replaces the offer message with a repayment message from mom`() = runTest {
        val economyState = MutableStateFlow(economyState(availableRub = 20, savingsRub = 0))
        val repository = PersistentMessagesRepository(InMemoryMessagesStore())
        val activeHelp = ParentHelpState(
            offerId = "offer",
            receivedRub = 100,
            totalRepaymentRub = 110,
            remainingRub = 110,
            paymentsRemaining = 2,
        )
        val coordinator = MessagesCoordinator(
            repository = repository,
            weekApi = weekApi(absoluteDay = 3),
            learningApi = learningApi(),
            progressionApi = unusedProgressionApi(),
            economyApi = economyApi(economyState, acceptedHelp = activeHelp),
            minimumHelpBalanceRub = 100,
            eventConfig = SecurityEventConfig(dailyProbability = 0.0),
        )
        repository.upsertParentHelpMessage(absoluteDay = 3, activeHelp = null)

        coordinator.requestParentHelp("offer")

        val messages = repository.observeInbox().value.threads
            .single { it.senderId == MessageSenderId.MOM }
            .messages
        assertEquals(listOf(MessageKind.PARENT_HELP_REPAYMENT), messages.map { it.kind })
    }

    @Test
    fun `mom offer returns after repayments are closed and balance still needs help`() = runTest {
        val economyState = MutableStateFlow(economyState(availableRub = 20, savingsRub = 0))
        val activeHelp = MutableStateFlow<ParentHelpState?>(null)
        val repository = PersistentMessagesRepository(InMemoryMessagesStore())
        val coordinator = MessagesCoordinator(
            repository = repository,
            weekApi = weekApi(absoluteDay = 3),
            learningApi = learningApi(),
            progressionApi = unusedProgressionApi(),
            economyApi = economyApi(economyState, activeHelp = activeHelp),
            minimumHelpBalanceRub = 100,
            eventConfig = SecurityEventConfig(dailyProbability = 0.0),
        )

        coordinator.start(backgroundScope)
        runCurrent()
        assertEquals(1, momMessages(repository).count { it.kind == MessageKind.PARENT_HELP_OFFER })

        val help = ParentHelpState(
            offerId = "offer",
            receivedRub = 100,
            totalRepaymentRub = 110,
            remainingRub = 110,
            paymentsRemaining = 2,
        )
        activeHelp.value = help
        economyState.value = economyState(availableRub = 15, savingsRub = 0, debtRub = 110)
        runCurrent()
        assertEquals(1, momMessages(repository).count { it.kind == MessageKind.PARENT_HELP_REPAYMENT })

        activeHelp.value = null
        economyState.value = economyState(availableRub = 10, savingsRub = 0, debtRub = 0)
        runCurrent()

        assertEquals(1, momMessages(repository).count { it.kind == MessageKind.PARENT_HELP_OFFER })
    }

    private fun momMessages(repository: PersistentMessagesRepository) = repository.observeInbox().value.threads
        .single { it.senderId == MessageSenderId.MOM }
        .messages

    private fun economyApi(
        state: MutableStateFlow<EconomyState>,
        acceptedHelp: ParentHelpState? = null,
        activeHelp: MutableStateFlow<ParentHelpState?> = MutableStateFlow(acceptedHelp),
    ): EconomyApi =
        Proxy.newProxyInstance(
            EconomyApi::class.java.classLoader,
            arrayOf(EconomyApi::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getState", "initialize" -> state.value
                "observeState" -> state
                "getParentHelp" -> activeHelp.value
                "parentHelpOffers" -> listOf(ParentHelpOffer("offer", 100, 2, 110))
                "requestParentHelp" -> ParentHelpRequestResult.Accepted(acceptedHelp!!, state.value)
                else -> error("Unexpected EconomyApi call: ${method.name}")
            }
        } as EconomyApi

    private fun weekApi(absoluteDay: Long): WeekApi {
        val weekState = MutableStateFlow(WeekState(absoluteDay))
        return object : WeekApi {
            override suspend fun initialize() = weekState.value
            override fun observeState(): Flow<WeekState> = weekState
            override suspend fun endDay(expectedAbsoluteDay: Long): EndDayResult = error("Unused")
            override suspend fun endWeekEarlyWithParentHelp(
                expectedAbsoluteDay: Long,
                minimumRequiredBalanceRub: Long,
            ): EarlyWeekEndResult = error("Unused")
        }
    }

    private fun learningApi() = object : LearningApi {
        override suspend fun record(action: LearningAction): RecordLearningResult = error("Unused")
        override fun observeAchievements(profileId: String): Flow<List<AchievementProgress>> = flowOf(emptyList())
        override fun observeParentRows(profileId: String): Flow<List<ParentProgressRow>> = flowOf(emptyList())
        override fun observePendingXpRewards(profileId: String): Flow<List<PendingXpReward>> = flowOf(emptyList())
        override suspend fun claimFirstExplanation(profileId: String, explanationId: String): Boolean = error("Unused")
        override suspend fun deliverPendingXpRewards(profileId: String): XpDeliveryResult = error("Unused")
        override suspend fun resetProfile(profileId: String) = error("Unused")
    }

    private fun unusedProgressionApi(): ProgressionApi = Proxy.newProxyInstance(
        ProgressionApi::class.java.classLoader,
        arrayOf(ProgressionApi::class.java),
    ) { _, method, _ -> error("Unexpected ProgressionApi call: ${method.name}") } as ProgressionApi

    private fun economyState(availableRub: Long, savingsRub: Long, debtRub: Long = 0) = EconomyState(
        availableRub = availableRub,
        savingsRub = savingsRub,
        debtRub = debtRub,
        periodicIncome = PeriodicIncome(amountRub = 1, periodMillis = 1, nextAtMillis = 1),
    )

    private class InMemoryMessagesStore : MessagesStore {
        private var state = StoredMessagesState()
        override fun load() = state
        override fun save(state: StoredMessagesState) {
            this.state = state
        }
    }
}
