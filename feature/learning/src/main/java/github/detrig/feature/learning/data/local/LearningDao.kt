package github.detrig.feature.learning.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningDao {
    @Query("SELECT * FROM learning_actions WHERE profileId = :profileId AND actionId = :actionId")
    suspend fun getAction(profileId: String, actionId: String): LearningActionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAction(action: LearningActionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOccurrence(occurrence: LearningMetricOccurrenceEntity): Long

    @Query("SELECT gamePeriod FROM learning_metric_occurrences WHERE profileId = :profileId AND metricId = :metricId ORDER BY gamePeriod, actionId")
    suspend fun getQualifyingPeriods(profileId: String, metricId: String): List<Long>

    @Upsert
    suspend fun upsertMetricProgress(progress: LearningMetricProgressEntity)

    @Query("SELECT * FROM learning_metric_progress WHERE profileId = :profileId ORDER BY metricId")
    fun observeMetricProgress(profileId: String): Flow<List<LearningMetricProgressEntity>>

    @Query("SELECT * FROM achievement_unlocks WHERE profileId = :profileId AND achievementId = :achievementId")
    suspend fun getUnlock(profileId: String, achievementId: String): AchievementUnlockEntity?

    @Query("SELECT * FROM achievement_unlocks WHERE profileId = :profileId AND sourceActionId = :actionId ORDER BY achievementId")
    suspend fun getUnlocksByAction(profileId: String, actionId: String): List<AchievementUnlockEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnlock(unlock: AchievementUnlockEntity): Long

    @Query("SELECT * FROM achievement_unlocks WHERE profileId = :profileId ORDER BY achievementId")
    fun observeUnlocks(profileId: String): Flow<List<AchievementUnlockEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExplanation(explanation: LearningExplanationEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertXpReward(reward: AchievementXpOutboxEntity): Long

    @Query("SELECT * FROM achievement_xp_outbox WHERE profileId = :profileId AND delivered = 0 ORDER BY grantId")
    suspend fun getPendingXpRewards(profileId: String): List<AchievementXpOutboxEntity>

    @Query("SELECT * FROM achievement_xp_outbox WHERE profileId = :profileId AND delivered = 0 ORDER BY grantId")
    fun observePendingXpRewards(profileId: String): Flow<List<AchievementXpOutboxEntity>>

    @Query("UPDATE achievement_xp_outbox SET delivered = 1 WHERE grantId = :grantId AND delivered = 0")
    suspend fun markXpRewardDelivered(grantId: String): Int

    @Query("DELETE FROM achievement_xp_outbox WHERE profileId = :profileId")
    suspend fun deleteXpRewards(profileId: String)

    @Query("DELETE FROM achievement_unlocks WHERE profileId = :profileId")
    suspend fun deleteUnlocks(profileId: String)

    @Query("DELETE FROM learning_metric_progress WHERE profileId = :profileId")
    suspend fun deleteMetricProgress(profileId: String)

    @Query("DELETE FROM learning_metric_occurrences WHERE profileId = :profileId")
    suspend fun deleteOccurrences(profileId: String)

    @Query("DELETE FROM learning_actions WHERE profileId = :profileId")
    suspend fun deleteActions(profileId: String)

    @Query("DELETE FROM learning_explanations WHERE profileId = :profileId")
    suspend fun deleteExplanations(profileId: String)
}
