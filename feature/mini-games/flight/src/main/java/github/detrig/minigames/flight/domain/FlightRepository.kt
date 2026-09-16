package github.detrig.minigames.flight.domain

interface FlightRepository {
    suspend fun load(profileId: String): FlightProgress
    suspend fun begin(session: FlightSession): FlightProgress
    suspend fun checkpoint(session: FlightSession)
    suspend fun finish(session: FlightSession, now: Long): FlightProgress
    suspend fun abandon(profileId: String, sessionId: String): FlightProgress
    suspend fun markTutorialSeen(profileId: String): FlightProgress
    suspend fun acknowledgeEffect(profileId: String, sessionId: String, delta: Int): FlightProgress
    suspend fun interruptIncompatible(profileId: String): FlightProgress
}
