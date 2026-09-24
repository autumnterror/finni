package github.detrig.feature.room.domain.model

internal enum class FirstRunOnboardingChapter(
    val firstStep: FirstRunOnboardingStep,
) {
    INTRODUCTION_AND_FIRST_MONEY(FirstRunOnboardingStep.INTRODUCTION),
    BUDGET_PLANNING(FirstRunOnboardingStep.PLAN),
    MINI_GAMES_DISCOVERY(FirstRunOnboardingStep.GAME_DISCOVERY),
    PIGGY_BANK_DISCOVERY(FirstRunOnboardingStep.PIGGY_BANK),
    FIRST_GOAL_SELECTION(FirstRunOnboardingStep.WAITING_FOR_PIGGY),
}

internal enum class FirstRunOnboardingStep {
    INTRODUCTION,
    WISH,
    FIRST_MONEY,
    MONEY_EXPLANATION,
    PLAN_TRANSITION,
    PLAN,
    PLAN_SAVED,
    GAME_DISCOVERY,
    GAME_DISCOVERY_DETAILS,
    GAME_SELECTION,
    GAME_SELECTED,
    PIGGY_BANK,
    PIGGY_TAP,
    WAITING_FOR_PIGGY,
    WAITING_FOR_GOAL,
    GOAL_CREATED,
    FIRST_DEPOSIT,
    WAITING_FOR_DEPOSIT,
    DEPOSIT_DONE,
    DEPOSIT_SKIPPED,
    GAMES,
    FINISH,
    COMPLETED,
}

internal data class FirstRunOnboardingProgress(
    val completedChapters: Set<FirstRunOnboardingChapter> = emptySet(),
) {
    val currentChapter: FirstRunOnboardingChapter?
        get() = FirstRunOnboardingChapter.entries.firstOrNull { it !in completedChapters }

    val firstStep: FirstRunOnboardingStep
        get() = currentChapter?.firstStep ?: FirstRunOnboardingStep.COMPLETED

    val isCompleted: Boolean
        get() = currentChapter == null

    fun complete(chapter: FirstRunOnboardingChapter): FirstRunOnboardingProgress {
        if (chapter in completedChapters || chapter != currentChapter) return this
        return copy(completedChapters = completedChapters + chapter)
    }
}

internal interface FirstRunOnboardingRepository {
    fun load(): FirstRunOnboardingProgress
    fun markChapterCompleted(chapter: FirstRunOnboardingChapter): FirstRunOnboardingProgress
    fun loadSuggestedGoalZoneId(): String?
    fun saveSuggestedGoalZoneId(zoneId: String?)
}

internal fun FirstRunOnboardingStep.completedChaptersForMigration(): Set<FirstRunOnboardingChapter> = when (this) {
    FirstRunOnboardingStep.INTRODUCTION,
    FirstRunOnboardingStep.WISH,
    FirstRunOnboardingStep.FIRST_MONEY,
    FirstRunOnboardingStep.MONEY_EXPLANATION,
    FirstRunOnboardingStep.PLAN_TRANSITION,
    -> emptySet()

    FirstRunOnboardingStep.PLAN,
    -> setOf(FirstRunOnboardingChapter.INTRODUCTION_AND_FIRST_MONEY)

    FirstRunOnboardingStep.PLAN_SAVED,
    FirstRunOnboardingStep.GAME_DISCOVERY,
    FirstRunOnboardingStep.GAME_DISCOVERY_DETAILS,
    FirstRunOnboardingStep.GAME_SELECTION,
    FirstRunOnboardingStep.GAME_SELECTED,
    -> setOf(
        FirstRunOnboardingChapter.INTRODUCTION_AND_FIRST_MONEY,
        FirstRunOnboardingChapter.BUDGET_PLANNING,
    )

    FirstRunOnboardingStep.PIGGY_BANK,
    FirstRunOnboardingStep.PIGGY_TAP,
    -> setOf(
        FirstRunOnboardingChapter.INTRODUCTION_AND_FIRST_MONEY,
        FirstRunOnboardingChapter.BUDGET_PLANNING,
        FirstRunOnboardingChapter.MINI_GAMES_DISCOVERY,
    )

    FirstRunOnboardingStep.WAITING_FOR_PIGGY,
    FirstRunOnboardingStep.WAITING_FOR_GOAL,
    -> setOf(
        FirstRunOnboardingChapter.INTRODUCTION_AND_FIRST_MONEY,
        FirstRunOnboardingChapter.BUDGET_PLANNING,
        FirstRunOnboardingChapter.MINI_GAMES_DISCOVERY,
        FirstRunOnboardingChapter.PIGGY_BANK_DISCOVERY,
    )

    FirstRunOnboardingStep.GOAL_CREATED,
    FirstRunOnboardingStep.FIRST_DEPOSIT,
    FirstRunOnboardingStep.WAITING_FOR_DEPOSIT,
    FirstRunOnboardingStep.DEPOSIT_DONE,
    FirstRunOnboardingStep.DEPOSIT_SKIPPED,
    FirstRunOnboardingStep.GAMES,
    FirstRunOnboardingStep.FINISH,
    FirstRunOnboardingStep.COMPLETED,
    -> FirstRunOnboardingChapter.entries.toSet()
}
