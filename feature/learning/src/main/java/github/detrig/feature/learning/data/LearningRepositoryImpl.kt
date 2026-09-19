package github.detrig.feature.learning.data

import github.detrig.core.database.RoomTransactionRunner
import github.detrig.feature.learning.data.local.AchievementUnlockEntity
import github.detrig.feature.learning.data.local.AchievementXpOutboxEntity
import github.detrig.feature.learning.data.local.LearningDao
import github.detrig.feature.learning.data.local.LearningExplanationEntity
import github.detrig.feature.learning.data.local.LearningMetricOccurrenceEntity
import github.detrig.feature.learning.data.local.toDomain
import github.detrig.feature.learning.data.local.toEntity
import github.detrig.feature.learning.domain.AchievementCatalog
import github.detrig.feature.learning.domain.AchievementProgress
import github.detrig.feature.learning.domain.AchievementUnlock
import github.detrig.feature.learning.domain.LearningAction
import github.detrig.feature.learning.domain.LearningConfig
import github.detrig.feature.learning.domain.LearningRuleEngine
import github.detrig.feature.learning.domain.MetricProgress
import github.detrig.feature.learning.domain.ParentProgressRow
import github.detrig.feature.learning.domain.PendingXpReward
import github.detrig.feature.learning.domain.RecordLearningResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

internal class LearningRepositoryImpl(
    private val dao: LearningDao,
    private val transactionRunner: RoomTransactionRunner,
    private val config: LearningConfig,
    private val catalog: AchievementCatalog,
    private val ruleEngine: LearningRuleEngine,
) : LearningRepository {

    override suspend fun record(action: LearningAction): RecordLearningResult =
        transactionRunner.runInTransaction {
            val incoming = action.toEntity(config.catalogVersion)
            dao.getAction(action.profileId, action.actionId)?.let { existing ->
                return@runInTransaction if (existing == incoming) {
                    RecordLearningResult.AlreadyProcessed(
                        actionId = action.actionId,
                        achievementsUnlockedByAction = dao.getUnlocksByAction(action.profileId, action.actionId)
                            .mapNotNull { it.toDomain(catalog) },
                    )
                } else {
                    RecordLearningResult.OperationIdConflict(action.actionId)
                }
            }

            val matchingRules = ruleEngine.rulesFor(action.type)
            if (matchingRules.isEmpty()) {
                return@runInTransaction RecordLearningResult.UnsupportedAction(action.type)
            }

            dao.insertAction(incoming)
            val updatedMetrics = matchingRules.map { rule ->
                dao.insertOccurrence(
                    LearningMetricOccurrenceEntity(
                        profileId = action.profileId,
                        metricId = rule.metricId,
                        actionId = action.actionId,
                        gamePeriod = action.gamePeriod,
                    )
                )
                ruleEngine.evaluate(
                    rule = rule,
                    qualifyingPeriods = dao.getQualifyingPeriods(action.profileId, rule.metricId),
                ).also { progress ->
                    dao.upsertMetricProgress(progress.toEntity(action.profileId))
                }
            }

            val newlyUnlocked = updatedMetrics.flatMap { progress ->
                unlockReachedAchievements(action, progress)
            }

            RecordLearningResult.Processed(
                actionId = action.actionId,
                updatedMetrics = updatedMetrics,
                newlyUnlocked = newlyUnlocked,
            )
        }

    override fun observeAchievements(profileId: String): Flow<List<AchievementProgress>> = combine(
        dao.observeMetricProgress(profileId),
        dao.observeUnlocks(profileId),
    ) { progressEntities, unlockEntities ->
        val progressByMetric = progressEntities.associate { it.metricId to it.toDomain() }
        val unlockByAchievement = unlockEntities.mapNotNull { it.toDomain(catalog) }
            .associateBy { it.definition.achievementId }
        catalog.definitions.map { definition ->
            AchievementProgress(
                definition = definition,
                metricProgress = progressByMetric[definition.metricId]
                    ?: MetricProgress.empty(definition.metricId),
                unlock = unlockByAchievement[definition.achievementId],
            )
        }
    }

    override fun observeParentRows(profileId: String): Flow<List<ParentProgressRow>> =
        observeAchievements(profileId).map { achievements ->
            achievements.mapNotNull { achievement ->
                achievement.unlock?.let { unlock ->
                    ParentProgressRow(
                        achievementId = achievement.definition.achievementId,
                        topicId = achievement.definition.topicId,
                        stage = achievement.definition.stage,
                        text = achievement.definition.parentText,
                        unlockedAtGamePeriod = unlock.unlockedAtGamePeriod,
                    )
                }
            }
        }

    override fun observePendingXpRewards(profileId: String): Flow<List<PendingXpReward>> =
        dao.observePendingXpRewards(profileId).map { rewards -> rewards.map { it.toDomain() } }

    override suspend fun pendingXpRewards(profileId: String): List<PendingXpReward> =
        dao.getPendingXpRewards(profileId).map { it.toDomain() }

    override suspend fun markXpRewardDelivered(grantId: String): Boolean =
        transactionRunner.runInTransaction { dao.markXpRewardDelivered(grantId) == 1 }

    override suspend fun claimFirstExplanation(profileId: String, explanationId: String): Boolean {
        require(profileId.isNotBlank()) { "Profile ID must not be blank" }
        require(explanationId.isNotBlank()) { "Explanation ID must not be blank" }
        return transactionRunner.runInTransaction {
            dao.insertExplanation(LearningExplanationEntity(profileId, explanationId)) != -1L
        }
    }

    override suspend fun resetProfile(profileId: String) {
        require(profileId.isNotBlank()) { "Profile ID must not be blank" }
        transactionRunner.runInTransaction {
            dao.deleteXpRewards(profileId)
            dao.deleteUnlocks(profileId)
            dao.deleteMetricProgress(profileId)
            dao.deleteOccurrences(profileId)
            dao.deleteActions(profileId)
            dao.deleteExplanations(profileId)
        }
    }

    private suspend fun unlockReachedAchievements(
        action: LearningAction,
        progress: MetricProgress,
    ): List<AchievementUnlock> = buildList {
        catalog.definitionsForMetric(progress.metricId)
            .filter { it.requiredProgressSteps <= progress.progressSteps }
            .forEach { definition ->
                if (dao.getUnlock(action.profileId, definition.achievementId) != null) return@forEach
                val grantId = "achievement-xp:${action.profileId}:${definition.achievementId}"
                val entity = AchievementUnlockEntity(
                    profileId = action.profileId,
                    achievementId = definition.achievementId,
                    sourceActionId = action.actionId,
                    unlockedAtGamePeriod = action.gamePeriod,
                    xpGrantId = grantId,
                )
                if (dao.insertUnlock(entity) != -1L) {
                    dao.insertXpReward(
                        AchievementXpOutboxEntity(
                            grantId = grantId,
                            profileId = action.profileId,
                            achievementId = definition.achievementId,
                            amount = definition.xpReward,
                            delivered = false,
                        )
                    )
                    add(requireNotNull(entity.toDomain(catalog)))
                }
            }
    }
}
