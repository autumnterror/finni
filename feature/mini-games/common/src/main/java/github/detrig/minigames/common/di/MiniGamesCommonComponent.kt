package github.detrig.minigames.common.di

import github.detrig.minigames.common.api.MiniGamesCommonApi
import github.detrig.minigames.common.domain.interactor.CheckQuestionAnswerInteractor
import github.detrig.minigames.common.domain.repository.MiniGameQuestionsRepository

internal interface MiniGamesCommonComponent {

    val api: MiniGamesCommonApi
    val miniGameQuestionsRepository: MiniGameQuestionsRepository
    val checkQuestionAnswerInteractor: CheckQuestionAnswerInteractor
}