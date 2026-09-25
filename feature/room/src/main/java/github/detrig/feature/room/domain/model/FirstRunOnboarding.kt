package github.detrig.feature.room.domain.model

import github.detrig.feature.room.api.FirstRunOnboardingStep

internal enum class FirstRunOnboardingChapter(
    val firstStep: FirstRunOnboardingStep,
) {
    INTRODUCTION_AND_FIRST_MONEY(FirstRunOnboardingStep.INTRODUCTION),
    BUDGET_PLANNING(FirstRunOnboardingStep.PLAN),
    MINI_GAMES_DISCOVERY(FirstRunOnboardingStep.GAME_DISCOVERY),
    PIGGY_BANK_DISCOVERY(FirstRunOnboardingStep.PIGGY_BANK),
    FIRST_GOAL_SELECTION(FirstRunOnboardingStep.WAITING_FOR_PIGGY),
    FIRST_NEED(FirstRunOnboardingStep.WAITING_FOR_HUNGER),
    FIRST_BEDTIME(FirstRunOnboardingStep.BEDTIME_LATE),
    SECOND_DAY_MORNING(FirstRunOnboardingStep.SECOND_DAY_MORNING),
    FIRST_WEEK_SUMMARY(FirstRunOnboardingStep.WAITING_FOR_WEEK_END),
    NEXT_WEEK_PLANNING(FirstRunOnboardingStep.NEW_WEEK_INTRO),
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
    fun isFirstGamePurchaseExplained(): Boolean
    fun markFirstGamePurchaseExplained()
    fun isFirstGameReadyIntroduced(): Boolean
    fun markFirstGameReadyIntroduced()
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
    -> setOf(
        FirstRunOnboardingChapter.INTRODUCTION_AND_FIRST_MONEY,
        FirstRunOnboardingChapter.BUDGET_PLANNING,
        FirstRunOnboardingChapter.MINI_GAMES_DISCOVERY,
        FirstRunOnboardingChapter.PIGGY_BANK_DISCOVERY,
        FirstRunOnboardingChapter.FIRST_GOAL_SELECTION,
    )

    FirstRunOnboardingStep.WAITING_FOR_HUNGER,
    FirstRunOnboardingStep.HUNGER_INTRO,
    FirstRunOnboardingStep.HUNGER_FIND_FOOD,
    FirstRunOnboardingStep.PHONE_GUIDANCE,
    FirstRunOnboardingStep.PHONE_STORE_GUIDANCE,
    FirstRunOnboardingStep.SHOP_PRICE_GUIDANCE,
    FirstRunOnboardingStep.SHOP_FOOD_GUIDANCE,
    FirstRunOnboardingStep.PURCHASE_READY,
    FirstRunOnboardingStep.PURCHASE_STORAGE_HINT,
    FirstRunOnboardingStep.FRIDGE_GUIDANCE,
    FirstRunOnboardingStep.FRIDGE_FOUND,
    FirstRunOnboardingStep.FRIDGE_EXPLANATION,
    FirstRunOnboardingStep.WAITING_FOR_FRIDGE_CLOSE,
    FirstRunOnboardingStep.TABLE_PROMPT,
    FirstRunOnboardingStep.TABLE_GUIDANCE,
    FirstRunOnboardingStep.FEEDING,
    FirstRunOnboardingStep.FEEDING_DONE,
    -> setOf(
        FirstRunOnboardingChapter.INTRODUCTION_AND_FIRST_MONEY,
        FirstRunOnboardingChapter.BUDGET_PLANNING,
        FirstRunOnboardingChapter.MINI_GAMES_DISCOVERY,
        FirstRunOnboardingChapter.PIGGY_BANK_DISCOVERY,
        FirstRunOnboardingChapter.FIRST_GOAL_SELECTION,
    )

    FirstRunOnboardingStep.BEDTIME_LATE,
    FirstRunOnboardingStep.BEDTIME_GUIDANCE,
    FirstRunOnboardingStep.WAITING_FOR_BED,
    -> FirstRunOnboardingChapter.entries.takeWhile {
        it != FirstRunOnboardingChapter.FIRST_BEDTIME
    }.toSet()

    FirstRunOnboardingStep.SECOND_DAY_MORNING,
    -> FirstRunOnboardingChapter.entries.takeWhile {
        it != FirstRunOnboardingChapter.SECOND_DAY_MORNING
    }.toSet()

    FirstRunOnboardingStep.WAITING_FOR_WEEK_END,
    FirstRunOnboardingStep.WEEK_END_INTRO,
    FirstRunOnboardingStep.WEEK_SUMMARY_VIEW,
    -> FirstRunOnboardingChapter.entries.takeWhile {
        it != FirstRunOnboardingChapter.FIRST_WEEK_SUMMARY
    }.toSet()

    FirstRunOnboardingStep.NEW_WEEK_INTRO,
    FirstRunOnboardingStep.NEW_WEEK_PLAN_GUIDANCE,
    -> FirstRunOnboardingChapter.entries.takeWhile {
        it != FirstRunOnboardingChapter.NEXT_WEEK_PLANNING
    }.toSet()
}
