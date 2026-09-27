package github.detrig.feature.learningtests.data

import github.detrig.core.database.RoomTransactionRunner
import github.detrig.feature.learningtests.data.local.LearningTestAnswerEntity
import github.detrig.feature.learningtests.data.local.LearningTestAttemptEntity
import github.detrig.feature.learningtests.data.local.LearningTestCompletionRewardOutboxEntity
import github.detrig.feature.learningtests.data.local.LearningTestDailyOfferEntity
import github.detrig.feature.learningtests.data.local.LearningTestMasteryEntity
import github.detrig.feature.learningtests.data.local.LearningTestQuestionXpOutboxEntity
import github.detrig.feature.learningtests.data.local.LearningTestsDao

internal class LearningTestsRepository(
    private val dao: LearningTestsDao,
    private val transactionRunner: RoomTransactionRunner,
) {
    suspend fun ensureDailyOffers(profileId: String, gameDay: Long, catalogIds: List<String>) {
        if (dao.getDailyOffers(profileId, gameDay).isNotEmpty()) return
        val mastered = dao.getMasteredTestIds(profileId).toSet()
        val startIndex = ((gameDay - 1) * DAILY_TEST_COUNT % catalogIds.size).toInt()
        val rotated = catalogIds.drop(startIndex) + catalogIds.take(startIndex)
        val selected = rotated.filterNot(mastered::contains).take(DAILY_TEST_COUNT)
        dao.insertDailyOffers(
            selected.mapIndexed { position, testId ->
                LearningTestDailyOfferEntity(profileId, gameDay, position, testId)
            },
        )
    }

    suspend fun dailyOffers(profileId: String, gameDay: Long) = dao.getDailyOffers(profileId, gameDay)

    suspend fun masteredTestIds(profileId: String) = dao.getMasteredTestIds(profileId).toSet()

    suspend fun attemptsForDay(profileId: String, gameDay: Long) =
        dao.getAttemptsForDay(profileId, gameDay).associateBy { it.testId }

    suspend fun beginAttempt(
        profileId: String,
        testId: String,
        gameDay: Long,
    ): LearningTestAttemptEntity? = transactionRunner.runInTransaction {
        if (testId !in dao.getDailyOffers(profileId, gameDay).map { it.testId }) return@runInTransaction null
        dao.getAttempt(profileId, testId, gameDay)?.let { return@runInTransaction it }
        if (testId in dao.getMasteredTestIds(profileId)) return@runInTransaction null
        dao.insertAttempt(
            LearningTestAttemptEntity(
                profileId = profileId,
                testId = testId,
                gameDay = gameDay,
                questionIndex = 0,
                mistakeCount = 0,
                isComplete = false,
                isPerfect = false,
            ),
        )
        dao.getAttempt(profileId, testId, gameDay)
    }

    suspend fun sessionRows(profileId: String, testId: String, gameDay: Long) =
        dao.getAttempt(profileId, testId, gameDay)?.let { attempt ->
            attempt to dao.getAnswer(profileId, testId, gameDay, attempt.questionIndex)
        }

    suspend fun answersForAttempt(profileId: String, testId: String, gameDay: Long) =
        dao.getAnswersForAttempt(profileId, testId, gameDay)

    suspend fun recordAnswer(
        profileId: String,
        testId: String,
        gameDay: Long,
        questionIndex: Int,
        selectedOptionIndex: Int,
        isCorrect: Boolean,
        questionCount: Int,
        xpAmount: Int,
        completionRewardRub: Long,
    ): Pair<LearningTestAttemptEntity, LearningTestAnswerEntity>? = transactionRunner.runInTransaction {
        val attempt = dao.getAttempt(profileId, testId, gameDay) ?: return@runInTransaction null
        val existingAnswer = dao.getAnswer(profileId, testId, gameDay, questionIndex)
        if (existingAnswer != null) return@runInTransaction attempt to existingAnswer
        if (attempt.isComplete || attempt.questionIndex != questionIndex) return@runInTransaction null

        val answer = LearningTestAnswerEntity(
            profileId = profileId,
            testId = testId,
            gameDay = gameDay,
            questionIndex = questionIndex,
            selectedOptionIndex = selectedOptionIndex,
            isCorrect = isCorrect,
        )
        if (dao.insertAnswer(answer) == -1L) {
            return@runInTransaction attempt to requireNotNull(
                dao.getAnswer(profileId, testId, gameDay, questionIndex),
            )
        }

        val mistakeCount = attempt.mistakeCount + if (isCorrect) 0 else 1
        val isFinalQuestion = questionIndex == questionCount - 1
        val isPerfect = isFinalQuestion && mistakeCount == 0
        val updatedAttempt = attempt.copy(
            mistakeCount = mistakeCount,
            isComplete = isFinalQuestion,
            isPerfect = isPerfect,
        )
        dao.updateAttempt(updatedAttempt)

        if (isCorrect) {
            dao.insertQuestionXpReward(
                LearningTestQuestionXpOutboxEntity(
                    profileId = profileId,
                    testId = testId,
                    questionIndex = questionIndex,
                    amount = xpAmount,
                    delivered = false,
                ),
            )
        }
        if (isPerfect) {
            dao.insertMastery(LearningTestMasteryEntity(profileId, testId, gameDay))
            dao.insertCompletionReward(
                LearningTestCompletionRewardOutboxEntity(
                    profileId = profileId,
                    testId = testId,
                    amountRub = completionRewardRub,
                    delivered = false,
                ),
            )
        }
        updatedAttempt to answer
    }

    suspend fun advanceQuestion(
        profileId: String,
        testId: String,
        gameDay: Long,
        expectedQuestionIndex: Int,
    ): LearningTestAttemptEntity? = transactionRunner.runInTransaction {
        val attempt = dao.getAttempt(profileId, testId, gameDay) ?: return@runInTransaction null
        if (attempt.isComplete || attempt.questionIndex != expectedQuestionIndex) return@runInTransaction attempt
        if (dao.getAnswer(profileId, testId, gameDay, expectedQuestionIndex) == null) return@runInTransaction attempt
        val advanced = attempt.copy(questionIndex = expectedQuestionIndex + 1)
        dao.updateAttempt(advanced)
        advanced
    }

    suspend fun pendingQuestionXp(profileId: String) = dao.pendingQuestionXp(profileId)
    suspend fun markQuestionXpDelivered(profileId: String, testId: String, questionIndex: Int) =
        dao.markQuestionXpDelivered(profileId, testId, questionIndex)

    suspend fun pendingCompletionRewards(profileId: String) = dao.pendingCompletionRewards(profileId)
    suspend fun markCompletionRewardDelivered(profileId: String, testId: String) =
        dao.markCompletionRewardDelivered(profileId, testId)

    private companion object {
        const val DAILY_TEST_COUNT = 2
    }
}
