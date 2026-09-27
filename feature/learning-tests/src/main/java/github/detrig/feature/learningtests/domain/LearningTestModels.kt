package github.detrig.feature.learningtests.domain

enum class LearningTestDifficulty(
    val title: String,
    val xpPerCorrectAnswer: Int,
    val perfectRewardRub: Long,
) {
    SIMPLE("Простая", 3, 5),
    MEDIUM("Средняя", 5, 10),
    HARD("Сложная", 7, 15),
}

data class LearningTestQuestion(
    val prompt: String,
    val options: List<String>,
    val correctOptionIndex: Int,
)

data class LearningTestDefinition(
    val id: String,
    val title: String,
    val difficulty: LearningTestDifficulty,
    val questions: List<LearningTestQuestion>,
) {
    val maxXp: Int get() = difficulty.xpPerCorrectAnswer * questions.size
}

data class LearningTestAnswer(
    val selectedOptionIndex: Int,
    val isCorrect: Boolean,
)

data class LearningTestAttempt(
    val gameDay: Long,
    val questionIndex: Int,
    val mistakeCount: Int,
    val isComplete: Boolean,
    val isPerfect: Boolean,
)

enum class LearningTestOfferStatus { NEW, IN_PROGRESS, COMPLETED_WITH_MISTAKES }

data class LearningTestOffer(
    val test: LearningTestDefinition,
    val status: LearningTestOfferStatus,
    val questionIndex: Int? = null,
    val mistakeCount: Int = 0,
)

data class DailyLearningTests(
    val gameDay: Long,
    val offers: List<LearningTestOffer>,
)

data class LearningTestSession(
    val test: LearningTestDefinition,
    val attempt: LearningTestAttempt,
    val question: LearningTestQuestion?,
    val answer: LearningTestAnswer?,
    val answers: Map<Int, LearningTestAnswer>,
)
