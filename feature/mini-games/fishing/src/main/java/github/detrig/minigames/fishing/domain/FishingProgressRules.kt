package github.detrig.minigames.fishing.domain

/** Чистые правила улова и результата, общие для сохранения и тестов. */
internal object FishingProgressRules {
    fun catch(progress: FishingProgress, confirmed: FishingSession): FishingProgress {
        require(!confirmed.tutorial)
        val catch = confirmed.catches.last()
        if (progress.session?.catches?.any { it.id == catch.id } == true) return progress
        return progress.copy(session = confirmed)
    }

    fun finish(progress: FishingProgress, session: FishingSession, now: Long): FishingProgress {
        if (progress.lastResult?.sessionId == session.id) return progress
        require(!session.tutorial && session.phase == FishingPhase.FINISHED)
        val oldRecord = progress.records.firstOrNull { it.rulesVersion == session.rulesVersion && !it.assisted }
        val isRecord = session.totalGrams > (oldRecord?.grams ?: 0)
        val result = FishingResult(session.id, session.rulesVersion, session.totalGrams, session.catches.size,
            now, isRecord, session.validCasts > 0,
            previousRecordGrams = oldRecord?.grams ?: 0,
            recordGrams = if (isRecord) session.totalGrams else oldRecord?.grams ?: 0,
            largestGrams = session.catches.maxOfOrNull { it.grams } ?: 0,
            junkCount = session.junkCount, lossCount = session.lossCount, reason = session.finishReason)
        val records = if (isRecord) progress.records.filterNot { it.rulesVersion == session.rulesVersion && !it.assisted } +
            FishingRecord(session.rulesVersion, result.grams, result.count, now) else progress.records
        return progress.copy(session = null, lastResult = result, records = records,
            pendingEffects = if (result.eligibleForHappiness) progress.pendingEffects + result else progress.pendingEffects)
    }
}
