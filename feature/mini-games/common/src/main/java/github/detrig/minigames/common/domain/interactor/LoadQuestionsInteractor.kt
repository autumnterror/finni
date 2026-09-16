package github.detrig.minigames.common.domain.interactor

import github.detrig.minigames.common.domain.model.QuestionModel
import github.detrig.minigames.common.domain.repository.MiniGameQuestionsRepository

class LoadQuestionsInteractor(
    private val repository: MiniGameQuestionsRepository,
) {

    suspend operator fun invoke(
        gameCode: String,
        questionPackId: String,
    ): List<QuestionModel> {
        return repository.getQuestions(
            gameCode = gameCode,
            questionPackId = questionPackId,
        )
    }
}
