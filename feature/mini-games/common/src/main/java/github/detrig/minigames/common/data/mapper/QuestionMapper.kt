package github.detrig.minigames.common.data.mapper

import github.detrig.minigames.common.data.database.entity.AnswerOptionEntity
import github.detrig.minigames.common.data.database.entity.QuestionEntity
import github.detrig.minigames.common.data.database.entity.TextAnswerEntity
import github.detrig.minigames.common.domain.model.QuestionAnswerModel
import github.detrig.minigames.common.domain.model.QuestionModel

internal fun QuestionEntity.toModel(
    answerOptions: List<AnswerOptionEntity>,
    textAnswers: List<TextAnswerEntity>,
): QuestionModel {
    return QuestionModel(
        id = id,
        text = text,
        answer = toAnswerModel(
            answerOptions = answerOptions,
            textAnswers = textAnswers,
        ),
        explanation = explanation,
    )
}

internal fun QuestionModel.toEntity(
    gameCode: String,
    questionPackId: String,
    orderIndex: Int,
): QuestionEntity {
    return QuestionEntity(
        id = id,
        gameCode = gameCode,
        questionPackId = questionPackId,
        text = text,
        answerType = answer.toAnswerType().name,
        explanation = explanation,
        orderIndex = orderIndex,
    )
}

internal fun QuestionAnswerModel.toAnswerOptionEntities(
    gameCode: String,
    questionPackId: String,
    questionId: String,
): List<AnswerOptionEntity> {
    return when (this) {
        is QuestionAnswerModel.SingleChoice -> options.mapIndexed { index, answerOption ->
            answerOption.toEntity(
                gameCode = gameCode,
                questionPackId = questionPackId,
                questionId = questionId,
                isCorrect = answerOption.id == correctOptionId,
                orderIndex = index,
            )
        }

        is QuestionAnswerModel.MultipleChoice -> options.mapIndexed { index, answerOption ->
            answerOption.toEntity(
                gameCode = gameCode,
                questionPackId = questionPackId,
                questionId = questionId,
                isCorrect = answerOption.id in correctOptionIds,
                orderIndex = index,
            )
        }

        is QuestionAnswerModel.Text -> emptyList()
    }
}

internal fun QuestionAnswerModel.toTextAnswerEntities(
    gameCode: String,
    questionPackId: String,
    questionId: String,
): List<TextAnswerEntity> {
    return when (this) {
        is QuestionAnswerModel.Text -> acceptedAnswers.mapIndexed { index, answer ->
            TextAnswerEntity(
                gameCode = gameCode,
                questionPackId = questionPackId,
                questionId = questionId,
                answer = answer,
                orderIndex = index,
            )
        }

        is QuestionAnswerModel.SingleChoice,
        is QuestionAnswerModel.MultipleChoice,
        -> emptyList()
    }
}

private fun QuestionEntity.toAnswerModel(
    answerOptions: List<AnswerOptionEntity>,
    textAnswers: List<TextAnswerEntity>,
): QuestionAnswerModel {
    return when (AnswerType.valueOf(answerType)) {
        AnswerType.SINGLE_CHOICE -> QuestionAnswerModel.SingleChoice(
            options = answerOptions.map { it.toModel() },
            correctOptionId = answerOptions.first { it.isCorrect }.id,
        )

        AnswerType.MULTIPLE_CHOICE -> QuestionAnswerModel.MultipleChoice(
            options = answerOptions.map { it.toModel() },
            correctOptionIds = answerOptions.filter { it.isCorrect }
                .map { it.id }
                .toSet(),
        )

        AnswerType.TEXT -> QuestionAnswerModel.Text(
            acceptedAnswers = textAnswers.map { it.answer },
        )
    }
}

private fun QuestionAnswerModel.toAnswerType(): AnswerType {
    return when (this) {
        is QuestionAnswerModel.SingleChoice -> AnswerType.SINGLE_CHOICE
        is QuestionAnswerModel.MultipleChoice -> AnswerType.MULTIPLE_CHOICE
        is QuestionAnswerModel.Text -> AnswerType.TEXT
    }
}

private enum class AnswerType {
    SINGLE_CHOICE,
    MULTIPLE_CHOICE,
    TEXT,
}