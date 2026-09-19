package github.detrig.feature.learning.domain

@JvmInline
value class LearningActionType(val value: String) {
    init {
        require(value.isNotBlank()) { "Learning action type must not be blank" }
    }
}

/**
 * Контекст конкретного доменного действия. Новые типизированные контексты
 * объявляются в learning-модуле рядом с правилом, которое их оценивает.
 */
interface LearningActionContext {
    /** Стабильное представление payload для проверки конфликта actionId. */
    val fingerprint: String
}

data class OpaqueLearningActionContext(
    override val fingerprint: String,
) : LearningActionContext {
    init {
        require(fingerprint.isNotBlank()) { "Learning action fingerprint must not be blank" }
    }
}

data class LearningAction(
    val actionId: String,
    val profileId: String,
    val gamePeriod: Long,
    val type: LearningActionType,
    val context: LearningActionContext,
    val sourceOperationId: String? = null,
) {
    init {
        require(actionId.isNotBlank()) { "Learning action ID must not be blank" }
        require(profileId.isNotBlank()) { "Profile ID must not be blank" }
        require(gamePeriod >= 0) { "Game period must not be negative" }
        require(context.fingerprint.isNotBlank()) { "Learning action fingerprint must not be blank" }
        require(sourceOperationId == null || sourceOperationId.isNotBlank()) {
            "Source operation ID must be null or non-blank"
        }
    }
}

enum class AchievementStage {
    INTRODUCTION,
    LEARNED,
}

data class AchievementDefinition(
    val achievementId: String,
    val topicId: String,
    val metricId: String,
    val stage: AchievementStage,
    val requiredProgressSteps: Int,
    val xpReward: Int,
    val childTitle: String,
    val childDescription: String,
    val parentText: String,
    val order: Int,
) {
    init {
        require(achievementId.isNotBlank())
        require(topicId.isNotBlank())
        require(metricId.isNotBlank())
        require(requiredProgressSteps > 0)
        require(xpReward > 0)
        require(childTitle.isNotBlank())
        require(childDescription.isNotBlank())
        require(parentText.isNotBlank())
        require(order >= 0)
    }
}

data class MetricProgress(
    val metricId: String,
    val progressSteps: Int,
    val qualifyingRepeats: Int,
    val distinctPeriods: Int,
    val currentStreak: Int,
    val lastQualifyingPeriod: Long?,
) {
    init {
        require(metricId.isNotBlank())
        require(progressSteps >= 0)
        require(qualifyingRepeats >= 0)
        require(distinctPeriods >= 0)
        require(currentStreak >= 0)
    }

    companion object {
        fun empty(metricId: String): MetricProgress = MetricProgress(
            metricId = metricId,
            progressSteps = 0,
            qualifyingRepeats = 0,
            distinctPeriods = 0,
            currentStreak = 0,
            lastQualifyingPeriod = null,
        )
    }
}

data class AchievementUnlock(
    val definition: AchievementDefinition,
    val sourceActionId: String,
    val unlockedAtGamePeriod: Long,
    val xpGrantId: String,
)

data class AchievementProgress(
    val definition: AchievementDefinition,
    val metricProgress: MetricProgress,
    val unlock: AchievementUnlock?,
) {
    val isUnlocked: Boolean get() = unlock != null
}

data class ParentProgressRow(
    val achievementId: String,
    val topicId: String,
    val stage: AchievementStage,
    val text: String,
    val unlockedAtGamePeriod: Long,
)

data class PendingXpReward(
    val grantId: String,
    val profileId: String,
    val achievementId: String,
    val amount: Int,
)

sealed interface RecordLearningResult {
    data class Processed(
        val actionId: String,
        val updatedMetrics: List<MetricProgress>,
        val newlyUnlocked: List<AchievementUnlock>,
    ) : RecordLearningResult

    data class AlreadyProcessed(
        val actionId: String,
        val achievementsUnlockedByAction: List<AchievementUnlock>,
    ) : RecordLearningResult

    data class OperationIdConflict(val actionId: String) : RecordLearningResult

    data class UnsupportedAction(val actionType: LearningActionType) : RecordLearningResult
}

data class XpDeliveryResult(
    val delivered: Int,
    val remaining: Int,
)
