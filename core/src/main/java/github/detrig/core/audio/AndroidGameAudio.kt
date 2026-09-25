package github.detrig.core.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.SoundPool
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log

/** Single SoundPool and audio-focus owner for all in-app sound effects. */
class AndroidGameAudio(context: Context) : GameAudio {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("finpet_feedback", Context.MODE_PRIVATE)
    private val audioManager = appContext.getSystemService(AudioManager::class.java)
    private val thread = HandlerThread("FinPetAudio").apply { start() }
    private val handler = Handler(thread.looper)
    private val policy = AudioPlaybackPolicy()
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attributes)
        .setOnAudioFocusChangeListener({ change ->
            if (change < 0) {
                handler.post { stopAllInternal() }
            }
        }, handler)
        .build()
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "sound" && !isEnabled()) stopAll()
    }
    private val sampleIds = mutableMapOf<String, Int>()
    private val readySamples = mutableSetOf<Int>()
    private var pool: SoundPool? = null
    private var pendingCue: AudioCue? = null
    private var currentCue: AudioCue? = null
    private var currentStreamId = 0
    private var hasFocus = false
    private var foreground = false
    private val finishCue = Runnable { stopAllInternal() }

    init {
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
        handler.post {
            pool = SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(attributes)
                .build().also { soundPool ->
                    soundPool.setOnLoadCompleteListener { _, sampleId, status ->
                        handler.post {
                            if (status == 0) {
                                readySamples.add(sampleId)
                                val cue = pendingCue
                                if (cue != null && sampleIds[cue.assetPath] == sampleId) {
                                    pendingCue = null
                                    playLoaded(cue)
                                }
                            } else {
                                Log.w(TAG, "Could not decode sound sample $sampleId")
                            }
                        }
                    }
                }
        }
    }

    override fun preload(cues: Collection<AudioCue>) {
        val copy = cues.toList()
        handler.post { copy.forEach(::load) }
    }

    override fun play(cue: AudioCue) {
        handler.post {
            if (!foreground || !isEnabled()) return@post
            val sampleId = load(cue) ?: return@post
            if (sampleId in readySamples) playLoaded(cue) else pendingCue = cue
        }
    }

    override fun stop(owner: String) {
        handler.post {
            if (pendingCue?.owner == owner) pendingCue = null
            if (currentCue?.owner == owner) stopAllInternal()
        }
    }

    override fun stopAll() {
        handler.post { stopAllInternal() }
    }

    override fun setForeground(foreground: Boolean) {
        handler.post {
            this.foreground = foreground
            if (!foreground) stopAllInternal()
        }
    }

    private fun load(cue: AudioCue): Int? {
        sampleIds[cue.assetPath]?.let { return it }
        val sampleId = try {
            appContext.assets.openFd(cue.assetPath).use { descriptor ->
                pool?.load(descriptor, 1)
            }
        } catch (error: Exception) {
            Log.w(TAG, "Could not load ${cue.assetPath}", error)
            null
        }
        if (sampleId == null || sampleId == 0) return null
        sampleIds[cue.assetPath] = sampleId
        return sampleId
    }

    private fun playLoaded(cue: AudioCue) {
        if (!foreground || !isEnabled()) return
        val now = SystemClock.elapsedRealtime()
        if (!policy.canPlay(cue, now)) return
        if (!hasFocus) {
            if (audioManager.requestAudioFocus(focusRequest) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return
            hasFocus = true
        }
        if (currentStreamId != 0) pool?.stop(currentStreamId)
        handler.removeCallbacks(finishCue)
        val sampleId = sampleIds[cue.assetPath] ?: return
        val streamId = pool?.play(sampleId, cue.volume, cue.volume, cue.priority, 0, 1f) ?: 0
        if (streamId == 0) {
            stopAllInternal()
            return
        }
        currentStreamId = streamId
        currentCue = cue
        policy.recordPlayed(cue, now)
        handler.postDelayed(finishCue, cue.blockMillis)
    }

    private fun stopAllInternal() {
        handler.removeCallbacks(finishCue)
        if (currentStreamId != 0) pool?.stop(currentStreamId)
        currentStreamId = 0
        currentCue = null
        pendingCue = null
        policy.stopAll()
        if (hasFocus) audioManager.abandonAudioFocusRequest(focusRequest)
        hasFocus = false
    }

    private fun isEnabled(): Boolean = preferences.getBoolean("sound", true)

    private companion object {
        const val TAG = "FinPetAudio"
    }
}
