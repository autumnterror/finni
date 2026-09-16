package github.detrig.minigames.flight.data

import github.detrig.core.database.RoomTransactionRunner
import github.detrig.minigames.flight.domain.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

internal class RoomFlightRepository(
    private val dao: FlightDao,
    private val transactionRunner: RoomTransactionRunner,
    private val config: FlightConfig,
) : FlightRepository {
    private val mutex = Mutex()
    private val json = Json { encodeDefaults = true }

    override suspend fun load(profileId: String): FlightProgress = withContext(Dispatchers.IO) {
        mutex.withLock { read(profileId) }
    }

    private suspend fun read(profileId: String): FlightProgress {
        val row = dao.find(profileId) ?: return FlightProgress(profileId = profileId)
        return json.decodeFromString<FlightProgress>(row.payload).also {
            check(it.schemaVersion == 1 && it.profileId == profileId) { "Unsupported flight save" }
            check(it.active == null || it.active.profileId == profileId)
        }
    }

    private suspend fun update(profileId: String, block: (FlightProgress) -> FlightProgress): FlightProgress =
        withContext(Dispatchers.IO) { mutex.withLock {
            transactionRunner.runInTransaction {
                val before = read(profileId)
                val after = block(before)
                if (after != before) dao.save(FlightProgressEntity(profileId, json.encodeToString(after)))
                after
            }
        } }

    override suspend fun begin(session: FlightSession) = update(session.profileId) {
        require(!session.training && session.outcome == null)
        check(it.active == null || it.active.id == session.id) { "Abandon previous flight first" }
        it.copy(active = session)
    }

    override suspend fun checkpoint(session: FlightSession) {
        require(!session.training)
        update(session.profileId) { progress ->
            val active = progress.active
            if (active?.id == session.id && active.outcome == null &&
                (session.tick > active.tick || session.tick == active.tick && session.flapCount >= active.flapCount))
                progress.copy(active = session) else progress
        }
    }

    override suspend fun finish(session: FlightSession, now: Long) =
        update(session.profileId) { it.finish(session, config, now) }

    override suspend fun abandon(profileId: String, sessionId: String) = update(profileId) {
        if (it.active?.id == sessionId) it.copy(active = null) else it
    }

    override suspend fun markTutorialSeen(profileId: String) = update(profileId) { it.copy(tutorialSeen = true) }

    override suspend fun acknowledgeEffect(profileId: String, sessionId: String, delta: Int) = update(profileId) {
        it.copy(pendingEffects = it.pendingEffects.filterNot { effect -> effect.sessionId == sessionId },
            lastResult = it.lastResult?.let { result ->
                if (result.sessionId == sessionId) result.copy(happinessDelta = delta) else result
            })
    }

    override suspend fun interruptIncompatible(profileId: String) = update(profileId) {
        if (it.active != null && it.active.rulesVersion != config.rulesVersion) it.copy(active = null) else it
    }
}
