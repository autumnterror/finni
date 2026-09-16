package github.detrig.minigames.common.data.network

import github.detrig.minigames.common.domain.model.QuestionAnswerModel
import github.detrig.minigames.common.domain.model.QuestionPackModel
import retrofit2.http.GET
import retrofit2.http.Path

interface RestApi {
    @GET("/api/mini-games/question-packs")
    suspend fun getQuestionPacks(): QuestionPackModel

    @GET("/api/mini-games/questions/{questionPackId}")
    suspend fun getQuestionsFromPack(
        @Path("questionPackId") questionPackId: Long
    ): QuestionAnswerModel
}
