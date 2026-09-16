package github.detrig.minigames.fishing.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import github.detrig.core.mvvm.CoreViewModel
import github.detrig.core.mvvm.ExceptionConsumer
import github.detrig.minigames.fishing.domain.*
import github.detrig.minigames.fishing.api.FishingFeedbackSettings
import github.detrig.minigames.fishing.navigation.FishingRouter
import java.util.UUID

internal class FishingViewModel(
    private val interactor: FishingInteractor,
    private val router: FishingRouter,
    private val now: () -> Long,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : CoreViewModel<FishingViewState, FishingViewEvent>(FishingViewState()) {
    val engine get() = interactor.engine
    // Кадровое состояние читается внутри Canvas, а не при построении всего экрана.
    var scene: FishingSession? by mutableStateOf(null)
        private set
    private var loaded = false
    private var checkpointSeconds = 0.0
    private var hudSeconds = 0.0
    private var returnPage = FishingPage.GAME
    private var retry: (() -> Unit)? = null
    private var operationKind: FishingError? = null

    override fun perform(viewEvent: FishingViewEvent) {
        when (viewEvent) {
            FishingViewEvent.Release -> { scene = scene?.let { engine.release(it) }; publish(); return }
            FishingViewEvent.CancelPress -> { scene = scene?.let { engine.release(it, cancel = true) }; publish(); return }
            FishingViewEvent.Pause -> { pause(); return }
            else -> Unit
        }
        val inputDuringCheckpoint = operationKind == FishingError.CHECKPOINT &&
            viewEvent in listOf(FishingViewEvent.Press, FishingViewEvent.Toggle, FishingViewEvent.Recast)
        if (stateData.error != null && viewEvent !in listOf(FishingViewEvent.Retry, FishingViewEvent.Exit,
                FishingViewEvent.Back, FishingViewEvent.Confirm, FishingViewEvent.Dismiss)) return
        if (stateData.busy && !inputDuringCheckpoint && viewEvent !in listOf(FishingViewEvent.Back, FishingViewEvent.Dismiss)) return
        when (viewEvent) {
            FishingViewEvent.Load -> if (!loaded) load()
            FishingViewEvent.Start -> {
                if (stateData.progress?.session != null) ask(FishingConfirmation.RESTART)
                else start(tutorial = false)
            }
            FishingViewEvent.Continue -> {
                if (scene == null) scene = stateData.progress?.session
                scene = scene?.let(engine::resume)
                updateState { copy(page = FishingPage.GAME, error = null) }
                publish()
            }
            FishingViewEvent.Tutorial -> openPage(FishingPage.PRACTICE_INFO)
            FishingViewEvent.SkipTutorial, FishingViewEvent.FinishPractice -> finishTutorial()
            FishingViewEvent.Press -> { scene = scene?.let(engine::press); publish(); checkpointTransition() }
            FishingViewEvent.Toggle -> { scene = scene?.let(engine::toggle); publish(); checkpointTransition() }
            FishingViewEvent.Recast -> { scene = scene?.let(engine::recast); publish(); checkpointTransition() }
            FishingViewEvent.Back -> back()
            FishingViewEvent.Records -> openPage(FishingPage.RECORDS)
            FishingViewEvent.DismissMigration -> operation(FishingError.SETTINGS) {
                updateProgress(interactor.dismissMigration(profileId()))
            }
            is FishingViewEvent.Preferences -> operation(FishingError.SETTINGS) {
                interactor.host.saveFeedbackSettings(FishingFeedbackSettings(viewEvent.value.sound,
                    viewEvent.value.haptics, viewEvent.value.reducedMotion))
                updateProgress(interactor.preferences(profileId(), viewEvent.value))
            }
            FishingViewEvent.Restart -> {
                ask(FishingConfirmation.RESTART)
            }
            FishingViewEvent.Exit -> if (scene != null && stateData.page == FishingPage.GAME) ask(FishingConfirmation.EXIT) else router.back()
            FishingViewEvent.Confirm -> confirm()
            FishingViewEvent.Dismiss -> updateState { copy(confirmation = null) }
            FishingViewEvent.Retry -> retry?.invoke()
            else -> Unit
        }
    }

    fun frame(seconds: Double) {
        val old = scene ?: return
        if (stateData.busy || stateData.page != FishingPage.GAME || old.paused) return
        if (!seconds.isFinite() || seconds <= 0) return
        // После системной задержки не догоняем время мгновенным обрывом лески.
        val frameSeconds = seconds.coerceAtMost(0.1)
        val next = engine.advance(old, frameSeconds)
        scene = next
        checkpointSeconds += frameSeconds
        hudSeconds += frameSeconds
        if (hudSeconds >= 0.1 || next.phase != old.phase || next.warning != old.warning) { publish(); hudSeconds = 0.0 }
        when {
            next.phase == FishingPhase.CATCH_PENDING -> {
                if (next.tutorial) { scene = engine.confirmCatch(next, now()); publish() }
                else operation(FishingError.CATCH) {
                    val p = interactor.catch(next)
                    val wasPaused = scene?.paused == true
                    scene = p.session?.let { if (wasPaused) engine.pause(it) else it }
                    updateProgress(p)
                }
            }
            next.phase == FishingPhase.FINISHED -> {
                if (next.tutorial) {
                    scene = null
                    updateState { copy(page = FishingPage.PRACTICE_INFO, hud = null) }
                } else finish(next)
            }
            !next.tutorial && (next.phase != old.phase || checkpointSeconds >= 0.75) -> checkpointTransition()
        }
    }

    private fun load() = operation(FishingError.LOAD) {
        val env = interactor.host.environment()
        if (!env.unlocked) {
            loaded = true
            updateState { copy(loading = false, environment = env) }
            interactor.host.showUnlockPreview()
            return@operation
        }
        val p = interactor.load(env.profileId)
        val feedback = interactor.host.feedbackSettings()
        val preferences = p.preferences.copy(sound = feedback.sound, haptics = feedback.haptics, reducedMotion = feedback.reducedMotion)
        val current = if (preferences == p.preferences) p else interactor.preferences(env.profileId, preferences)
        val existing = current.session
        val prepared = existing ?: engine.create(newId(), env.profileId, now(), now())
        val progress = if (existing == null) interactor.start(prepared) else current
        scene = when {
            existing == null || existing.phase == FishingPhase.FINISHED -> prepared
            existing.phase == FishingPhase.COUNTDOWN -> existing.copy(paused = false, phaseSeconds = 0.0, resumeSeconds = 0.0)
            else -> engine.resume(existing).copy(resumeSeconds = engine.config.round.countdownSeconds)
        }
        loaded = true
        updateState { copy(loading = false, environment = env, progress = progress,
            page = FishingPage.GAME, hud = scene) }
        if (existing?.phase == FishingPhase.FINISHED) finishOnLoad = true
        effectsOnLoad = progress.pendingEffects.isNotEmpty()
    }
    private var finishOnLoad = false
    private var effectsOnLoad = false

    private fun start(tutorial: Boolean) {
        if (stateData.environment?.unlocked != true) return
        val s = engine.create(newId(), profileId(), now(), now(), tutorial)
        operation(FishingError.CHECKPOINT) {
            if (!tutorial) updateProgress(interactor.start(s))
            scene = s
            checkpointSeconds = 0.0
            updateState { copy(page = FishingPage.GAME, tutorialJustFinished = false, confirmation = null) }
            publish()
        }
    }

    private fun finishTutorial() {
        updateState { copy(page = FishingPage.GAME) }
    }

    private fun finish(s: FishingSession) = operation(FishingError.FINISH) {
        updateProgress(interactor.finish(s))
        scene = null
        updateState { copy(page = FishingPage.RESULTS, hud = null, confirmation = null) }
        effectsOnLoad = true
    }

    private fun deliverEffects() = operation(FishingError.EFFECT) {
        updateProgress(interactor.deliverEffects(profileId()))
    }

    private fun pause() {
        val s = scene ?: return
        if (s.paused) return
        scene = engine.pause(s)
        publish()
        if (!stateData.busy && !s.tutorial) checkpointTransition()
    }

    private fun checkpointTransition() {
        val s = scene ?: return
        if (s.tutorial || stateData.busy || s.phase in listOf(FishingPhase.CATCH_PENDING, FishingPhase.FINISHED)) return
        operation(FishingError.CHECKPOINT) { updateProgress(interactor.checkpoint(s)) }
    }

    private fun openPage(page: FishingPage) {
        if (stateData.page == FishingPage.GAME) pause()
        returnPage = stateData.page
        updateState { copy(page = page) }
    }

    private fun back() {
        when (stateData.page) {
            FishingPage.RECORDS -> updateState { copy(page = returnPage) }
            FishingPage.PRACTICE_INFO -> finishTutorial()
            FishingPage.GAME -> if (scene?.paused == true) ask(FishingConfirmation.EXIT) else pause()
            else -> router.back()
        }
    }

    private fun ask(confirmation: FishingConfirmation) {
        pause()
        updateState { copy(confirmation = confirmation) }
    }
    private fun confirm() {
        val confirmation = stateData.confirmation ?: return
        val s = scene ?: stateData.progress?.session
        operation(FishingError.CHECKPOINT) {
            if (s != null && !s.tutorial) updateProgress(interactor.abandon(profileId(), s.id))
            scene = null
            updateState { copy(confirmation = null, hud = null, page = FishingPage.GAME) }
            if (confirmation == FishingConfirmation.RESTART) restartAfterOperation = true else router.back()
        }
    }
    private var restartAfterOperation = false

    private fun operation(kind: FishingError, block: suspend () -> Unit) {
        if (stateData.busy) return
        retry = { operation(kind, block) }
        operationKind = kind
        updateState { copy(busy = true, error = null) }
        launchCoroutine(handleAction = ExceptionConsumer {
            operationKind = null
            scene = scene?.let(engine::pause)
            updateState { copy(busy = false, loading = false, error = kind, hud = scene) }
            true
        }) {
            block()
            operationKind = null
            checkpointSeconds = 0.0
            updateState { copy(busy = false, error = null, hud = scene) }
            when {
                restartAfterOperation -> { restartAfterOperation = false; start(tutorial = false) }
                finishOnLoad -> { finishOnLoad = false; scene?.let(::finish) }
                effectsOnLoad -> { effectsOnLoad = false; deliverEffects() }
            }
        }
    }

    private fun updateProgress(p: FishingProgress) { updateState { copy(progress = p) } }
    private fun profileId() = requireNotNull(stateData.environment).profileId
    private fun publish() { updateState { copy(hud = scene) } }
}
