package github.detrig.feature.gamestate.domain.model

/** Факты совместной игры. Размер эффекта определяет общий домен. */
data class PetPlayCompletion(
    val profileId: String,
    val sessionId: String,
    val gameId: String,
    val completedNaturally: Boolean,
    val validCastCount: Int = 0,
    val validActionCount: Int = validCastCount,
    val activePlayMillis: Long = 75_000,
)

object PetPlayReward {
    const val HAPPINESS_PER_ROUND = 3
    const val FLIGHT_MIN_ACTIVE_MILLIS = 10_000L

    fun delta(currentHappiness: Int, completedNaturally: Boolean, hasActivity: Boolean): Int =
        if (completedNaturally && hasActivity)
            minOf(HAPPINESS_PER_ROUND, (100 - currentHappiness).coerceAtLeast(0)) else 0

    fun delta(currentHappiness: Int, completion: PetPlayCompletion): Int {
        val enoughTime = completion.gameId != "flight" ||
            completion.activePlayMillis >= FLIGHT_MIN_ACTIVE_MILLIS
        return delta(currentHappiness, completion.completedNaturally,
            completion.validActionCount > 0 && enoughTime)
    }
}
