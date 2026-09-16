package github.detrig.minigames.flight.domain

import kotlinx.serialization.Serializable

@Serializable
enum class FlightOutcome { LANDED, FINISHED, ABANDONED }

@Serializable
data class FlightGate(
    val id: Int,
    val x: Double,
    val center: Double,
    val gap: Double,
    val passed: Boolean = false,
    val missed: Boolean = false,
)

@Serializable
data class FlightSession(
    val id: String,
    val profileId: String,
    val rulesVersion: Int,
    val appearanceId: String,
    val seed: Int,
    val rng: Int,
    val startedAtMillis: Long,
    val training: Boolean = false,
    val started: Boolean = false,
    val tick: Int = 0,
    val y: Double,
    val velocity: Double = 0.0,
    val flapCount: Int = 0,
    val lastFlapTick: Int = -1000,
    val score: Int = 0,
    val gates: List<FlightGate> = emptyList(),
    val nextGateId: Int = 0,
    val lastCenter: Double,
    val outcome: FlightOutcome? = null,
)
