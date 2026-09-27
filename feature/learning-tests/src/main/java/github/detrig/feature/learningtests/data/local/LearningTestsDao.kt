package github.detrig.feature.learningtests.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface LearningTestsDao {
    @Query("SELECT * FROM learning_test_daily_offers WHERE profileId = :profileId AND gameDay = :gameDay ORDER BY position")
    suspend fun getDailyOffers(profileId: String, gameDay: Long): List<LearningTestDailyOfferEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDailyOffers(offers: List<LearningTestDailyOfferEntity>)

    @Query("SELECT testId FROM learning_test_mastery WHERE profileId = :profileId")
    suspend fun getMasteredTestIds(profileId: String): List<String>

    @Query("SELECT * FROM learning_test_attempts WHERE profileId = :profileId AND gameDay = :gameDay")
    suspend fun getAttemptsForDay(profileId: String, gameDay: Long): List<LearningTestAttemptEntity>

    @Query("SELECT * FROM learning_test_attempts WHERE profileId = :profileId AND testId = :testId AND gameDay = :gameDay")
    suspend fun getAttempt(profileId: String, testId: String, gameDay: Long): LearningTestAttemptEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAttempt(attempt: LearningTestAttemptEntity): Long

    @Update
    suspend fun updateAttempt(attempt: LearningTestAttemptEntity)

    @Query("SELECT * FROM learning_test_answers WHERE profileId = :profileId AND testId = :testId AND gameDay = :gameDay AND questionIndex = :questionIndex")
    suspend fun getAnswer(profileId: String, testId: String, gameDay: Long, questionIndex: Int): LearningTestAnswerEntity?

    @Query("SELECT * FROM learning_test_answers WHERE profileId = :profileId AND testId = :testId AND gameDay = :gameDay ORDER BY questionIndex")
    suspend fun getAnswersForAttempt(profileId: String, testId: String, gameDay: Long): List<LearningTestAnswerEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAnswer(answer: LearningTestAnswerEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMastery(mastery: LearningTestMasteryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertQuestionXpReward(reward: LearningTestQuestionXpOutboxEntity)

    @Query("SELECT * FROM learning_test_question_xp_outbox WHERE profileId = :profileId AND delivered = 0")
    suspend fun pendingQuestionXp(profileId: String): List<LearningTestQuestionXpOutboxEntity>

    @Query("UPDATE learning_test_question_xp_outbox SET delivered = 1 WHERE profileId = :profileId AND testId = :testId AND questionIndex = :questionIndex")
    suspend fun markQuestionXpDelivered(profileId: String, testId: String, questionIndex: Int)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletionReward(reward: LearningTestCompletionRewardOutboxEntity)

    @Query("SELECT * FROM learning_test_completion_reward_outbox WHERE profileId = :profileId AND delivered = 0")
    suspend fun pendingCompletionRewards(profileId: String): List<LearningTestCompletionRewardOutboxEntity>

    @Query("UPDATE learning_test_completion_reward_outbox SET delivered = 1 WHERE profileId = :profileId AND testId = :testId")
    suspend fun markCompletionRewardDelivered(profileId: String, testId: String)
}
