package github.detrig.core.audio

/** A short, non-looping effect. Assets from feature modules are merged into the app APK. */
data class AudioCue(
    val id: String,
    val owner: String,
    val assetPath: String,
    val volume: Float = 0.4f,
    val priority: Int = 1,
    val cooldownMillis: Long = 0,
    val blockMillis: Long = 350,
) {
    init {
        require(id.isNotBlank() && owner.isNotBlank() && assetPath.isNotBlank())
        require(volume in 0f..1f && priority >= 0)
        require(cooldownMillis >= 0 && blockMillis > 0)
    }
}

/** One application-wide path for game effects. Calls may come from any thread. */
interface GameAudio {
    fun preload(cues: Collection<AudioCue>)
    fun play(cue: AudioCue)
    fun stop(owner: String)
    fun stopAll()
    fun setForeground(foreground: Boolean)
}

object SilentGameAudio : GameAudio {
    override fun preload(cues: Collection<AudioCue>) = Unit
    override fun play(cue: AudioCue) = Unit
    override fun stop(owner: String) = Unit
    override fun stopAll() = Unit
    override fun setForeground(foreground: Boolean) = Unit
}
