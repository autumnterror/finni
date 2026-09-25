package github.detrig.minigames.fishing.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import github.detrig.core.audio.AudioCue
import github.detrig.core.audio.GameAudio
import github.detrig.minigames.fishing.domain.FishingPhase
import github.detrig.minigames.fishing.domain.FishingPreferences
import github.detrig.minigames.fishing.domain.FishingSession
import github.detrig.minigames.fishing.domain.ObjectKind

/** Feedback is emitted only for state transitions, never for Compose drawing. */
@Composable
internal fun FishingFeedback(session: FishingSession, preferences: FishingPreferences,
    active: Boolean, audio: GameAudio) {
    val haptics = LocalHapticFeedback.current
    DisposableEffect(audio) {
        audio.preload(FISHING_CUES.values.toList())
        onDispose { audio.stop(FISHING_OWNER) }
    }
    var previous by remember { mutableStateOf(session) }
    var wasActive by remember { mutableStateOf(active) }
    val reelBeat = (session.reelingSeconds / .45).toInt()
    val hazardBeat = (session.phaseSeconds / 1.5).toInt()

    LaunchedEffect(session.id, session.phase, session.warning, session.reelCount,
        session.pulling, reelBeat, hazardBeat, active, preferences.sound) {
        if (!active || !preferences.sound) audio.stop(FISHING_OWNER)
        if (active && wasActive && session.id == previous.id) {
            val cue = when {
                session.phase != previous.phase -> when (session.phase) {
                    FishingPhase.CHARGING -> "charge"
                    FishingPhase.FLYING -> "cast"
                    FishingPhase.SEARCHING -> if (previous.phase == FishingPhase.FLYING) "splash" else null
                    FishingPhase.FIGHTING -> "bite"
                    FishingPhase.HAULING -> if (session.attachedObject?.kind == ObjectKind.BOMB) "hazard" else "junk"
                    FishingPhase.JUNK_RELEASE -> "junk"
                    FishingPhase.BOMB_POP -> "pop"
                    FishingPhase.RELEASING -> "catch"
                    FishingPhase.EMPTY_RETURN -> "escape"
                    FishingPhase.READY -> if (previous.phase == FishingPhase.RELEASING) "release" else null
                    else -> null
                }
                session.warning && !previous.warning -> "warning"
                !session.warning && previous.warning && session.phase == FishingPhase.FIGHTING -> "relax"
                session.attachedObject?.kind == ObjectKind.BOMB &&
                    hazardBeat != (previous.phaseSeconds / 1.5).toInt() -> "hazard"
                session.phase == FishingPhase.FIGHTING && previous.pulling && !session.pulling -> "relax"
                session.pulling && session.phase in listOf(FishingPhase.FIGHTING, FishingPhase.HAULING) &&
                    (!previous.pulling || reelBeat != (previous.reelingSeconds / .45).toInt()) -> "reel"
                session.reelCount != previous.reelCount -> "reel"
                else -> null
            }
            if (cue != null && preferences.sound) FISHING_CUES[cue]?.let(audio::play)
            if (cue in listOf("bite", "warning", "catch") && preferences.haptics) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
        previous = session
        wasActive = active
    }
}

private const val FISHING_OWNER = "fishing"

private val FISHING_CUES = listOf(
    "charge", "cast", "splash", "bite", "reel", "relax", "warning", "catch",
    "release", "escape", "junk", "hazard", "pop",
).associateWith { name ->
    AudioCue(
        id = "fishing.$name",
        owner = FISHING_OWNER,
        assetPath = "fishing/audio/$name.wav",
        volume = if (name == "reel") 0.18f else 0.42f,
        priority = when (name) {
            "warning", "catch", "hazard", "pop" -> 3
            "bite", "escape" -> 2
            "reel" -> 0
            else -> 1
        },
        cooldownMillis = if (name == "reel") 180 else 0,
        blockMillis = when (name) {
            "reel" -> 90
            "catch" -> 380
            "pop" -> 340
            "cast", "release", "escape" -> 280
            else -> 230
        },
    )
}
