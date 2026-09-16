package github.detrig.minigames.common.domain.model

data class QuestionModel(
    val id: String,
    val text: String,
    val answer: QuestionAnswerModel,
    val explanation: String?,
)