package github.detrig.minigames.common.api

import github.detrig.minigames.common.domain.interactor.CheckQuestionAnswerInteractor
import github.detrig.minigames.common.domain.repository.MiniGameQuestionsRepository

internal class MiniGamesCommonApiImpl(
    private val miniGameQuestionsRepository: MiniGameQuestionsRepository,
    private val checkQuestionAnswerInteractor: CheckQuestionAnswerInteractor,
) : MiniGamesCommonApi {

    override fun miniGameQuestionsRepository(): MiniGameQuestionsRepository {
        return miniGameQuestionsRepository
    }

    override fun checkQuestionAnswerInteractor(): CheckQuestionAnswerInteractor {
        return checkQuestionAnswerInteractor
    }
}