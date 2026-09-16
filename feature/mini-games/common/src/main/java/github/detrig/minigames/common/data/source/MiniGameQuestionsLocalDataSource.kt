package github.detrig.minigames.common.data.source

import github.detrig.minigames.common.data.database.api.MiniGameQuestionsDao
import github.detrig.minigames.common.data.mapper.toAnswerOptionEntities
import github.detrig.minigames.common.data.mapper.toEntity
import github.detrig.minigames.common.data.mapper.toModel
import github.detrig.minigames.common.data.mapper.toTextAnswerEntities
import github.detrig.minigames.common.domain.model.QuestionModel
import github.detrig.minigames.common.domain.model.QuestionPackModel

internal class MiniGameQuestionsLocalDataSource(
    private val dao: MiniGameQuestionsDao,
) {

    suspend fun getQuestionPacks(gameCode: String): List<QuestionPackModel> {
        return dao.getQuestionPacks(gameCode).map { it.toModel() }
    }

    suspend fun saveQuestionPacks(
        gameCode: String,
        questionPacks: List<QuestionPackModel>,
    ) {
        dao.saveQuestionPacks(
            questionPacks = questionPacks.map { questionPack ->
                questionPack.toEntity(gameCode)
            },
        )
    }

    suspend fun getQuestions(
        gameCode: String,
        questionPackId: String,
    ): List<QuestionModel> {
        val questions = dao.getQuestionsFromPack(
            gameCode = gameCode,
            questionPackId = questionPackId,
        )
        if (questions.isEmpty()) return emptyList()

        val questionIds = questions.map { it.id }
        val answerOptionsByQuestionId = dao.getAnswerOptions(
            gameCode = gameCode,
            questionPackId = questionPackId,
            questionIds = questionIds,
        ).groupBy { it.questionId }
        val textAnswersByQuestionId = dao.getTextAnswers(
            gameCode = gameCode,
            questionPackId = questionPackId,
            questionIds = questionIds,
        ).groupBy { it.questionId }

        return questions.map { question ->
            question.toModel(
                answerOptions = answerOptionsByQuestionId.getValueOrEmpty(question.id),
                textAnswers = textAnswersByQuestionId.getValueOrEmpty(question.id),
            )
        }
    }

    suspend fun replaceQuestions(
        gameCode: String,
        questionPackId: String,
        questions: List<QuestionModel>,
    ) {
        dao.replaceQuestions(
            gameCode = gameCode,
            questionPackId = questionPackId,
            questions = questions.mapIndexed { index, question ->
                question.toEntity(
                    gameCode = gameCode,
                    questionPackId = questionPackId,
                    orderIndex = index,
                )
            },
            answerOptions = questions.flatMap { question ->
                question.answer.toAnswerOptionEntities(
                    gameCode = gameCode,
                    questionPackId = questionPackId,
                    questionId = question.id,
                )
            },
            textAnswers = questions.flatMap { question ->
                question.answer.toTextAnswerEntities(
                    gameCode = gameCode,
                    questionPackId = questionPackId,
                    questionId = question.id,
                )
            },
        )
    }

    private fun <T> Map<String, List<T>>.getValueOrEmpty(key: String): List<T> {
        return this[key].orEmpty()
    }
}