package github.detrig.minigames.flight.presentation

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.HandlerThread
import androidx.compose.runtime.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import github.detrig.core.mvvm.command.CommandsQueueEffect
import github.detrig.core.mvvm.command.ImmutableCommandsQueue
import github.detrig.minigames.flight.api.FlightFeedbackSettings

@Composable
internal fun FlightFeedback(viewModel: FlightViewModel, settings: FlightFeedbackSettings) {
    val currentSettings = rememberUpdatedState(settings)
    val haptics = LocalHapticFeedback.current
    val audio = remember { FlightAudioPlayer() }
    DisposableEffect(audio) { onDispose { audio.close() } }
    LaunchedEffect(settings.sound) { if (settings.sound) audio.prepare() }
    CommandsQueueEffect(remember(viewModel) { ImmutableCommandsQueue(viewModel.commands<FlightCue>()) }) { cue ->
        if (currentSettings.value.sound) {
            audio.play(cue)
        }
        if (currentSettings.value.haptics && cue == FlightCue.GATE)
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
}

/** AudioTrack/ToneGenerator могут блокировать поток: создание и сигналы вне кадра UI. */
private class FlightAudioPlayer {
    private val thread = HandlerThread("FlightAudio").apply { start() }
    private val handler = Handler(thread.looper)
    private var tone: ToneGenerator? = null
    private fun create() {
        if (tone == null) tone = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 18) }.getOrNull()
    }
    fun prepare() { handler.post { create() } }
    fun play(cue: FlightCue) { handler.post {
        create()
        tone?.startTone(if (cue == FlightCue.GATE) ToneGenerator.TONE_PROP_BEEP else ToneGenerator.TONE_PROP_ACK, 65)
    } }
    fun close() { handler.post { tone?.release(); tone = null; thread.quitSafely() } }
}
