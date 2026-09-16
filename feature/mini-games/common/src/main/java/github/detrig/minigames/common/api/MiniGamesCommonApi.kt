package github.detrig.minigames.common.api

import github.detrig.minigames.common.domain.interactor.CheckQuestionAnswerInteractor
import github.detrig.minigames.common.domain.repository.MiniGameQuestionsRepository

interface MiniGamesCommonApi {

    fun miniGameQuestionsRepository(): MiniGameQuestionsRepository

    fun checkQuestionAnswerInteractor(): CheckQuestionAnswerInteractor
}