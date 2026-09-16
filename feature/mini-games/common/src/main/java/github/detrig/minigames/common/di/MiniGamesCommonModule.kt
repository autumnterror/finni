package github.detrig.minigames.common.di

import github.detrig.minigames.common.MiniGamesCommonDependencies
import github.detrig.minigames.common.api.MiniGamesCommonApi
import github.detrig.minigames.common.api.MiniGamesCommonApiImpl
import github.detrig.minigames.common.data.repo.MiniGameQuestionsRepositoryImpl
import github.detrig.minigames.common.data.source.MiniGameQuestionsLocalDataSource
import github.detrig.minigames.common.domain.interactor.CheckQuestionAnswerInteractor
import github.detrig.minigames.common.domain.repository.MiniGameQuestionsRepository

internal class MiniGamesCommonModule(
    private val dependencies: MiniGamesCommonDependencies,
) : MiniGamesCommonComponent {

    private val localDataSource: MiniGameQuestionsLocalDataSource by lazy {
        MiniGameQuestionsLocalDataSource(
            dao = dependencies.miniGameQuestionsDao(),
        )
    }

    override val miniGameQuestionsRepository: MiniGameQuestionsRepository by lazy {
        MiniGameQuestionsRepositoryImpl(
            remoteApi = dependencies.restApi(),
            localDataSource = localDataSource,
        )
    }

    override val checkQuestionAnswerInteractor: CheckQuestionAnswerInteractor by lazy {
        CheckQuestionAnswerInteractor()
    }

    override val api: MiniGamesCommonApi by lazy {
        MiniGamesCommonApiImpl(
            miniGameQuestionsRepository = miniGameQuestionsRepository,
            checkQuestionAnswerInteractor = checkQuestionAnswerInteractor,
        )
    }
}