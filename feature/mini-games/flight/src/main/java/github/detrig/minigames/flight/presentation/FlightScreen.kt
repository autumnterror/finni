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
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import github.detrig.designsystem.component.*
import github.detrig.designsystem.theme.AppTheme
import github.detrig.designsystem.theme.FinPetTheme
import github.detrig.minigames.flight.FlightFeature
import github.detrig.minigames.flight.R
import github.detrig.minigames.flight.domain.FlightOutcome
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
    LaunchedEffect(state.page, state.foreground, artwork, petBitmap, model) {
        if (!state.foreground || artwork == null || petBitmap == null) return@LaunchedEffect
        val id = model.frames.value?.id ?: return@LaunchedEffect
        if (state.page == FlightPage.PLAYING) {
            while (true) withFrameNanos { model.perform(FlightViewEvent.Frame(id, it)) }
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
                state.settings.reducedMotion,
                state.page in listOf(FlightPage.READY, FlightPage.PLAYING) && state.foreground,
                stringResource(if (state.page == FlightPage.READY) R.string.flight_first_tap else R.string.flight_flap),
                onFlap, Modifier.fillMaxSize().testTag("flight_scene"),
                petBitmap = petBitmap)

            if (state.page == FlightPage.READY || state.page == FlightPage.PLAYING) {
                val scoreLabel = stringResource(R.string.flight_score, state.score)
                Text(state.score.toString(),
                    Modifier.align(Alignment.TopEnd).safeDrawingPadding().padding(AppTheme.spacing.xl)
                        .testTag("flight_score").semantics { contentDescription = scoreLabel },
                    style = AppTheme.typography.gameScore.copy(shadow = hudShadow),
                    color = colors.flightCloud)
            }
            if (state.page == FlightPage.READY) {
                Text(stringResource(if (frames.value.session?.started == true) R.string.flight_resume_tap
                    else R.string.flight_first_tap),
                    Modifier.align(Alignment.BottomCenter).safeDrawingPadding()
                        .padding(AppTheme.spacing.xl).testTag("flight_ready_hint"),
                    style = AppTheme.typography.bodyStrong.copy(shadow = hudShadow),
                    color = colors.flightCloud,
                    textAlign = TextAlign.Center)
            }
            if (state.page == FlightPage.LOADING || state.page == FlightPage.SAVING || petBitmap == null) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = colors.flightCloud)
            }
            if (state.page in listOf(FlightPage.RECORDS, FlightPage.ERROR, FlightPage.LOCKED)) {
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
        if (state.page == FlightPage.RESULTS) {
            val result = state.progress?.lastResult
            FlightResultsDialog(
                title = stringResource(if (result?.outcome == FlightOutcome.FINISHED)
                    R.string.flight_finished else R.string.flight_landed),
                score = result?.score ?: 0,
                record = state.progress?.records?.get(model.engine.config.rulesVersion)?.score ?: 0,
                newRecord = result?.newRecord == true,
                onAgain = { onEvent(FlightViewEvent.Start) },
                onRoom = { onEvent(FlightViewEvent.Exit) },
            )
        }
    }
}

private const val PET_BITMAP_SIZE_PX = 256

@Composable
private fun FlightResultsDialog(
    title: String,
    score: Int,
    record: Int,
    newRecord: Boolean,
    onAgain: () -> Unit,
    onRoom: () -> Unit,
) {
    FinPetModalDialog(
        title = title,
        onDismissRequest = null,
        actions = {
            FinPetButton(stringResource(R.string.flight_again), onAgain,
                Modifier.fillMaxWidth().testTag("flight_again"),
                style = FinPetButtonDefaults.storefrontPrimaryStyle())
            FinPetOutlinedButton(stringResource(R.string.flight_room), onRoom,
                Modifier.fillMaxWidth().testTag("flight_room"),
                style = FinPetButtonDefaults.storefrontOutlinedStyle())
        },
    ) {
        FinPetModalSection(Modifier.fillMaxWidth(), tone = FinPetModalSectionTone.Highlighted) {
            Column(Modifier.fillMaxWidth().padding(AppTheme.spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                Text(stringResource(R.string.flight_current_result),
                    style = AppTheme.typography.bodyStrong,
                    color = AppTheme.colors.storefront.onSurface)
                Text(score.toString(), Modifier.testTag("flight_result_score"),
                    style = AppTheme.typography.gameScore,
                    color = AppTheme.colors.storefront.onSurface)
            }
        }
        FinPetModalSection(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(AppTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
                Text(stringResource(if (newRecord) R.string.flight_new_record else R.string.flight_record_label),
                    Modifier.weight(1f), style = AppTheme.typography.bodyStrong,
                    color = if (newRecord) AppTheme.colors.statusPositive.accent
                    else AppTheme.colors.storefront.onSurface)
                Text(record.toString(), Modifier.testTag("flight_result_record"),
                    style = AppTheme.typography.metricValue,
                    color = AppTheme.colors.storefront.onSurface)
            }
        }
    }
}

@Preview(name = "Flight result", widthDp = 360, heightDp = 700, showBackground = true)
@Composable
private fun FlightResultsPreview() {
    FinPetTheme {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            FlightResultsDialog("Полёт окончен", 4, 4, true, {}, {})
        }
    }
}

@Composable
private fun FlightOverlay(state: FlightViewState, rules: Int, onEvent: (FlightViewEvent) -> Unit) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(AppTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally) {
        when (state.page) {
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
