package github.detrig.feature.learningtests.data.local

import androidx.room.Entity

@Entity(
    tableName = "learning_test_daily_offers",
    primaryKeys = ["profileId", "gameDay", "testId"],
)
data class LearningTestDailyOfferEntity(
    val profileId: String,
    val gameDay: Long,
    val position: Int,
    val testId: String,
)

@Entity(
    tableName = "learning_test_attempts",
    primaryKeys = ["profileId", "testId", "gameDay"],
)
data class LearningTestAttemptEntity(
    val profileId: String,
    val testId: String,
    val gameDay: Long,
    val questionIndex: Int,
    val mistakeCount: Int,
    val isComplete: Boolean,
    val isPerfect: Boolean,
)

@Entity(
    tableName = "learning_test_answers",
    primaryKeys = ["profileId", "testId", "gameDay", "questionIndex"],
)
data class LearningTestAnswerEntity(
    val profileId: String,
    val testId: String,
    val gameDay: Long,
    val questionIndex: Int,
    val selectedOptionIndex: Int,
    val isCorrect: Boolean,
)

@Entity(
    tableName = "learning_test_mastery",
    primaryKeys = ["profileId", "testId"],
)
data class LearningTestMasteryEntity(
    val profileId: String,
    val testId: String,
    val completedAtGameDay: Long,
)

@Entity(
    tableName = "learning_test_question_xp_outbox",
    primaryKeys = ["profileId", "testId", "questionIndex"],
)
data class LearningTestQuestionXpOutboxEntity(
    val profileId: String,
    val testId: String,
    val questionIndex: Int,
    val amount: Int,
    val delivered: Boolean,
)

@Entity(
    tableName = "learning_test_completion_reward_outbox",
    primaryKeys = ["profileId", "testId"],
)
data class LearningTestCompletionRewardOutboxEntity(
    val profileId: String,
    val testId: String,
    val amountRub: Long,
    val delivered: Boolean,
)
