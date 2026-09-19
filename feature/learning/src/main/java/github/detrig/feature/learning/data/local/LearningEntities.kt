package github.detrig.feature.learning.data.local

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "learning_actions",
    primaryKeys = ["profileId", "actionId"],
    indices = [Index(value = ["profileId", "actionType"])],
)
data class LearningActionEntity(
    val profileId: String,
    val actionId: String,
    val actionType: String,
    val gamePeriod: Long,
    val sourceOperationId: String?,
    val payloadFingerprint: String,
    val catalogVersion: Int,
)

@Entity(
    tableName = "learning_metric_occurrences",
    primaryKeys = ["profileId", "metricId", "actionId"],
    indices = [Index(value = ["profileId", "metricId", "gamePeriod"])],
)
data class LearningMetricOccurrenceEntity(
    val profileId: String,
    val metricId: String,
    val actionId: String,
    val gamePeriod: Long,
)

@Entity(
    tableName = "learning_metric_progress",
    primaryKeys = ["profileId", "metricId"],
)
data class LearningMetricProgressEntity(
    val profileId: String,
    val metricId: String,
    val progressSteps: Int,
    val qualifyingRepeats: Int,
    val distinctPeriods: Int,
    val currentStreak: Int,
    val lastQualifyingPeriod: Long?,
)

@Entity(
    tableName = "achievement_unlocks",
    primaryKeys = ["profileId", "achievementId"],
    indices = [Index(value = ["profileId", "sourceActionId"])],
)
data class AchievementUnlockEntity(
    val profileId: String,
    val achievementId: String,
    val sourceActionId: String,
    val unlockedAtGamePeriod: Long,
    val xpGrantId: String,
)

@Entity(
    tableName = "learning_explanations",
    primaryKeys = ["profileId", "explanationId"],
)
data class LearningExplanationEntity(
    val profileId: String,
    val explanationId: String,
)

@Entity(
    tableName = "achievement_xp_outbox",
    primaryKeys = ["grantId"],
    indices = [Index(value = ["profileId", "delivered"])],
)
data class AchievementXpOutboxEntity(
    val grantId: String,
    val profileId: String,
    val achievementId: String,
    val amount: Int,
    val delivered: Boolean,
)
