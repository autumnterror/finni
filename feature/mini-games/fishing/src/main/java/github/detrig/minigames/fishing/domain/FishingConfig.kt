package github.detrig.minigames.fishing.domain

import kotlinx.serialization.Serializable

@Serializable
internal data class FishingConfig(
    val rulesVersion: Int,
    val round: RoundRules,
    val input: InputRules,
    val casting: CastingRules,
    val fight: FightRules,
    val population: PopulationRules,
    val tutorial: TutorialRules,
    val species: List<FishRules>,
    val world: WorldRules = WorldRules(),
) {
    fun fish(id: String): FishRules = species.first { it.id == id }
    fun validate(): FishingConfig = apply {
        require(rulesVersion > 0 && round.durationSeconds > 0 && species.size == 6)
        require(species.map { it.id }.distinct().size == species.size)
        require(input.chargeSeconds > 0 && casting.minWorldX < casting.maxWorldX)
        require(fight.warningOff < fight.warningOn && fight.warningOn < fight.maxTension)
        require(fight.releaseCoolingPerSecond > 0 && population.massStepGrams > 0)
        require(world.populationSize in 6..14 && world.reelInterval >= .12)
        require(world.fixedStep in .001..0.02 && world.maxLineLength > 1 && world.landingRadius > 0)
        require(world.maxDepth in 1.5..3.0 && world.sinkSpeed > 0 && world.smallFishHoldSeconds > 0 && world.largeFishHoldSeconds > world.smallFishHoldSeconds && world.objectReelSpeed > 0)
        require(world.turnMinSeconds > 0 && world.turnMaxSeconds >= world.turnMinSeconds)
        require(world.cameraFollowDepth > 0 && world.cameraFollowDepth < 1 && world.cameraSpeed > 0)
        require(world.maxLineLength > world.maxDepth - world.rodDepth)
        require(world.swimSpeed > 0 && world.verticalSwimSpeed >= 0)
        require(world.catchRadiusX > 0 && world.catchRadiusY > 0 && world.emptyLiftImpulse > 0)
        require(world.postReelSinkMultiplier > 0 && world.postReelSinkMultiplier < 1)
        require(world.bombAfterSeconds >= 15 && world.bombVisibleSeconds >= 1.5)
        species.forEach {
            require(it.massGrams.min > 0 && it.massGrams.max >= it.massGrams.min)
            require(it.tensionPerSecond > 0 && it.worldXPatrol.size == 2 && it.waterDepth.size == 2)
            require(it.waterDepth[0] >= .09 && it.waterDepth[0] < it.waterDepth[1] && it.waterDepth[1] <= world.maxDepth)
            it.burst?.let { burst -> require(burst.periodSeconds > burst.durationSeconds && burst.firstAtSeconds >= fight.burstWarningSeconds) }
        }
    }
}
@Serializable internal data class WorldRules(
    val populationSize: Int = 10, val fixedStep: Double = .01,
    val rodX: Double = .34, val rodDepth: Double = -.15,
    val landingDepth: Double = .035, val landingRadius: Double = .045,
    val maxDepth: Double = 2.35, val maxLineLength: Double = 2.7,
    val sinkSpeed: Double = .11, val currentSpeed: Double = .006,
    val flightMin: Double = .35, val flightMax: Double = .85,
    val reelInterval: Double = .12,
    val smallFishHoldSeconds: Double = 3.0, val largeFishHoldSeconds: Double = 8.0,
    val objectReelSpeed: Double = .32,
    val impulseSpeed: Double = .85,
    val turnMinSeconds: Double = 3.0, val turnMaxSeconds: Double = 7.0,
    val swimSpeed: Double = .045, val verticalSwimSpeed: Double = .018,
    val cameraFollowDepth: Double = .58, val cameraSpeed: Double = 1.4,
    val mouthOffset: Double = .043,
    val catchBodyOffset: Double = .018, val catchRadiusX: Double = .036, val catchRadiusY: Double = .025,
    val emptyLiftImpulse: Double = .085, val postReelSinkMultiplier: Double = .7,
    val objectRadius: Double = .032, val bombAfterSeconds: Double = 15.0,
    val bombVisibleSeconds: Double = 1.5, val bombPopSeconds: Double = .7,
    val fastReturnSeconds: Double = 1.2, val lostReturnSeconds: Double = .75,
    val junkReleaseSeconds: Double = .55,
)
@Serializable internal data class RoundRules(
    val durationSeconds: Double, val countdownSeconds: Double,
    val catchReleaseSeconds: Double, val resumeCountdownSeconds: Double,
    val catchSummaryVisibleSeconds: Double,
)
@Serializable internal data class InputRules(val chargeSeconds: Double)
@Serializable internal data class CastingRules(val minWorldX: Double, val maxWorldX: Double)
@Serializable internal data class FightRules(
    val initialTension: Double, val maxTension: Double, val warningOn: Double, val warningOff: Double,
    val releaseCoolingPerSecond: Double,
    val burstReelMultiplier: Double, val burstWarningSeconds: Double,
    val escapeAfterContinuousReleaseSeconds: Double,
    val slackHintAfterSeconds: Double,
    val criticalOn: Double = 90.0,
)
@Serializable internal data class PopulationRules(
    val respawnDelaySeconds: Double, val massStepGrams: Int,
)
@Serializable internal data class TutorialRules(
    val forcedSpeciesId: String, val forcedMassGrams: Int, val maxTension: Double,
    val minimumReleaseSeconds: Double,
)
@Serializable internal data class FishRules(
    val id: String, val nameRu: String, val massGrams: MassRange,
    val tensionPerSecond: Double,
    val burst: BurstRules? = null, val worldXPatrol: List<Double>, val waterDepth: List<Double>,
)
@Serializable internal data class MassRange(val min: Int, val max: Int)
@Serializable internal data class BurstRules(
    val firstAtSeconds: Double, val periodSeconds: Double, val durationSeconds: Double, val tensionPerSecond: Double,
)
