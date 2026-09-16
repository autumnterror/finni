package github.detrig.minigames.fishing.domain

import github.detrig.minigames.fishing.api.FishingHost

internal class FishingInteractor(
    private val repository: FishingRepository,
    val host: FishingHost,
    val engine: FishingEngine,
    private val now: () -> Long,
) {
    suspend fun load(profileId: String) = repository.load(profileId)
    suspend fun start(s: FishingSession) = repository.update(s.profileId) {
        require(it.session == null || it.session.id == s.id) { "An unfinished round already exists" }
        if (it.session?.id == s.id) it else it.copy(session = s)
    }

    suspend fun checkpoint(s: FishingSession) = repository.update(s.profileId) { p ->
        val stored = p.session
        if (stored?.id != s.id || stored.catches.size > s.catches.size) p else p.copy(session = s)
    }

    suspend fun catch(s: FishingSession) = repository.update(s.profileId) { p ->
        val active = p.session
        require(active?.id == s.id)
        val catchId = s.id + ":" + (s.catches.size + 1)
        if (active.catches.any { it.id == catchId }) p
        else FishingProgressRules.catch(p, engine.confirmCatch(s, now()))
    }

    suspend fun finish(s: FishingSession) = repository.update(s.profileId) { p ->
        if (p.lastResult?.sessionId == s.id) p
        else {
            require(p.session?.id == s.id)
            FishingProgressRules.finish(p, s, now())
        }
    }

    suspend fun abandon(profileId: String, sessionId: String) = repository.update(profileId) {
        if (it.session?.id == sessionId) it.copy(session = null) else it
    }
    suspend fun tutorialDone(profileId: String) = repository.update(profileId) { it.copy(tutorialDone = true) }
    suspend fun preferences(profileId: String, preferences: FishingPreferences) = repository.update(profileId) { it.copy(preferences = preferences) }
    suspend fun dismissMigration(profileId: String) = repository.update(profileId) { it.copy(migrationNotice = false) }

    suspend fun deliverEffects(profileId: String): FishingProgress {
        var progress = repository.load(profileId)
        for (effect in progress.pendingEffects) {
            val delta = host.applyPlayEffect(profileId, effect.sessionId)
            progress = repository.update(profileId) { current ->
                current.copy(
                    pendingEffects = current.pendingEffects.filterNot { it.sessionId == effect.sessionId },
                    lastResult = current.lastResult?.let { if (it.sessionId == effect.sessionId) it.copy(happinessDelta = delta) else it },
                )
            }
        }
        return progress
    }
}
