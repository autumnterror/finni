package github.detrig.feature.learningtests.domain

import github.detrig.feature.economy.api.EconomyApi
import github.detrig.feature.economy.domain.FinancialOperationResult
import github.detrig.feature.economy.domain.OperationContext
import github.detrig.feature.gamestate.api.ProgressionApi
import github.detrig.feature.gamestate.domain.progression.GrantXpResult
import github.detrig.feature.learningtests.data.LearningTestsRepository
import github.detrig.feature.learningtests.data.local.LearningTestAnswerEntity
import github.detrig.feature.learningtests.data.local.LearningTestAttemptEntity
import github.detrig.feature.learningtests.navigation.LearningTestsRouter
import github.detrig.feature.week.api.WeekApi

internal class LearningTestsInteractor(
    private val repository: LearningTestsRepository,
    private val economyApi: EconomyApi,
    private val progressionApi: ProgressionApi,
    private val weekApi: WeekApi,
) {
    suspend fun loadDailyTests(profileId: String = CURRENT_PROFILE_ID): DailyLearningTests {
        deliverPendingRewards(profileId)
        val gameDay = weekApi.initialize().absoluteDay
        repository.ensureDailyOffers(profileId, gameDay, LearningTestCatalog.tests.map { it.id })
        val mastered = repository.masteredTestIds(profileId)
        val attempts = repository.attemptsForDay(profileId, gameDay)
        val offers = repository.dailyOffers(profileId, gameDay).mapNotNull { offer ->
            if (offer.testId in mastered) return@mapNotNull null
            val definition = LearningTestCatalog.byId[offer.testId] ?: return@mapNotNull null
            val attempt = attempts[offer.testId]
            LearningTestOffer(
                test = definition,
                status = when {
                    attempt?.isComplete == true -> LearningTestOfferStatus.COMPLETED_WITH_MISTAKES
                    attempt != null -> LearningTestOfferStatus.IN_PROGRESS
                    else -> LearningTestOfferStatus.NEW
                },
                questionIndex = attempt?.questionIndex?.takeIf { attempt.isComplete.not() },
                mistakeCount = attempt?.mistakeCount ?: 0,
            )
        }
        return DailyLearningTests(gameDay, offers)
    }

    suspend fun startTest(testId: String, profileId: String = CURRENT_PROFILE_ID): LearningTestSession? {
        deliverPendingRewards(profileId)
        val definition = LearningTestCatalog.byId[testId] ?: return null
        val gameDay = weekApi.initialize().absoluteDay
        repository.ensureDailyOffers(profileId, gameDay, LearningTestCatalog.tests.map { it.id })
        repository.beginAttempt(profileId, testId, gameDay) ?: return null
        return loadSession(profileId, definition, gameDay)
    }

    suspend fun submitAnswer(
        testId: String,
        questionIndex: Int,
        selectedOptionIndex: Int,
        profileId: String = CURRENT_PROFILE_ID,
    ): LearningTestSession? {
        val definition = LearningTestCatalog.byId[testId] ?: return null
        val question = definition.questions.getOrNull(questionIndex) ?: return null
        if (selectedOptionIndex !in question.options.indices) return null
        val gameDay = weekApi.initialize().absoluteDay
        val rows = repository.sessionRows(profileId, testId, gameDay) ?: return null
        if (rows.first.questionIndex != questionIndex && rows.second == null) return null
        repository.recordAnswer(
            profileId = profileId,
            testId = testId,
            gameDay = gameDay,
            questionIndex = questionIndex,
            selectedOptionIndex = selectedOptionIndex,
            isCorrect = selectedOptionIndex == question.correctOptionIndex,
            questionCount = definition.questions.size,
            xpAmount = definition.difficulty.xpPerCorrectAnswer,
            completionRewardRub = definition.difficulty.perfectRewardRub,
        )
        deliverPendingRewards(profileId)
        return loadSession(profileId, definition, gameDay)
    }

    suspend fun continueTest(
        testId: String,
        expectedQuestionIndex: Int,
        profileId: String = CURRENT_PROFILE_ID,
    ): LearningTestSession? {
        val definition = LearningTestCatalog.byId[testId] ?: return null
        val gameDay = weekApi.initialize().absoluteDay
        repository.advanceQuestion(profileId, testId, gameDay, expectedQuestionIndex) ?: return null
        return loadSession(profileId, definition, gameDay)
    }

    private suspend fun loadSession(
        profileId: String,
        definition: LearningTestDefinition,
        gameDay: Long,
    ): LearningTestSession? {
        val (attemptRow, answerRow) = repository.sessionRows(profileId, definition.id, gameDay) ?: return null
        val attempt = attemptRow.toDomain()
        val answers = repository.answersForAttempt(profileId, definition.id, gameDay)
            .associate { it.questionIndex to it.toDomain() }
        return LearningTestSession(
            test = definition,
            attempt = attempt,
            question = definition.questions.getOrNull(attempt.questionIndex),
            answer = answerRow?.toDomain(),
            answers = answers,
        )
    }

    private suspend fun deliverPendingRewards(profileId: String) {
        repository.pendingQuestionXp(profileId).forEach { reward ->
            val grantId = questionXpGrantId(profileId, reward.testId, reward.questionIndex)
            when (progressionApi.grantXp(grantId, profileId, reward.amount, XP_SOURCE)) {
                is GrantXpResult.Granted, is GrantXpResult.AlreadyGranted ->
                    repository.markQuestionXpDelivered(profileId, reward.testId, reward.questionIndex)
                is GrantXpResult.OperationIdConflict -> error("Conflicting quiz XP grant: $grantId")
            }
        }
        repository.pendingCompletionRewards(profileId).forEach { reward ->
            val operationId = completionOperationId(profileId, reward.testId)
            val result = economyApi.credit(
                operationId = operationId,
                amountRub = reward.amountRub,
                context = OperationContext(
                    reasonId = "financial_literacy_test",
                    metadata = "testId=${reward.testId}",
                ),
            )
            when (result) {
                is FinancialOperationResult.Applied, is FinancialOperationResult.AlreadyApplied ->
                    repository.markCompletionRewardDelivered(profileId, reward.testId)
                is FinancialOperationResult.Rejected -> error("Quiz reward was rejected")
            }
        }
    }

    companion object {
        const val CURRENT_PROFILE_ID = "current"
        private const val XP_SOURCE = "financial_literacy_test"

        fun questionXpGrantId(profileId: String, testId: String, questionIndex: Int) =
            "quiz-xp:$profileId:$testId:$questionIndex"

        fun completionOperationId(profileId: String, testId: String) =
            "quiz-reward:$profileId:$testId"
    }
}

private fun LearningTestAttemptEntity.toDomain() = LearningTestAttempt(
    gameDay = gameDay,
    questionIndex = questionIndex,
    mistakeCount = mistakeCount,
    isComplete = isComplete,
    isPerfect = isPerfect,
)

private fun LearningTestAnswerEntity.toDomain() = LearningTestAnswer(
    selectedOptionIndex = selectedOptionIndex,
    isCorrect = isCorrect,
)
