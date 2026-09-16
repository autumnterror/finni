package github.detrig.minigames.common.data.mapper

import github.detrig.minigames.common.data.database.entity.AnswerOptionEntity
import github.detrig.minigames.common.domain.model.AnswerOptionModel

internal fun AnswerOptionEntity.toModel(): AnswerOptionModel {
    return AnswerOptionModel(
        id = id,
        text = text,
    )
}

internal fun AnswerOptionModel.toEntity(
    gameCode: String,
    questionPackId: String,
    questionId: String,
    isCorrect: Boolean,
    orderIndex: Int,
): AnswerOptionEntity {
    return AnswerOptionEntity(
        gameCode = gameCode,
        questionPackId = questionPackId,
        questionId = questionId,
        id = id,
        text = text,
        isCorrect = isCorrect,
        orderIndex = orderIndex,
    )
}
