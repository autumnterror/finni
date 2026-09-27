package github.detrig.feature.gamestate.domain.model

data class PetDirtAnchor(
    val cleanedAtMillis: Long,
    val completedGamesAtClean: Long,
)

data class PetDirtConfig(
    val firstStageMillis: Long = 2 * 60 * 60 * 1_000L,
    val secondStageMillis: Long = 5 * 60 * 60 * 1_000L,
    val thirdStageMillis: Long = 9 * 60 * 60 * 1_000L,
    val gamesPerStage: Long = 5L,
) {
    init {
        require(firstStageMillis > 0 && secondStageMillis > firstStageMillis &&
            thirdStageMillis > secondStageMillis && gamesPerStage > 0)
    }
}

object PetDirtRules {

    /** Moves the debug stage while keeping subsequent real-time and play progression intact. */
    fun anchorAtStage(stage: Int, nowMillis: Long, totalCompletedGames: Long,
        config: PetDirtConfig = PetDirtConfig()): PetDirtAnchor {
        require(stage in 0..3)
        val elapsed = when (stage) {
            0 -> 0L
            1 -> config.firstStageMillis
            2 -> config.secondStageMillis
            else -> config.thirdStageMillis
        }
        return PetDirtAnchor(nowMillis - elapsed, totalCompletedGames)
    }

    /** Real time and completed mini-game rounds both contribute to the current dirt stage. */
    fun stage(anchor: PetDirtAnchor, nowMillis: Long, totalCompletedGames: Long,
        config: PetDirtConfig = PetDirtConfig()): Int {
        val elapsed = if (nowMillis >= anchor.cleanedAtMillis) nowMillis - anchor.cleanedAtMillis else 0L
        val timed = when {
            elapsed >= config.thirdStageMillis -> 3
            elapsed >= config.secondStageMillis -> 2
            elapsed >= config.firstStageMillis -> 1
            else -> 0
        }
        val played = (totalCompletedGames - anchor.completedGamesAtClean)
            .coerceAtLeast(0L) / config.gamesPerStage
        return (timed + played).coerceAtMost(3L).toInt()
    }
}
