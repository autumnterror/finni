package github.detrig.minigames.flight.api

data class FlightEnvironment(
    val profileId: String,
    val unlocked: Boolean,
    val appearanceId: String,
)
data class FlightFeedbackSettings(val sound: Boolean = true, val haptics: Boolean = true,
    val reducedMotion: Boolean = false)
data class FlightCompletion(val profileId: String, val sessionId: String,
    val activeMillis: Long, val flapCount: Int)

/** Узкий мост к профилю, питомцу и общим настройкам приложения. */
interface FlightHost {
    suspend fun environment(): FlightEnvironment
    suspend fun applyPlayEffect(completion: FlightCompletion): Int
    fun feedbackSettings(): FlightFeedbackSettings
    fun saveFeedbackSettings(settings: FlightFeedbackSettings)
}
