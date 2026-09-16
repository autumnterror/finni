package github.detrig.minigames.flight.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class FlightConfig(
    val schemaVersion: Int,
    val rulesVersion: Int,
    val gameId: String,
    val world: World,
    val physics: Physics,
    val round: Round,
    val gates: Gates,
    val stages: List<Stage>,
    val tutorial: Tutorial,
    val persistence: Persistence,
    val badges: Badges,
) {
    @Serializable data class World(val width: Double, val height: Double, val petX: Double,
        val startY: Double, val radius: Double, val top: Double, val bottom: Double)
    @Serializable data class Physics(val tickRate: Int, val gravity: Double, val flapVelocity: Double,
        val maxFallVelocity: Double, val minFlapTicks: Int, val maxFrameMillis: Long)
    @Serializable data class Round(val durationTicks: Int, val countdownSeconds: Int, val resumeCountdownSeconds: Int)
    @Serializable data class Gates(val firstX: Double, val width: Double, val spacing: Double,
        val firstEasyCount: Int, val firstGap: Double, val maxCenterShift: Double, val edgeMargin: Double)
    @Serializable data class Stage(val fromTick: Int, val speed: Double, val gap: Double)
    @Serializable data class Tutorial(val targetGates: Int)
    @Serializable data class Persistence(val checkpointTicks: Int)
    @Serializable data class Badges(val firstFlightScore: Int, val skilledPilotScore: Int)

    fun validate(): FlightConfig = apply {
        require(schemaVersion == 1 && rulesVersion > 0 && gameId == "flight")
        require(physics.tickRate in 30..240 && physics.gravity.isFinite() && physics.gravity > 0)
        require(physics.flapVelocity.isFinite() && physics.flapVelocity < 0)
        require(physics.maxFallVelocity.isFinite() && physics.maxFallVelocity > 0)
        require(physics.minFlapTicks > 0 && physics.maxFrameMillis in 50..500)
        require(world.width > 0 && world.height > 0 && world.radius > 0)
        require(world.top >= 0 && world.bottom <= world.height && world.bottom > world.top)
        require(world.petX > world.radius && world.petX < world.width - world.radius)
        require(world.startY > world.top + world.radius && world.startY < world.bottom - world.radius)
        require(gates.firstX > world.width && gates.width > 0 && gates.spacing > gates.width + 2 * world.radius)
        require(gates.maxCenterShift >= 0 && gates.edgeMargin >= world.top)
        require(stages.isNotEmpty() && stages.first().fromTick == 0)
        require(stages.zipWithNext().all { (a, b) -> a.fromTick < b.fromTick })
        require((stages.map { it.gap } + gates.firstGap).all {
            it > 2 * world.radius && it + 2 * gates.edgeMargin < world.height
        })
        require(stages.all { it.speed > 0 && it.speed.isFinite() })
        require(round.durationTicks > 0 && round.countdownSeconds > 0 && round.resumeCountdownSeconds > 0)
        require(persistence.checkpointTicks > 0 && tutorial.targetGates > 0)
        require(badges.firstFlightScore > 0 && badges.skilledPilotScore > badges.firstFlightScore)
    }

    companion object {
        fun decode(value: String): FlightConfig = Json.decodeFromString<FlightConfig>(value).validate()
    }
}
