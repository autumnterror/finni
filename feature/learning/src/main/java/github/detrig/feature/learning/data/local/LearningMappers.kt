package github.detrig.feature.learning.data.local

import github.detrig.feature.learning.domain.AchievementCatalog
import github.detrig.feature.learning.domain.AchievementUnlock
import github.detrig.feature.learning.domain.LearningAction
import github.detrig.feature.learning.domain.MetricProgress
import github.detrig.feature.learning.domain.PendingXpReward

internal fun LearningAction.toEntity(catalogVersion: Int): LearningActionEntity = LearningActionEntity(
    profileId = profileId,
    actionId = actionId,
    actionType = type.value,
    gamePeriod = gamePeriod,
    sourceOperationId = sourceOperationId,
    payloadFingerprint = context.fingerprint,
    catalogVersion = catalogVersion,
)

internal fun MetricProgress.toEntity(profileId: String): LearningMetricProgressEntity =
    LearningMetricProgressEntity(
        profileId = profileId,
        metricId = metricId,
        progressSteps = progressSteps,
        qualifyingRepeats = qualifyingRepeats,
        distinctPeriods = distinctPeriods,
        currentStreak = currentStreak,
        lastQualifyingPeriod = lastQualifyingPeriod,
    )

internal fun LearningMetricProgressEntity.toDomain(): MetricProgress = MetricProgress(
    metricId = metricId,
    progressSteps = progressSteps,
    qualifyingRepeats = qualifyingRepeats,
    distinctPeriods = distinctPeriods,
    currentStreak = currentStreak,
    lastQualifyingPeriod = lastQualifyingPeriod,
)

internal fun AchievementUnlockEntity.toDomain(catalog: AchievementCatalog): AchievementUnlock? {
    val definition = catalog.definition(achievementId) ?: return null
    return AchievementUnlock(
        definition = definition,
        sourceActionId = sourceActionId,
        unlockedAtGamePeriod = unlockedAtGamePeriod,
        xpGrantId = xpGrantId,
    )
}

internal fun AchievementXpOutboxEntity.toDomain(): PendingXpReward = PendingXpReward(
    grantId = grantId,
    profileId = profileId,
    achievementId = achievementId,
    amount = amount,
)
