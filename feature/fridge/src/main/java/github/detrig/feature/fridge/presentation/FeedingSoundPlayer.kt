package github.detrig.feature.fridge.presentation

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.util.concurrent.ConcurrentHashMap

internal enum class FeedingSound(
    val assetName: String,
    val volume: Float,
) {
    Pickup("pickup.wav", 0.28f),
    Bite("bite.wav", 0.42f),
    Chew("chew.wav", 0.32f),
}

/** Short one-shot cues for the feeding interaction; no looping or background audio. */
internal class FeedingSoundPlayer(context: Context) {
    private val loadedSamples = ConcurrentHashMap.newKeySet<Int>()
    private val soundIds = mutableMapOf<FeedingSound, Int>()
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loadedSamples.add(sampleId)
        }
        FeedingSound.entries.forEach { sound ->
            runCatching {
                context.assets.openFd("feeding/audio/${sound.assetName}").use { descriptor ->
                    soundIds[sound] = soundPool.load(descriptor, 1)
                }
            }
        }
    }

    fun play(sound: FeedingSound) {
        val sampleId = soundIds[sound] ?: return
        if (sampleId !in loadedSamples) return
        soundPool.play(sampleId, sound.volume, sound.volume, 1, 0, 1f)
    }

    fun release() {
        soundPool.setOnLoadCompleteListener(null)
        soundPool.release()
    }
}
