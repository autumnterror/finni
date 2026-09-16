package github.detrig.minigames.common.domain.interactor

import github.detrig.minigames.common.domain.model.QuestionPackModel
import github.detrig.minigames.common.domain.repository.MiniGameQuestionsRepository

class LoadQuestionPacksInteractor(
    private val repository: MiniGameQuestionsRepository,
) {

    suspend operator fun invoke(gameCode: String): List<QuestionPackModel> {
        return repository.getQuestionPacks(gameCode)
    }
}
