package github.detrig.minigames.fishing.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import github.detrig.designsystem.component.*
import github.detrig.designsystem.theme.AppTheme
import github.detrig.feature.pet.api.PetApi
import github.detrig.minigames.fishing.FishingFeature
import github.detrig.minigames.fishing.R
import github.detrig.minigames.fishing.domain.*

@Composable
internal fun FishingScreen(
    vm: FishingViewModel = viewModel { FishingFeature.component().viewModel() },
    petApi: PetApi? = null,
) {
    val petBitmap = petApi?.rememberCurrentAppearanceBitmap(PET_BITMAP_SIZE_PX)
    val state by vm.state().observeAsState(FishingViewState())
    val context = LocalContext.current
    var art by remember { mutableStateOf<FishingArt?>(null) }
    var artError by remember { mutableStateOf(false) }
    var artAttempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(artAttempt) {
        artError = false
        try { art = FishingArt.load(context) }
        catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (_: Exception) { artError = true }
    }
    LaunchedEffect(vm) { vm.perform(FishingViewEvent.Load) }
    val lifecycle = LocalLifecycleOwner.current
    val view = LocalView.current
    DisposableEffect(lifecycle, view, vm) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) vm.perform(FishingViewEvent.Pause)
        }
        val focus = android.view.ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (!hasFocus) vm.perform(FishingViewEvent.Pause)
        }
        lifecycle.lifecycle.addObserver(observer)
        view.viewTreeObserver.addOnWindowFocusChangeListener(focus)
        onDispose {
            vm.perform(FishingViewEvent.Pause)
            lifecycle.lifecycle.removeObserver(observer)
            if (view.viewTreeObserver.isAlive) view.viewTreeObserver.removeOnWindowFocusChangeListener(focus)
        }
    }
    LaunchedEffect(vm, art, state.page, state.hud?.paused, lifecycle) {
        if (art != null && state.page == FishingPage.GAME && state.hud?.paused != true) {
            lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                var previous = withFrameNanos { it }
                while (true) {
                    val next = withFrameNanos { it }
                    vm.frame((next - previous) / 1_000_000_000.0)
                    previous = next
                }
            }
        }
    }
    BackHandler { vm.perform(FishingViewEvent.Back) }
    val action: (FishingViewEvent) -> Unit = vm::perform
    Surface(color = AppTheme.colors.surfaceBase, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            when {
                state.error == FishingError.LOAD -> FishingErrorPanel(R.string.fishing_loading_error,
                    onRetry = { action(FishingViewEvent.Retry) }, onExit = { action(FishingViewEvent.Exit) })
                artError -> FishingErrorPanel(R.string.fishing_art_error, { artAttempt++ }, { action(FishingViewEvent.Exit) })
                state.loading || art == null || petApi != null && petBitmap == null -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg)) {
                    CircularProgressIndicator(color = AppTheme.colors.actionPrimary)
                    Text(stringResource(R.string.fishing_loading), style = AppTheme.typography.body)
                }
                state.environment?.unlocked != true -> Column(Modifier.align(Alignment.Center).padding(AppTheme.spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg)) {
                    Text(stringResource(R.string.fishing_locked), style = AppTheme.typography.body)
                    FinPetButton(stringResource(R.string.fishing_room), { action(FishingViewEvent.Exit) })
                }
                else -> {
                    val loadedArt = requireNotNull(art)
                    when (state.page) {
                        FishingPage.GAME -> FishingGame(state, loadedArt, petBitmap, vm, action)
                        FishingPage.RECORDS -> FishingRecords(state, vm.engine.config, action)
                        FishingPage.RESULTS -> FishingResults(state, petBitmap, action)
                        FishingPage.PRACTICE_INFO -> FishingPracticeInfo(state, action)
                    }
                    if (state.error != null) {
                        Box(Modifier.fillMaxSize().background(AppTheme.colors.roomBackground.copy(alpha = .72f)),
                            contentAlignment = Alignment.Center) {
                            val message = when (state.error) {
                                FishingError.CATCH -> R.string.fishing_catch_error
                                FishingError.FINISH -> R.string.fishing_finish_error
                                FishingError.EFFECT -> R.string.fishing_effect_error
                                else -> R.string.fishing_checkpoint_error
                            }
                            FishingErrorPanel(message, { action(FishingViewEvent.Retry) }, { action(FishingViewEvent.Exit) })
                        }
                    }
                }
            }
            if (state.confirmation != null) {
                AlertDialog(onDismissRequest = { action(FishingViewEvent.Dismiss) },
                    title = { Text(stringResource(R.string.fishing_exit_title)) },
                    text = { Text(stringResource(if (state.error == FishingError.CATCH) R.string.fishing_unsaved_message else R.string.fishing_exit_message)) },
                    confirmButton = { TextButton(onClick = { action(FishingViewEvent.Confirm) }, enabled = !state.busy) {
                        Text(stringResource(if (state.confirmation == FishingConfirmation.RESTART) R.string.fishing_restart else R.string.fishing_confirm_exit))
                    } },
                    dismissButton = { TextButton(onClick = { action(FishingViewEvent.Dismiss) }) { Text(stringResource(R.string.fishing_stay)) } },
                    shape = AppTheme.shapes.dialog)
            }
        }
    }
}

@Composable
private fun FishingGame(
    state: FishingViewState,
    art: FishingArt,
    petBitmap: ImageBitmap?,
    vm: FishingViewModel,
    action: (FishingViewEvent) -> Unit,
) {
    val s = state.hud ?: return
    val preferences = state.progress?.preferences ?: FishingPreferences()
    FishingFeedback(s, preferences, active = !s.paused && s.resumeSeconds <= 0 && state.error == null)
    Box(Modifier.fillMaxSize()) {
        FishingWorld(art, vm, preferences, petBitmap, Modifier.fillMaxSize().fishingInput(state, action))
        Row(Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(AppTheme.spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            val remaining = kotlin.math.ceil((vm.engine.config.round.durationSeconds - s.activeSeconds).coerceAtLeast(0.0)).toInt()
            FishingSceneMetric(stringResource(if (s.tutorial) R.string.fishing_practice else R.string.fishing_time),
                if (s.tutorial) "∞" else "${remaining / 60}:" + (remaining % 60).toString().padStart(2, '0'), "fishing_time")
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                FishingSceneMetric(stringResource(R.string.fishing_weight), formatFishingMass(s.totalGrams), "fishing_weight")
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(AppTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            if (s.phase == FishingPhase.SEARCHING && !s.paused) {
                Surface(onClick = { action(FishingViewEvent.Recast) }, shape = AppTheme.shapes.button,
                    color = AppTheme.colors.textPrimary.copy(alpha = .68f), contentColor = AppTheme.colors.onActionPrimary,
                    modifier = Modifier.align(Alignment.End).heightIn(min = AppTheme.sizes.minimumTouchTarget), enabled = !state.busy) {
                    Box(Modifier.padding(horizontal = AppTheme.spacing.md), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.fishing_recast), style = AppTheme.typography.label)
                    }
                }
            }
            FishingControls(state, vm.engine.config, Modifier.fillMaxWidth())
        }
        if (s.paused) {
            Box(Modifier.fillMaxSize().background(AppTheme.colors.textPrimary.copy(alpha = .5f)), contentAlignment = Alignment.Center) {
                FinPetCard(Modifier.padding(AppTheme.spacing.lg).widthIn(max = AppTheme.sizes.preferredTouchTarget * 8)) {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(AppTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
                        Text(stringResource(R.string.fishing_pause), style = AppTheme.typography.screenTitle)
                        FinPetButton(stringResource(R.string.fishing_continue), { action(FishingViewEvent.Continue) }, Modifier.fillMaxWidth(), !state.busy)
                        FinPetOutlinedButton(stringResource(R.string.fishing_restart), { action(FishingViewEvent.Restart) }, Modifier.fillMaxWidth(), !state.busy)
                        FinPetOutlinedButton(stringResource(R.string.fishing_records), { action(FishingViewEvent.Records) }, Modifier.fillMaxWidth(), !state.busy)
                        FinPetOutlinedButton(stringResource(R.string.fishing_how), { action(FishingViewEvent.Tutorial) }, Modifier.fillMaxWidth(), !state.busy)
                        if (state.progress?.migrationNotice == true) Text(stringResource(R.string.fishing_migrated), style = AppTheme.typography.caption)
                        FishingSettings(preferences, !state.busy, action)
                        if (s.tutorial) TextButton(onClick = { action(FishingViewEvent.SkipTutorial) }) { Text(stringResource(R.string.fishing_skip)) }
                        TextButton(onClick = { action(FishingViewEvent.Exit) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.fishing_room)) }
                    }
                }
            }
        }
    }
}

private const val PET_BITMAP_SIZE_PX = 256

@Composable
private fun FishingSceneMetric(label: String, value: String, tag: String) {
    Column(Modifier.background(AppTheme.colors.textPrimary.copy(alpha = .68f), AppTheme.shapes.card)
        .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm)) {
        Text(label, style = AppTheme.typography.caption, color = AppTheme.colors.onActionPrimary)
        Text(value, style = AppTheme.typography.metricValue, color = AppTheme.colors.onActionPrimary, modifier = Modifier.testTag(tag))
    }
}

@Composable
private fun FishingWorld(art: FishingArt, vm: FishingViewModel, preferences: FishingPreferences, petBitmap: ImageBitmap?, modifier: Modifier) {
    Box(modifier.background(AppTheme.colors.roomBackground), contentAlignment = Alignment.Center) {
        val preview = remember(vm) { vm.engine.create("preview", "current", 42, 0).copy(phase = FishingPhase.READY) }
        Box(Modifier.fillMaxSize()) {
            val description = stringResource(R.string.fishing_scene_description)
            FishingScene(art, vm.engine, { vm.scene ?: preview }, preferences.reducedMotion,
                Modifier.fillMaxSize().semantics { contentDescription = description }, petBitmap, showInputCue = true)
            val s = vm.state().observeAsState().value?.hud
            if (s != null) {
                if (s.tutorial && !s.paused) {
                    val hint = when {
                        s.warning -> R.string.fishing_relax
                        s.phase == FishingPhase.FIGHTING -> R.string.fishing_pull
                        s.phase == FishingPhase.CHARGING -> R.string.fishing_release_cast
                        s.phase == FishingPhase.READY -> R.string.fishing_hold_cast
                        else -> R.string.fishing_searching
                    }
                    FinPetCard(Modifier.align(Alignment.Center).padding(AppTheme.spacing.sm)) {
                        Text(stringResource(hint), Modifier.padding(AppTheme.spacing.sm), style = AppTheme.typography.bodyStrong)
                    }
                    TextButton(onClick = { vm.perform(FishingViewEvent.SkipTutorial) }, Modifier.align(Alignment.CenterEnd)) {
                        Text(stringResource(R.string.fishing_skip))
                    }
                }
                if (s.phase == FishingPhase.COUNTDOWN || s.resumeSeconds > 0) {
                    val count = kotlin.math.ceil(if (s.resumeSeconds > 0) s.resumeSeconds else vm.engine.config.round.countdownSeconds - s.phaseSeconds).toInt().coerceAtLeast(1)
                    FinPetCard(Modifier.align(Alignment.Center)) {
                        Text(count.toString(), Modifier.testTag("fishing_countdown").padding(AppTheme.spacing.xl), style = AppTheme.typography.brand)
                    }
                }
                if (s.bannerSeconds > 0 && s.catches.isNotEmpty()) {
                    val fish = s.catches.last()
                    FinPetCard(Modifier.align(Alignment.Center).padding(AppTheme.spacing.sm)) {
                        Text(stringResource(R.string.fishing_catch_label, vm.engine.config.fish(fish.speciesId).nameRu, formatFishingMass(fish.grams)),
                            Modifier.padding(AppTheme.spacing.sm), style = AppTheme.typography.bodyStrong)
                    }
                }
            }
        }
    }
}

@Composable
internal fun FishingErrorPanel(message: Int, onRetry: () -> Unit, onExit: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(AppTheme.spacing.xl), contentAlignment = Alignment.Center) {
        FinPetCard {
            Column(Modifier.padding(AppTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg)) {
                Text(stringResource(message), style = AppTheme.typography.bodyStrong)
                FinPetButton(stringResource(R.string.fishing_retry), onRetry, Modifier.fillMaxWidth())
                FinPetOutlinedButton(stringResource(R.string.fishing_room), onExit, Modifier.fillMaxWidth())
            }
        }
    }
}
