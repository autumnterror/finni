package github.detrig.feature.room.domain.model

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

internal interface FirstRunOnboardingRepository {
    fun load(): FirstRunOnboardingStep
    fun save(step: FirstRunOnboardingStep)
    fun loadSuggestedGoalZoneId(): String?
    fun saveSuggestedGoalZoneId(zoneId: String?)
}
