package github.detrig.feature.phone.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import github.detrig.core.exception.CoreErrorHandler
import github.detrig.core.exception.mapper.CoreExceptionMapper
import github.detrig.core.infrastructure.network.NetworkManager
import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.gamestate.api.GameStateApi
import github.detrig.feature.pet.api.PetApi
import github.detrig.feature.phone.navigation.PhoneRouter
import github.detrig.feature.week.api.WeekApi
import github.detrig.feature.week.domain.EarlyWeekEndResult
import github.detrig.feature.week.domain.EndDayResult
import github.detrig.feature.week.domain.WeekState
import java.lang.reflect.Proxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DebugMenuViewModelTest {
    @get:Rule
    val liveData = InstantTaskExecutorRule()

    private lateinit var weekApi: FakeWeekApi

    @Before
    fun setUp() {
        CoreErrorHandler.init(
            handler = { throw it },
            exceptionMapper = CoreExceptionMapper(object : NetworkManager {
                override fun isNetworkAvailable() = true
            }),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun endWeekJumpsToSundayWithoutReplayingBedActions() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        weekApi = FakeWeekApi(absoluteDay = 3)
        val viewModel = DebugMenuViewModel(
            economyApi = unusedApi(),
            weekApi = weekApi,
            gameStateApi = unusedApi(),
            resetDemoProgress = {},
            petApi = unusedApi(),
            router = object : PhoneRouter {
                override fun open() = Unit
                override fun openApp(appId: String) = Unit
                override fun back() = Unit
                override fun close() = Unit
            },
            createSecuritySituation = { true },
            createWishSituation = { true },
        )

        viewModel.perform(DebugMenuViewEvent.EndWeek)
        advanceUntilIdle()

        assertEquals(7L, weekApi.current.absoluteDay)
        assertEquals(1, weekApi.debugSkipCalls)
        assertEquals(0, weekApi.endDayCalls)
        assertNull(viewModel.state().value?.errorMessage)
        assertEquals(
            "Воскресенье. Вернитесь в комнату и нажмите на кровать, чтобы увидеть итоги недели.",
            viewModel.state().value?.statusMessage,
        )
    }

    @Test
    fun randomSituationOpensWishInRoom() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val router = RecordingRouter()
        var wishCreated = false
        val viewModel = debugViewModel(
            router = router,
            createSecuritySituation = { error("Security was not selected") },
            createWishSituation = { wishCreated = true; true },
            randomBoolean = { false },
        )

        viewModel.perform(DebugMenuViewEvent.TriggerRandomSituation)
        advanceUntilIdle()

        assertTrue(wishCreated)
        assertEquals(1, router.closeCalls)
        assertNull(router.openedAppId)
        assertFalse(viewModel.state().value!!.isTriggeringSituation)
    }

    @Test
    fun randomSituationFallsBackToSecurityWhenWishIsUnavailable() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val router = RecordingRouter()
        var securityCreated = false
        val viewModel = debugViewModel(
            router = router,
            createSecuritySituation = { securityCreated = true; true },
            createWishSituation = { false },
            randomBoolean = { false },
        )

        viewModel.perform(DebugMenuViewEvent.TriggerRandomSituation)
        advanceUntilIdle()

        assertTrue(securityCreated)
        assertEquals("messages", router.openedAppId)
        assertEquals(0, router.closeCalls)
    }

    private fun debugViewModel(
        router: PhoneRouter,
        createSecuritySituation: suspend () -> Boolean,
        createWishSituation: suspend () -> Boolean,
        randomBoolean: () -> Boolean,
    ) = DebugMenuViewModel(
        economyApi = unusedApi(),
        weekApi = unusedApi(),
        gameStateApi = unusedApi(),
        resetDemoProgress = {},
        petApi = unusedApi(),
        router = router,
        createSecuritySituation = createSecuritySituation,
        createWishSituation = createWishSituation,
        randomBoolean = randomBoolean,
    )

    private class RecordingRouter : PhoneRouter {
        var openedAppId: String? = null
        var closeCalls: Int = 0

        override fun open() = Unit
        override fun openApp(appId: String) { openedAppId = appId }
        override fun back() = Unit
        override fun close() { closeCalls++ }
    }

    private inline fun <reified T> unusedApi(): T = Proxy.newProxyInstance(
        T::class.java.classLoader,
        arrayOf(T::class.java),
    ) { _, method, _ -> error("Unexpected ${T::class.java.simpleName}.${method.name} call") } as T

    private class FakeWeekApi(absoluteDay: Long) : WeekApi {
        private val state = MutableStateFlow(WeekState(absoluteDay))
        val current: WeekState get() = state.value
        var debugSkipCalls = 0
            private set
        var endDayCalls = 0
            private set

        override suspend fun initialize() = current
        override fun observeState(): Flow<WeekState> = state
        override suspend fun endDay(expectedAbsoluteDay: Long): EndDayResult {
            endDayCalls++
            error("Debug end-week must not replay regular bed actions")
        }
        override suspend fun skipToSundayForDebug(): WeekState {
            debugSkipCalls++
            return current.copy(absoluteDay = current.lastDayAbsoluteDay).also { state.value = it }
        }
        override suspend fun endWeekEarlyWithParentHelp(
            expectedAbsoluteDay: Long,
            minimumRequiredBalanceRub: Long,
        ): EarlyWeekEndResult = error("Unused")
    }
}
