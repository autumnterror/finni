package github.detrig.feature.gamestate.domain.model

/** Once per game day, on the first actual launch of an unlocked game. */
object MiniGameHappinessRewards {
    fun firstLaunchPoints(gameId: String): Int = when (gameId) {
        "fishing" -> 12
        "drawing" -> 15
        "music" -> 18
        else -> 0
    }
}
