package github.detrig.minigames.flight

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import github.detrig.core.exception.CoreErrorHandler
import github.detrig.core.exception.mapper.CoreExceptionMapper
import github.detrig.core.infrastructure.network.NetworkManager
import github.detrig.minigames.flight.api.*
import github.detrig.minigames.flight.domain.*
import github.detrig.minigames.flight.navigation.FlightRouter
import github.detrig.minigames.flight.presentation.*
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FlightViewModelTest {
    @get:Rule val instant = InstantTaskExecutorRule()
    private val dispatcher = StandardTestDispatcher()
    private lateinit var model: FlightViewModel
    private lateinit var repository: MemoryRepository
    private var exits = 0
    private val config = FlightConfig.decode(File("src/main/assets/flight_balance.json").readText())
    private val host = object : FlightHost {
        override suspend fun environment() = FlightEnvironment("p", true, "pet")
        override suspend fun applyPlayEffect(completion: FlightCompletion) = 0
        override fun feedbackSettings() = FlightFeedbackSettings()
        override fun saveFeedbackSettings(settings: FlightFeedbackSettings) {}
    }
    private fun createModel() = FlightViewModel(FlightEngine(config), FlightInteractor(repository, host),
        object : FlightRouter { override fun open() {}; override fun back() { exits++ } }, { 123 })

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        CoreErrorHandler.init(handler = { throw it }, exceptionMapper = CoreExceptionMapper(
            object : NetworkManager { override fun isNetworkAvailable() = true }))
        repository = MemoryRepository()
        model = createModel()
        event(FlightViewEvent.Load)
    }
    @After fun cleanup() { Dispatchers.resetMain() }
    private fun event(event: FlightViewEvent) { model.perform(event); dispatcher.scheduler.runCurrent() }
    private fun state() = model.state().value!!
    private fun countdown() {
        val id = model.frames.value!!.id
        repeat(3) { event(FlightViewEvent.CountdownTick(id, model.countdownGeneration)) }
    }

    @Test fun firstEntryGoesDirectlyToThreeSecondCountdownAndLaunchesWithoutTap() {
        assertFalse(repository.progress.tutorialSeen)
        assertEquals(FlightPage.COUNTDOWN, state().page)
        assertEquals(3, state().countdown)
        assertFalse(model.frames.value!!.started)
        countdown()
        assertEquals(FlightPage.PLAYING, state().page)
        assertTrue(model.frames.value!!.started)
        assertEquals(0, model.frames.value!!.flapCount)
        val id = model.frames.value!!.id
        event(FlightViewEvent.Frame(id, 0))
        event(FlightViewEvent.Frame(id, 100_000_000))
        assertEquals(12, model.frames.value!!.tick)
    }

    @Test fun doubleLoadStartAndOldCountdownDoNotCreateAnotherAttempt() {
        val id = model.frames.value!!.id
        event(FlightViewEvent.Load)
        event(FlightViewEvent.Start)
        assertEquals(1, repository.begins)
        assertEquals(id, model.frames.value!!.id)
        event(FlightViewEvent.CountdownTick("wrong", model.countdownGeneration))
        assertEquals(3, state().countdown)
    }

    @Test fun backgroundFreezesWorldAndReturningAutomaticallyCountsDown() {
        countdown()
        val id = model.frames.value!!.id
        event(FlightViewEvent.Frame(id, 0))
        event(FlightViewEvent.Frame(id, 100_000_000))
        val generation = model.countdownGeneration
        event(FlightViewEvent.Foreground(false))
        val snapshot = model.frames.value
        event(FlightViewEvent.Frame(id, 10_000_000_000))
        event(FlightViewEvent.CountdownTick(id, model.countdownGeneration))
        event(FlightViewEvent.Flap)
        assertEquals(snapshot, model.frames.value)
        assertEquals(3, state().countdown)
        event(FlightViewEvent.Foreground(true))
        event(FlightViewEvent.CountdownTick(id, generation))
        assertEquals(3, state().countdown)
        countdown()
        assertEquals(FlightPage.PLAYING, state().page)
        assertEquals(snapshot, model.frames.value)
        event(FlightViewEvent.Frame(id, 100_000_000_000))
        assertEquals(snapshot, model.frames.value)
    }

    @Test fun countdownDoesNotConsumeTimeOrAcceptInput() {
        val snapshot = model.frames.value
        event(FlightViewEvent.Frame(snapshot!!.id, 1_000_000_000))
        event(FlightViewEvent.Flap)
        assertEquals(snapshot, model.frames.value)
    }

    @Test fun stalledFrameDoesNotOpenMenuOrSimulateWholeStall() {
        countdown()
        val id = model.frames.value!!.id
        event(FlightViewEvent.Frame(id, 0))
        event(FlightViewEvent.Frame(id, 2_000_000_000))
        assertEquals(FlightPage.PLAYING, state().page)
        assertEquals(12, model.frames.value!!.tick)
    }

    @Test fun backExitsImmediatelyAndKeepsActiveSnapshot() {
        countdown()
        event(FlightViewEvent.Flap)
        event(FlightViewEvent.Back)
        assertEquals(1, exits)
        assertEquals(model.frames.value, repository.progress.active)
    }

    @Test fun restoredSessionUsesAutomaticThreeSecondCountdown() {
        val saved = FlightEngine(config).create("restore", "p", "pet", 44, 0).copy(started = true, tick = 25)
        repository.progress = repository.progress.copy(active = saved)
        val restored = createModel()
        restored.perform(FlightViewEvent.Load)
        dispatcher.scheduler.runCurrent()
        assertEquals(FlightPage.COUNTDOWN, restored.state().value!!.page)
        assertEquals(3, restored.state().value!!.countdown)
        assertEquals(saved, restored.frames.value)
    }

    @Test fun backgroundDuringLoadingDoesNotStartHiddenGame() {
        val restored = createModel()
        restored.perform(FlightViewEvent.Load)
        restored.perform(FlightViewEvent.Foreground(false))
        dispatcher.scheduler.runCurrent()
        repeat(3) { restored.perform(FlightViewEvent.CountdownTick(restored.frames.value!!.id, restored.countdownGeneration)) }
        assertFalse(restored.state().value!!.foreground)
        assertEquals(3, restored.state().value!!.countdown)
        assertFalse(restored.frames.value!!.started)
    }

    @Test fun failedBackgroundSaveCanExitWithoutRepeatingFailedWrite() {
        countdown()
        val saved = repository.progress.active
        event(FlightViewEvent.Flap)
        repository.failCheckpoint = true
        event(FlightViewEvent.Foreground(false))
        assertEquals(FlightPage.ERROR, state().page)
        assertEquals(saved, repository.progress.active)
        event(FlightViewEvent.Exit)
        assertEquals(1, exits)
        assertEquals(1, repository.checkpoints)
    }

    @Test fun lossAndDoubleRetryStartOnlyOneNewRound() {
        countdown()
        val id = model.frames.value!!.id
        event(FlightViewEvent.Frame(id, 0))
        repeat(25) { event(FlightViewEvent.Frame(id, (it + 1) * 100_000_000L)) }
        assertEquals(FlightPage.RESULTS, state().page)
        assertNull(repository.progress.active)
        event(FlightViewEvent.Start)
        val nextId = model.frames.value!!.id
        event(FlightViewEvent.Start)
        event(FlightViewEvent.Frame(id, 9_000_000_000))
        assertNotEquals(id, nextId)
        assertEquals(2, repository.begins)
        assertEquals(0, model.frames.value!!.tick)
        assertEquals(FlightPage.COUNTDOWN, state().page)
    }

    private inner class MemoryRepository : FlightRepository {
        var progress = FlightProgress(profileId = "p")
        var begins = 0
        var checkpoints = 0
        var failCheckpoint = false
        override suspend fun load(profileId: String) = progress
        override suspend fun begin(session: FlightSession): FlightProgress {
            begins++; progress = progress.copy(active = session); return progress
        }
        override suspend fun checkpoint(session: FlightSession) {
            checkpoints++
            check(!failCheckpoint) { "Disk unavailable" }
            progress = progress.copy(active = session)
        }
        override suspend fun finish(session: FlightSession, now: Long): FlightProgress {
            progress = progress.finish(session, config, now); return progress
        }
        override suspend fun abandon(profileId: String, sessionId: String): FlightProgress {
            progress = progress.copy(active = null); return progress
        }
        override suspend fun markTutorialSeen(profileId: String): FlightProgress {
            progress = progress.copy(tutorialSeen = true); return progress
        }
        override suspend fun acknowledgeEffect(profileId: String, sessionId: String, delta: Int): FlightProgress {
            progress = progress.copy(pendingEffects = progress.pendingEffects.filterNot { it.sessionId == sessionId })
            return progress
        }
        override suspend fun interruptIncompatible(profileId: String) = progress.copy(active = null).also { progress = it }
    }
}
