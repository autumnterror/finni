package github.detrig.minigames.common.data.repo

import github.detrig.minigames.common.data.network.RestApi
import github.detrig.minigames.common.data.source.MiniGameQuestionsLocalDataSource
import github.detrig.minigames.common.domain.exception.MiniGameQuestionsApiContractException
import github.detrig.minigames.common.domain.model.QuestionModel
import github.detrig.minigames.common.domain.model.QuestionPackModel
import github.detrig.minigames.common.domain.repository.MiniGameQuestionsRepository

internal class MiniGameQuestionsRepositoryImpl(
    private val remoteApi: RestApi,
    private val localDataSource: MiniGameQuestionsLocalDataSource,
) : MiniGameQuestionsRepository {

    override suspend fun getQuestionPacks(gameCode: String): List<QuestionPackModel> {
        return getRemoteDataWithCacheFallback(
            remoteDataProvider = {
                listOf(remoteApi.getQuestionPacks())
            },
            localDataProvider = {
                localDataSource.getQuestionPacks(gameCode)
            },
            cacheRemoteData = { questionPacks ->
                localDataSource.saveQuestionPacks(
                    gameCode = gameCode,
                    questionPacks = questionPacks,
                )
            },
        )
    }

    override suspend fun getQuestions(
        gameCode: String,
        questionPackId: String,
    ): List<QuestionModel> {
        return getRemoteDataWithCacheFallback(
            remoteDataProvider = {
                loadRemoteQuestions(questionPackId)
            },
            localDataProvider = {
                localDataSource.getQuestions(
                    gameCode = gameCode,
                    questionPackId = questionPackId,
                )
            },
            cacheRemoteData = { questions ->
                localDataSource.replaceQuestions(
                    gameCode = gameCode,
                    questionPackId = questionPackId,
                    questions = questions,
                )
            },
        )
    }

    private suspend fun loadRemoteQuestions(questionPackId: String): List<QuestionModel> {
        remoteApi.getQuestionsFromPack(questionPackId.toLong())
        throw MiniGameQuestionsApiContractException()
    }

    private suspend fun <T> getRemoteDataWithCacheFallback(
        remoteDataProvider: suspend () -> List<T>,
        localDataProvider: suspend () -> List<T>,
        cacheRemoteData: suspend (List<T>) -> Unit,
    ): List<T> {
        val remoteData = try {
            remoteDataProvider()
        } catch (error: Throwable) {
            if (error is MiniGameQuestionsApiContractException) throw error

            return localDataProvider().ifEmpty {
                throw error
            }
        }

        cacheRemoteData(remoteData)
        return remoteData
    }
}