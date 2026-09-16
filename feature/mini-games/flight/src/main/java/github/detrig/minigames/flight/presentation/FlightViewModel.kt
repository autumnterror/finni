package github.detrig.minigames.flight.presentation

import github.detrig.core.mvvm.CoreViewModel
import github.detrig.core.mvvm.ExceptionConsumer
import github.detrig.minigames.flight.domain.*
import github.detrig.minigames.flight.navigation.FlightRouter
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class FlightViewModel(
    val engine: FlightEngine,
    private val interactor: FlightInteractor,
    private val router: FlightRouter,
    private val now: () -> Long,
) : CoreViewModel<FlightViewState, FlightViewEvent>(FlightViewState()) {
    private val mutableFrames = MutableStateFlow<FlightSession?>(null)
    val frames = mutableFrames.asStateFlow()
    private val mutableRender = MutableStateFlow(FlightRenderFrame())
    val renderFrames = mutableRender.asStateFlow()
    private val clock = FlightFrameClock(engine.config.physics.tickRate, engine.config.physics.maxFrameMillis)
    private var operation: Job? = null
    private var checkpointJob: Job? = null
    private var retry: (() -> Unit)? = null
    private var lastCheckpointTick = 0
    private var exitRequested = false
    val countdownGeneration get() = stateData.countdownGeneration

    override fun perform(viewEvent: FlightViewEvent) {
        when (viewEvent) {
            FlightViewEvent.Load -> if (stateData.page == FlightPage.LOADING) load()
            FlightViewEvent.Start -> if (stateData.page == FlightPage.RESULTS) begin()
            FlightViewEvent.Flap -> flap()
            is FlightViewEvent.Frame -> frame(viewEvent)
            is FlightViewEvent.CountdownTick -> countdown(viewEvent)
            is FlightViewEvent.Foreground -> foreground(viewEvent.active)
            FlightViewEvent.Back -> if (stateData.page == FlightPage.RECORDS)
                updateState { copy(page = FlightPage.RESULTS) } else exit()
            FlightViewEvent.Exit -> exit()
            FlightViewEvent.Records -> if (stateData.page == FlightPage.RESULTS)
                updateState { copy(page = FlightPage.RECORDS) }
            FlightViewEvent.Retry -> if (stateData.page == FlightPage.ERROR) retry?.invoke()
            FlightViewEvent.RetryEffect -> deliverEffects()
            is FlightViewEvent.Settings -> {
                interactor.host.saveFeedbackSettings(viewEvent.value)
                updateState { copy(settings = viewEvent.value) }
            }
        }
    }

    private fun runOperation(action: suspend () -> Unit) {
        if (operation?.isActive == true) return
        retry = { runOperation(action) }
        updateState { copy(page = FlightPage.SAVING) }
        operation = launchCoroutine(ExceptionConsumer {
            clock.reset()
            updateState { copy(page = FlightPage.ERROR) }
            if (exitRequested) router.back()
            true
        }) {
            action()
            if (exitRequested) router.back()
        }
    }

    private fun load() = runOperation {
        val environment = interactor.host.environment()
        updateState { copy(environment = environment, settings = interactor.host.feedbackSettings()) }
        if (!environment.unlocked) {
            updateState { copy(page = FlightPage.LOCKED) }
            return@runOperation
        }
        var progress = interactor.repository.load(environment.profileId)
        if (progress.active?.rulesVersion?.let { it != engine.config.rulesVersion } == true)
            progress = interactor.repository.interruptIncompatible(environment.profileId)
        val ended = progress.active?.takeIf { it.outcome != null }
        if (ended != null) progress = interactor.repository.finish(ended, now())
        progress = tryDeliver(progress)
        // Даже при восстановлении завершённой попытки вход из комнаты ведёт в новый полёт.
        val session = progress.active ?: engine.create(UUID.randomUUID().toString(), environment.profileId,
            environment.appearanceId, UUID.randomUUID().hashCode(), now())
        progress = interactor.repository.begin(session)
        updateState { copy(progress = progress, effectPending = progress.pendingEffects.isNotEmpty()) }
        prepareCountdown(session)
    }

    private fun begin() {
        if (operation?.isActive == true) return
        val environment = stateData.environment ?: return
        val session = engine.create(UUID.randomUUID().toString(), environment.profileId,
            environment.appearanceId, UUID.randomUUID().hashCode(), now())
        runOperation {
            val current = interactor.host.environment()
            check(current.profileId == environment.profileId && current.unlocked)
            val progress = interactor.repository.begin(session)
            updateState { copy(progress = progress) }
            prepareCountdown(session)
        }
    }

    private fun setFrame(session: FlightSession, fraction: Float = 0f) {
        mutableFrames.value = session
        mutableRender.value = FlightRenderFrame(session, fraction)
    }

    private fun prepareCountdown(session: FlightSession) {
        setFrame(session)
        lastCheckpointTick = session.tick
        clock.reset()
        updateState { copy(page = FlightPage.COUNTDOWN, score = session.score,
            countdown = engine.config.round.countdownSeconds, countdownGeneration = countdownGeneration + 1) }
    }

    private fun countdown(event: FlightViewEvent.CountdownTick) {
        if (!stateData.foreground || stateData.page != FlightPage.COUNTDOWN ||
            event.sessionId != frames.value?.id || event.generation != countdownGeneration) return
        if (stateData.countdown > 1) updateState { copy(countdown = countdown - 1) }
        else {
            setFrame(engine.launch(requireNotNull(frames.value)))
            clock.reset()
            updateState { copy(countdown = 0, page = FlightPage.PLAYING) }
        }
    }

    private fun flap() {
        if (!stateData.foreground || stateData.page != FlightPage.PLAYING) return
        val before = frames.value ?: return
        val after = engine.flap(before)
        if (after !== before) setFrame(after)
    }

    private fun frame(event: FlightViewEvent.Frame) {
        if (!stateData.foreground || stateData.page != FlightPage.PLAYING ||
            frames.value?.id != event.sessionId) return
        val batch = clock.frame(event.nanos)
        val before = frames.value ?: return
        val after = engine.advance(before, batch.ticks)
        setFrame(after, if (after.outcome == null) batch.fraction else 0f)
        if (after.score != stateData.score) {
            updateState { copy(score = after.score) }
            commands.onNext(FlightCue.GATE)
        }
        if (after.outcome != null) {
            commands.onNext(FlightCue.LAND)
            finalize(after)
        } else if (after.tick - lastCheckpointTick >= engine.config.persistence.checkpointTicks &&
            checkpointJob?.isActive != true) checkpoint(after)
    }

    private fun checkpoint(session: FlightSession) {
        lastCheckpointTick = session.tick
        checkpointJob = launchCoroutine(ExceptionConsumer {
            clock.reset()
            retry = { runOperation {
                interactor.repository.checkpoint(requireNotNull(frames.value))
                prepareCountdown(requireNotNull(frames.value))
            } }
            updateState { copy(page = FlightPage.ERROR) }
            true
        }) { interactor.repository.checkpoint(session) }
    }

    private fun finalize(session: FlightSession) = runOperation {
        checkpointJob?.join()
        interactor.repository.checkpoint(session)
        val progress = tryDeliver(interactor.repository.finish(session, now()))
        updateState { copy(page = FlightPage.RESULTS, progress = progress,
            effectPending = progress.pendingEffects.isNotEmpty()) }
    }

    private suspend fun tryDeliver(progress: FlightProgress): FlightProgress = try {
        interactor.deliverEffects(progress.profileId)
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: Exception) { progress }

    private fun deliverEffects() {
        if (stateData.page != FlightPage.RESULTS) return
        val progress = stateData.progress ?: return
        runOperation {
            val delivered = tryDeliver(progress)
            updateState { copy(progress = delivered, effectPending = delivered.pendingEffects.isNotEmpty(),
                page = FlightPage.RESULTS) }
        }
    }

    private fun foreground(active: Boolean) {
        if (stateData.foreground == active) return
        updateState { copy(foreground = active) }
        clock.reset()
        if (!active && stateData.page in listOf(FlightPage.COUNTDOWN, FlightPage.PLAYING)) {
            val session = frames.value ?: return
            prepareCountdown(session)
            // Запись идёт независимо от отсчёта; поздний снимок не откатывает более новый.
            val previous = checkpointJob
            checkpointJob = launchCoroutine(ExceptionConsumer {
                retry = { runOperation {
                    interactor.repository.checkpoint(requireNotNull(frames.value))
                    prepareCountdown(requireNotNull(frames.value))
                } }
                updateState { copy(page = FlightPage.ERROR) }
                true
            }) {
                previous?.join()
                interactor.repository.checkpoint(session)
            }
        }
    }

    private fun exit() {
        clock.reset()
        if (operation?.isActive == true) { exitRequested = true; return }
        if (stateData.page == FlightPage.ERROR) { router.back(); return }
        val session = frames.value
        if (session != null && session.outcome == null) {
            exitRequested = true
            runOperation {
                checkpointJob?.join()
                interactor.repository.checkpoint(session)
            }
        } else router.back()
    }
}