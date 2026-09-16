package github.detrig.minigames.flight.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import github.detrig.designsystem.component.*
import github.detrig.designsystem.theme.AppTheme
import github.detrig.minigames.flight.FlightFeature
import github.detrig.minigames.flight.R
import github.detrig.minigames.flight.domain.FlightOutcome
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun FlightScreen() {
    val component = remember { FlightFeature.component() }
    val model = viewModel { component.viewModel() }
    val petBitmap = component.petApi.rememberCurrentAppearanceBitmap(PET_BITMAP_SIZE_PX)
    val state by model.state().observeAsState(FlightViewState())
    val frames = model.renderFrames.collectAsState()
    val onEvent: (FlightViewEvent) -> Unit = remember(model) { model::perform }
    val onFlap = remember(model) { { model.perform(FlightViewEvent.Flap) } }
    val owner = LocalLifecycleOwner.current
    val view = LocalView.current
    val resources = LocalResources.current
    val artwork by produceState<FlightArtwork?>(null, resources) {
        value = withContext(Dispatchers.Default) { loadFlightArtwork(resources) }
    }
    LaunchedEffect(model) { model.perform(FlightViewEvent.Load) }
    DisposableEffect(owner, view, model) {
        fun visibility() = model.perform(FlightViewEvent.Foreground(
            owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && view.hasWindowFocus()))
        val observer = LifecycleEventObserver { _, _ -> visibility() }
        val focusListener = android.view.ViewTreeObserver.OnWindowFocusChangeListener { visibility() }
        owner.lifecycle.addObserver(observer)
        view.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)
        visibility()
        onDispose {
            model.perform(FlightViewEvent.Foreground(false))
            owner.lifecycle.removeObserver(observer)
            if (view.viewTreeObserver.isAlive) view.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
        }
    }
    LaunchedEffect(state.page, state.foreground, state.countdownGeneration, artwork, petBitmap, model) {
        if (!state.foreground || artwork == null || petBitmap == null) return@LaunchedEffect
        val id = model.frames.value?.id ?: return@LaunchedEffect
        if (state.page == FlightPage.PLAYING) {
            while (true) withFrameNanos { model.perform(FlightViewEvent.Frame(id, it)) }
        }
        if (state.page == FlightPage.COUNTDOWN) {
            val generation = state.countdownGeneration
            while (true) { delay(1000); model.perform(FlightViewEvent.CountdownTick(id, generation)) }
        }
    }
    BackHandler { onEvent(FlightViewEvent.Back) }
    FlightFeedback(model, state.settings)
    val colors = AppTheme.colors
    val shadowSize = with(LocalDensity.current) { AppTheme.elevation.low.toPx() }
    val hudShadow = Shadow(colors.roomBackground, Offset(0f, shadowSize), shadowSize * 2)
    CompositionLocalProvider(LocalContentColor provides colors.textPrimary) {
        Box(Modifier.fillMaxSize().testTag("flight_screen")) {
            FlightScene(frames, model.engine.config, artwork,
                state.settings.reducedMotion, state.page == FlightPage.PLAYING && state.foreground,
                stringResource(R.string.flight_flap), onFlap, Modifier.fillMaxSize().testTag("flight_scene"),
                petBitmap = petBitmap)

            if (state.page == FlightPage.COUNTDOWN || state.page == FlightPage.PLAYING) {
                val scoreLabel = stringResource(R.string.flight_score, state.score)
                Text(state.score.toString(),
                    Modifier.align(Alignment.TopEnd).safeDrawingPadding().padding(AppTheme.spacing.xl)
                        .testTag("flight_score").semantics { contentDescription = scoreLabel },
                    style = AppTheme.typography.gameScore.copy(shadow = hudShadow),
                    color = colors.flightCloud)
            }
            if (state.page == FlightPage.COUNTDOWN) {
                Column(Modifier.align(Alignment.Center).padding(AppTheme.spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.countdown.toString(), Modifier.testTag("flight_countdown"),
                        style = AppTheme.typography.gameCountdown.copy(shadow = hudShadow),
                        color = colors.flightCloud)
                    Text(stringResource(R.string.flight_hint),
                        style = AppTheme.typography.bodyStrong, color = colors.textPrimary,
                        textAlign = TextAlign.Center)
                }
            }
            if (state.page == FlightPage.LOADING || state.page == FlightPage.SAVING || petBitmap == null) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = colors.flightCloud)
            }
            if (state.page in listOf(FlightPage.RESULTS, FlightPage.RECORDS, FlightPage.ERROR, FlightPage.LOCKED)) {
                Box(Modifier.matchParentSize().background(colors.roomBackground.copy(alpha = .32f)))
                Box(Modifier.fillMaxSize().safeDrawingPadding().padding(AppTheme.spacing.lg),
                    contentAlignment = Alignment.Center) {
                    FinPetCard(Modifier.widthIn(max = AppTheme.sizes.preferredTouchTarget * 6)
                        .fillMaxWidth().testTag("flight_overlay")) {
                        FlightOverlay(state, model.engine.config.rulesVersion, onEvent)
                    }
                }
            }
        }
    }
}

private const val PET_BITMAP_SIZE_PX = 256

@Composable
private fun FlightOverlay(state: FlightViewState, rules: Int, onEvent: (FlightViewEvent) -> Unit) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(AppTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally) {
        when (state.page) {
            FlightPage.RESULTS -> {
                val result = state.progress?.lastResult
                Text(stringResource(if (result?.outcome == FlightOutcome.FINISHED)
                    R.string.flight_finished else R.string.flight_landed),
                    style = AppTheme.typography.sectionTitle, textAlign = TextAlign.Center)
                Text((result?.score ?: 0).toString(), Modifier.testTag("flight_result_score"),
                    style = AppTheme.typography.gameScore)
                Text(if (result?.newRecord == true) stringResource(R.string.flight_new_record)
                    else stringResource(R.string.flight_record, state.progress?.records?.get(rules)?.score ?: 0),
                    style = AppTheme.typography.bodyStrong)
                if ((result?.happinessDelta ?: 0) > 0)
                    Text(stringResource(R.string.flight_happiness, result!!.happinessDelta!!),
                        style = AppTheme.typography.caption)
                if (state.effectPending) {
                    Text(stringResource(R.string.flight_effect_pending), style = AppTheme.typography.caption)
                    Action(R.string.flight_retry_effect, FlightViewEvent.RetryEffect, onEvent, false)
                }
                Action(R.string.flight_again, FlightViewEvent.Start, onEvent)
                Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                    FinPetOutlinedButton(stringResource(R.string.flight_records), { onEvent(FlightViewEvent.Records) }, Modifier.weight(1f))
                    FinPetOutlinedButton(stringResource(R.string.flight_room), { onEvent(FlightViewEvent.Exit) }, Modifier.weight(1f))
                }
            }
            FlightPage.RECORDS -> {
                Text(stringResource(R.string.flight_records), style = AppTheme.typography.sectionTitle)
                if (state.progress?.records.isNullOrEmpty())
                    Text(stringResource(R.string.flight_no_record), style = AppTheme.typography.body)
                state.progress?.records?.toSortedMap(compareByDescending { it })?.forEach { (version, record) ->
                    Text(stringResource(R.string.flight_record_version, version, record.score), style = AppTheme.typography.body)
                }
                val settings = state.settings
                Setting(R.string.flight_sound, settings.sound) { onEvent(FlightViewEvent.Settings(settings.copy(sound = it))) }
                Setting(R.string.flight_haptics, settings.haptics) { onEvent(FlightViewEvent.Settings(settings.copy(haptics = it))) }
                Setting(R.string.flight_reduced_motion, settings.reducedMotion) { onEvent(FlightViewEvent.Settings(settings.copy(reducedMotion = it))) }
                Action(R.string.flight_back, FlightViewEvent.Back, onEvent)
            }
            FlightPage.ERROR -> {
                Text(stringResource(R.string.flight_error), style = AppTheme.typography.sectionTitle)
                Text(stringResource(R.string.flight_error_body), style = AppTheme.typography.body)
                Action(R.string.flight_retry, FlightViewEvent.Retry, onEvent)
                Action(R.string.flight_room, FlightViewEvent.Exit, onEvent, false)
            }
            FlightPage.LOCKED -> {
                Text(stringResource(R.string.flight_locked), style = AppTheme.typography.body)
                Action(R.string.flight_room, FlightViewEvent.Exit, onEvent)
            }
            else -> Unit
        }
    }
}
@Composable
private fun Action(label: Int, event: FlightViewEvent, onEvent: (FlightViewEvent) -> Unit, primary: Boolean = true) {
    if (primary) FinPetButton(stringResource(label), { onEvent(event) },
        Modifier.fillMaxWidth().heightIn(min = AppTheme.sizes.preferredTouchTarget))
    else FinPetOutlinedButton(stringResource(label), { onEvent(event) }, Modifier.fillMaxWidth())
}
@Composable
private fun Setting(label: Int, checked: Boolean, change: (Boolean) -> Unit) {
    val title = stringResource(label)
    Row(Modifier.fillMaxWidth().heightIn(min = AppTheme.sizes.minimumTouchTarget), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = AppTheme.typography.body)
        Switch(checked, change, Modifier.semantics { contentDescription = title })
    }
}
