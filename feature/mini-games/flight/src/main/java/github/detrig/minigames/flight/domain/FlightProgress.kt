package github.detrig.minigames.flight.domain

import kotlinx.serialization.Serializable

@Serializable
data class FlightRecord(val rulesVersion: Int, val score: Int, val ticks: Int,
    val sessionId: String, val atMillis: Long)

@Serializable
data class FlightResult(val sessionId: String, val rulesVersion: Int, val score: Int,
    val activeMillis: Long, val flapCount: Int, val outcome: FlightOutcome,
    val newRecord: Boolean, val newBadges: Set<String>, val happinessDelta: Int? = null)

@Serializable
data class FlightProgress(
    val schemaVersion: Int = 1,
    val profileId: String,
    val tutorialSeen: Boolean = false,
    val active: FlightSession? = null,
    val records: Map<Int, FlightRecord> = emptyMap(),
    val badges: Set<String> = emptySet(),
    val lastResult: FlightResult? = null,
    val pendingEffects: List<FlightResult> = emptyList(),
) {
    fun finish(session: FlightSession, config: FlightConfig, now: Long): FlightProgress {
        require(session.profileId == profileId && session.rulesVersion == config.rulesVersion)
        require(!session.training && session.outcome != null)
        if (active?.id != session.id) return this
        if (session.outcome == FlightOutcome.ABANDONED) return copy(active = null)
        val record = records[session.rulesVersion]
        val isRecord = session.score > (record?.score ?: 0)
        val earned = buildSet {
            if (session.score >= config.badges.firstFlightScore) add("first_flight")
            if (session.score >= config.badges.skilledPilotScore) add("skilled_pilot")
            if (session.outcome == FlightOutcome.FINISHED) add("cloud_trail")
        }
        val result = FlightResult(session.id, session.rulesVersion, session.score,
            session.tick * 1000L / config.physics.tickRate, session.flapCount,
            session.outcome, isRecord, earned - badges)
        return copy(active = null, records = if (isRecord) records + (session.rulesVersion to
            FlightRecord(session.rulesVersion, session.score, session.tick, session.id, now)) else records,
            badges = badges + earned, lastResult = result, pendingEffects = pendingEffects + result)
    }
}
