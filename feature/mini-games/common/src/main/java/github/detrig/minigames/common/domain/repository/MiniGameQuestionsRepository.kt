package github.detrig.minigames.common.domain.repository

import github.detrig.minigames.common.domain.model.QuestionModel
import github.detrig.minigames.common.domain.model.QuestionPackModel

interface MiniGameQuestionsRepository {

    suspend fun getQuestionPacks(
        gameCode: String,
    ): List<QuestionPackModel>

    suspend fun getQuestions(
        gameCode: String,
        questionPackId: String,
    ): List<QuestionModel>
}
