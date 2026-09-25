package github.detrig.core.audio

/** Pure decision rules shared by every caller of the audio service. */
class AudioPlaybackPolicy {
    private data class Active(val owner: String, val priority: Int, val untilMillis: Long)

    private val lastPlayedAt = mutableMapOf<String, Long>()
    private var active: Active? = null

    fun canPlay(cue: AudioCue, nowMillis: Long): Boolean {
        val last = lastPlayedAt[cue.id]
        if (last != null && nowMillis - last in 0 until cue.cooldownMillis) return false
        val current = active
        return current == null || nowMillis >= current.untilMillis || cue.priority >= current.priority
    }

    fun recordPlayed(cue: AudioCue, nowMillis: Long) {
        lastPlayedAt[cue.id] = nowMillis
        active = Active(cue.owner, cue.priority, nowMillis + cue.blockMillis)
    }

    fun stop(owner: String) {
        if (active?.owner == owner) active = null
    }

    fun stopAll() {
        active = null
    }
}
