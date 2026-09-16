package github.detrig.minigames.flight.domain

import github.detrig.minigames.flight.api.FlightCompletion
import github.detrig.minigames.flight.api.FlightHost

internal class FlightInteractor(
    val repository: FlightRepository,
    val host: FlightHost,
) {
    suspend fun deliverEffects(profileId: String): FlightProgress {
        val progress = repository.load(profileId)
        for (result in progress.pendingEffects) {
            val environment = host.environment()
            check(environment.profileId == profileId && environment.unlocked)
            val delta = host.applyPlayEffect(FlightCompletion(profileId, result.sessionId,
                result.activeMillis, result.flapCount))
            repository.acknowledgeEffect(profileId, result.sessionId, delta)
        }
        return repository.load(profileId)
    }
}
