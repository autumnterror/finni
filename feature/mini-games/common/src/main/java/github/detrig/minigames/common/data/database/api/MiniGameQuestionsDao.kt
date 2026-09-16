package github.detrig.minigames.common.data.database.api

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import github.detrig.minigames.common.data.database.entity.AnswerOptionEntity
import github.detrig.minigames.common.data.database.entity.QuestionEntity
import github.detrig.minigames.common.data.database.entity.QuestionPackEntity
import github.detrig.minigames.common.data.database.entity.TextAnswerEntity

@Dao
interface MiniGameQuestionsDao {

    @Query("SELECT * FROM mini_game_question_packs WHERE gameCode = :gameCode ORDER BY title")
    suspend fun getQuestionPacks(gameCode: String): List<QuestionPackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveQuestionPacks(questionPacks: List<QuestionPackEntity>)

    @Query(
        """
        SELECT * FROM mini_game_questions
        WHERE gameCode = :gameCode AND questionPackId = :questionPackId
        ORDER BY orderIndex
        """
    )
    suspend fun getQuestionsFromPack(
        gameCode: String,
        questionPackId: String,
    ): List<QuestionEntity>

    @Query(
        """
        SELECT * FROM mini_game_answer_options
        WHERE gameCode = :gameCode
            AND questionPackId = :questionPackId
            AND questionId IN (:questionIds)
        ORDER BY orderIndex
        """
    )
    suspend fun getAnswerOptions(
        gameCode: String,
        questionPackId: String,
        questionIds: List<String>,
    ): List<AnswerOptionEntity>

    @Query(
        """
        SELECT * FROM mini_game_text_answers
        WHERE gameCode = :gameCode
            AND questionPackId = :questionPackId
            AND questionId IN (:questionIds)
        ORDER BY orderIndex
        """
    )
    suspend fun getTextAnswers(
        gameCode: String,
        questionPackId: String,
        questionIds: List<String>,
    ): List<TextAnswerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveQuestions(questions: List<QuestionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAnswerOptions(options: List<AnswerOptionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTextAnswers(answers: List<TextAnswerEntity>)

    @Query("DELETE FROM mini_game_questions WHERE gameCode = :gameCode AND questionPackId = :questionPackId")
    suspend fun deleteQuestionsFromPack(
        gameCode: String,
        questionPackId: String,
    )

    @Query(
        """
        DELETE FROM mini_game_answer_options
        WHERE gameCode = :gameCode
            AND questionPackId = :questionPackId
            AND questionId IN (:questionIds)
        """
    )
    suspend fun deleteAnswerOptions(
        gameCode: String,
        questionPackId: String,
        questionIds: List<String>,
    )

    @Query(
        """
        DELETE FROM mini_game_text_answers
        WHERE gameCode = :gameCode
            AND questionPackId = :questionPackId
            AND questionId IN (:questionIds)
        """
    )
    suspend fun deleteTextAnswers(
        gameCode: String,
        questionPackId: String,
        questionIds: List<String>,
    )

    /**
     * Полностью заменяет кеш вопросов для выбранного набора.
     * Транзакция нужна, чтобы вопросы и ответы не сохранились частично.
     */
    @Transaction
    suspend fun replaceQuestions(
        gameCode: String,
        questionPackId: String,
        questions: List<QuestionEntity>,
        answerOptions: List<AnswerOptionEntity>,
        textAnswers: List<TextAnswerEntity>,
    ) {
        val oldQuestionIds = getQuestionsFromPack(
            gameCode = gameCode,
            questionPackId = questionPackId,
        ).map { it.id }

        if (oldQuestionIds.isNotEmpty()) {
            deleteAnswerOptions(
                gameCode = gameCode,
                questionPackId = questionPackId,
                questionIds = oldQuestionIds,
            )
            deleteTextAnswers(
                gameCode = gameCode,
                questionPackId = questionPackId,
                questionIds = oldQuestionIds,
            )
        }

        deleteQuestionsFromPack(
            gameCode = gameCode,
            questionPackId = questionPackId,
        )
        saveQuestions(questions)
        saveAnswerOptions(answerOptions)
        saveTextAnswers(textAnswers)
    }
}
