package github.detrig.minigames.flight.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import github.detrig.core.audio.AudioCue
import github.detrig.core.mvvm.command.CommandsQueueEffect
import github.detrig.core.mvvm.command.ImmutableCommandsQueue
import github.detrig.minigames.flight.FlightFeature
import github.detrig.minigames.flight.api.FlightFeedbackSettings

@Composable
internal fun FlightFeedback(viewModel: FlightViewModel, settings: FlightFeedbackSettings) {
    val currentSettings = rememberUpdatedState(settings)
    val haptics = LocalHapticFeedback.current
    val audio = remember { FlightFeature.component().gameAudio }
    DisposableEffect(audio) {
        audio.preload(listOf(GATE_CUE, LAND_CUE))
        onDispose { audio.stop("flight") }
    }
    LaunchedEffect(settings.sound) {
        if (!settings.sound) audio.stop("flight")
    }
    CommandsQueueEffect(remember(viewModel) { ImmutableCommandsQueue(viewModel.commands<FlightCue>()) }) { cue ->
        if (currentSettings.value.sound) audio.play(if (cue == FlightCue.GATE) GATE_CUE else LAND_CUE)
        if (currentSettings.value.haptics && cue == FlightCue.GATE) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
}

private val GATE_CUE = AudioCue(
    id = "flight.gate", owner = "flight", assetPath = "audio/kenney/confirmation_001.ogg",
    volume = 0.25f, priority = 2, cooldownMillis = 120, blockMillis = 320,
)
private val LAND_CUE = AudioCue(
    id = "flight.land", owner = "flight", assetPath = "audio/kenney/bong_001.ogg",
    volume = 0.22f, priority = 3, blockMillis = 550,
)
