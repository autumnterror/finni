package github.detrig.minigames.common.domain.model

/**
 * Класс для экрана выбора набора вопросов
 **/
data class QuestionPackModel(
    val id: String,
    val title: String,
    val description: String?,
    val questionsCount: Int,
    val difficulty: QuestionDifficultyModel,
    val bestScore: Int?,
    val accuracyPercent: Int?,
)

enum class QuestionDifficultyModel {
    EASY,
    MEDIUM,
    HARD,
}