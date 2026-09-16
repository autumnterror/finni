package github.detrig.minigames.fishing.presentation

import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import github.detrig.minigames.fishing.domain.*
import java.util.concurrent.ConcurrentHashMap

/** Только короткие сигналы действий. Нет фоновой дорожки и зацикленных потоков. */
@Composable
internal fun FishingFeedback(session: FishingSession, preferences: FishingPreferences, active: Boolean) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val audio = remember {
        SoundPool.Builder().setMaxStreams(3).setAudioAttributes(AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
    }
    val sounds = remember { mutableMapOf<String, Int>() }
    val loaded = remember { ConcurrentHashMap.newKeySet<Int>() }
    val streams = remember { ArrayDeque<Int>() }
    DisposableEffect(audio) {
        audio.setOnLoadCompleteListener { _, id, status -> if (status == 0) loaded.add(id) }
        listOf("charge", "cast", "splash", "bite", "reel", "relax", "warning", "catch", "release", "escape", "junk", "hazard", "pop").forEach { name ->
            context.assets.openFd("fishing/audio/$name.wav").use { sounds[name] = audio.load(it, 1) }
        }
        onDispose { audio.setOnLoadCompleteListener(null); audio.release() }
    }
    var previous by remember { mutableStateOf(session) }
    var wasActive by remember { mutableStateOf(active) }
    val reelBeat = (session.reelingSeconds / .45).toInt()
    val hazardBeat = (session.phaseSeconds / 1.5).toInt()
    LaunchedEffect(session.id, session.phase, session.warning, session.reelCount, session.pulling, reelBeat, hazardBeat, active, preferences.sound) {
        if (!active || !preferences.sound) {
            streams.forEach(audio::stop)
            streams.clear()
        }
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
                session.attachedObject?.kind == ObjectKind.BOMB && hazardBeat != (previous.phaseSeconds / 1.5).toInt() -> "hazard"
                session.phase == FishingPhase.FIGHTING && previous.pulling && !session.pulling -> "relax"
                session.pulling && session.phase in listOf(FishingPhase.FIGHTING, FishingPhase.HAULING) &&
                    (!previous.pulling || reelBeat != (previous.reelingSeconds / .45).toInt()) -> "reel"
                session.reelCount != previous.reelCount -> "reel"
                else -> null
            }
            if (cue != null && preferences.sound) sounds[cue]?.takeIf { it in loaded }?.let { id ->
                val volume = if (cue == "reel") .18f else .45f
                val stream = audio.play(id, volume, volume, if (cue == "reel") 0 else 1, 0, 1f)
                if (stream != 0) {
                    if (streams.size >= 8) streams.removeFirst()
                    streams.addLast(stream)
                }
            }
            if (cue in listOf("bite", "warning", "catch") && preferences.haptics) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        previous = session
        wasActive = active
    }
}
