package github.detrig.minigames.fishing.domain

import github.detrig.minigames.fishing.api.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class FishingPersistenceRulesTest {
    private val engine = FishingEngine(testConfig())
    private val repository = MemoryRepository()
    private val host = TestHost()
    private val interactor = FishingInteractor(repository, host, engine) { 1000 }
    private fun pending(id: String = "r") = engine.create(id, "p", 42, 1).let {
        it.copy(phase = FishingPhase.CATCH_PENDING, targetSpawnId = it.fish.first().spawnId, validCasts = 1)
    }

    @Test fun duplicateCatchAndStaleCheckpointCannotDuplicateOrEraseCatch() = runBlocking {
        val s = pending()
        interactor.start(s)
        val first = interactor.catch(s)
        assertEquals(first, interactor.catch(s))
        assertEquals(first, interactor.checkpoint(s))
        assertEquals(1, first.session!!.catches.size)
    }
    @Test fun abandonCreatesNoRecordOrReward() = runBlocking {
        val s = pending()
        interactor.start(s)
        interactor.catch(s)
        val abandoned = interactor.abandon("p", "r")
        assertNull(abandoned.session)
        assertTrue(abandoned.records.isEmpty())
        assertTrue(abandoned.pendingEffects.isEmpty())
    }
    @Test fun finishAndEffectRetryAreIdempotentAcrossAcknowledgementFailure() = runBlocking {
        val s = pending()
        interactor.start(s)
        val caught = interactor.catch(s).session!!
        val finished = caught.copy(phase = FishingPhase.FINISHED)
        val result = interactor.finish(finished)
        assertEquals(result, interactor.finish(finished))
        repository.failNextUpdate = true
        assertTrue(runCatching { interactor.deliverEffects("p") }.isFailure)
        assertEquals(3, host.happiness)
        val retried = interactor.deliverEffects("p")
        assertEquals(3, host.happiness)
        assertTrue(retried.pendingEffects.isEmpty())
        assertEquals(3, retried.lastResult!!.happinessDelta)
    }
    @Test fun idleRoundGivesNoRewardAndZeroIsNotARecord() {
        val s = engine.create("r", "p", 1, 1).copy(phase = FishingPhase.FINISHED)
        val result = FishingProgressRules.finish(FishingProgress(profileId = "p"), s, 10)
        assertTrue(result.pendingEffects.isEmpty())
        assertFalse(result.lastResult!!.newRecord)
        assertTrue(result.records.isEmpty())
    }
    @Test fun equalScoreKeepsEarlierRecordAndVersionsAreSeparate() {
        val s = pending().copy(phase = FishingPhase.FINISHED,
            catches = listOf(FishingCatch("r:1", "crucian", 250, 1)))
        val old = FishingRecord(s.rulesVersion, 250, 1, 1)
        val progress = FishingProgress(profileId = "p", records = listOf(old))
        val tied = FishingProgressRules.finish(progress, s, 20)
        assertEquals(listOf(old), tied.records)
        assertFalse(tied.lastResult!!.newRecord)
        val upgraded = FishingProgressRules.finish(progress, s.copy(rulesVersion = s.rulesVersion + 1), 30)
        assertEquals(2, upgraded.records.size)
    }
    @Test fun resultPreservesPreviousRecordAndNewValueAcrossRetryAndSerialization() {
        val old = FishingRecord(engine.config.rulesVersion, 250, 1, 1)
        val session = pending().copy(phase = FishingPhase.FINISHED,
            catches = listOf(FishingCatch("r:1", "carp", 1200, 20)))
        val progress = FishingProgressRules.finish(FishingProgress(profileId = "p", records = listOf(old)), session, 30)
        val result = progress.lastResult!!
        assertTrue(result.newRecord)
        assertEquals(250, result.previousRecordGrams)
        assertEquals(1200, result.recordGrams)
        assertEquals(progress, FishingProgressRules.finish(progress, session, 40))
        val restored = kotlinx.serialization.json.Json.decodeFromString<FishingProgress>(kotlinx.serialization.json.Json.encodeToString(FishingProgress.serializer(), progress))
        assertEquals(result, restored.lastResult)
    }
    @Test fun newRoundCannotOverwriteAnUnfinishedRound() = runBlocking {
        interactor.start(pending())
        assertEquals("r", interactor.start(pending()).session!!.id)
        assertTrue(runCatching { interactor.start(pending("other")) }.isFailure)
        assertEquals("r", interactor.load("p").session!!.id)
    }

    @Test fun previousAssistedAndTapRecordsSurviveTheNewHoldingRules() {
        val old = listOf(FishingRecord(4, 200, 1, 0), FishingRecord(4, 300, 2, 1, assisted = true))
        val s = pending().copy(phase = FishingPhase.FINISHED, catches = listOf(FishingCatch("r:1", "crucian", 500, 1)))
        val updated = FishingProgressRules.finish(FishingProgress(profileId = "p", records = old), s, 100)
        assertTrue(updated.records.containsAll(old))
        assertEquals(3, updated.records.size)
        assertFalse(updated.lastResult!!.assisted)
        assertEquals(0, updated.lastResult!!.previousRecordGrams)
    }

    private class MemoryRepository : FishingRepository {
        var failNextUpdate = false
        private val profiles = mutableMapOf<String, FishingProgress>()
        override suspend fun load(profileId: String) = profiles[profileId] ?: FishingProgress(profileId = profileId)
        override suspend fun update(profileId: String, transform: (FishingProgress) -> FishingProgress): FishingProgress {
            if (failNextUpdate) { failNextUpdate = false; error("Disk unavailable") }
            return transform(load(profileId)).also { profiles[profileId] = it }
        }
    }
    private class TestHost : FishingHost {
        var happiness = 0
        val applied = mutableSetOf<String>()
        override suspend fun environment() = FishingEnvironment("p", true)
        override fun showUnlockPreview() = Unit
        override suspend fun applyPlayEffect(profileId: String, sessionId: String): Int {
            if (applied.add(sessionId)) happiness += 3
            return 3
        }
        override fun feedbackSettings() = FishingFeedbackSettings()
        override fun saveFeedbackSettings(settings: FishingFeedbackSettings) = Unit
    }
}
