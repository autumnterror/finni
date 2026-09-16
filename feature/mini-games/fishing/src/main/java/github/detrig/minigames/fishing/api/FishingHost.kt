package github.detrig.minigames.fishing.api

/** Узкий мост к общему профилю; фича не знает устройство комнаты и кошелька. */
interface FishingHost {
    suspend fun environment(): FishingEnvironment
    fun showUnlockPreview()
    suspend fun applyPlayEffect(profileId: String, sessionId: String): Int
    fun feedbackSettings(): FishingFeedbackSettings
    fun saveFeedbackSettings(settings: FishingFeedbackSettings)
}

data class FishingEnvironment(
    val profileId: String,
    val unlocked: Boolean,
)

data class FishingFeedbackSettings(
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val reducedMotion: Boolean = false,
)
