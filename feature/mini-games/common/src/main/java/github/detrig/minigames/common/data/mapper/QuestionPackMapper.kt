package github.detrig.minigames.common.data.mapper

import github.detrig.minigames.common.data.database.entity.QuestionPackEntity
import github.detrig.minigames.common.domain.model.QuestionDifficultyModel
import github.detrig.minigames.common.domain.model.QuestionPackModel

internal fun QuestionPackEntity.toModel(): QuestionPackModel {
    return QuestionPackModel(
        id = id,
        title = title,
        description = description,
        questionsCount = questionsCount,
        difficulty = QuestionDifficultyModel.valueOf(difficulty),
        bestScore = bestScore,
        accuracyPercent = accuracyPercent,
    )
}

internal fun QuestionPackModel.toEntity(gameCode: String): QuestionPackEntity {
    return QuestionPackEntity(
        id = id,
        gameCode = gameCode,
        title = title,
        description = description,
        questionsCount = questionsCount,
        difficulty = difficulty.name,
        bestScore = bestScore,
        accuracyPercent = accuracyPercent,
    )
}