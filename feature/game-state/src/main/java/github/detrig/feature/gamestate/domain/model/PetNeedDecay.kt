package github.detrig.feature.gamestate.domain.model

import github.detrig.core.time.elapsedIntervals

data class PetNeedDecayConfig(
    val hungerIntervalMillis: Long = 60 * 60 * 1_000L,
    val happinessIntervalMillis: Long = 8 * 60 * 60 * 1_000L,
    val hungerLossPerInterval: Int = 10,
    val happinessLossPerInterval: Int = 2,
) {
    init {
        require(hungerIntervalMillis > 0 && happinessIntervalMillis > 0)
        require(hungerLossPerInterval > 0 && happinessLossPerInterval > 0)
    }
}

data class TimedPetNeeds(
    val hunger: Int,
    val happiness: Int,
    val hungerCheckpointMillis: Long,
    val happinessCheckpointMillis: Long,
)

/** The same calculation is used by foreground checks and background workers. */
fun reconcilePetNeeds(
    current: TimedPetNeeds,
    nowMillis: Long,
    config: PetNeedDecayConfig = PetNeedDecayConfig(),
): TimedPetNeeds {
    require(current.hunger in 0..100 && current.happiness in 0..100)
    val hunger = elapsedIntervals(current.hungerCheckpointMillis, nowMillis, config.hungerIntervalMillis)
    val happiness = elapsedIntervals(current.happinessCheckpointMillis, nowMillis, config.happinessIntervalMillis)
    return TimedPetNeeds(
        hunger = (current.hunger.toLong() - hunger.count.coerceAtMost(100) * config.hungerLossPerInterval)
            .coerceAtLeast(0).toInt(),
        happiness = (current.happiness.toLong() - happiness.count.coerceAtMost(100) * config.happinessLossPerInterval)
            .coerceAtLeast(0).toInt(),
        hungerCheckpointMillis = hunger.checkpointMillis,
        happinessCheckpointMillis = happiness.checkpointMillis,
    )
}
