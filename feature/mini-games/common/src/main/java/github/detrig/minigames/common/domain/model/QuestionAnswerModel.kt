package github.detrig.minigames.common.domain.model

sealed interface QuestionAnswerModel {

    data class SingleChoice(
        val options: List<AnswerOptionModel>,
        val correctOptionId: String,
    ) : QuestionAnswerModel

    data class MultipleChoice(
        val options: List<AnswerOptionModel>,
        val correctOptionIds: Set<String>,
    ) : QuestionAnswerModel

    data class Text(
        val acceptedAnswers: List<String>,
    ) : QuestionAnswerModel
}
