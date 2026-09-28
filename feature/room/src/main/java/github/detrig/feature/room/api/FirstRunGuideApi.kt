package github.detrig.feature.room.api

import kotlinx.coroutines.flow.StateFlow

enum class FirstRunOnboardingStep {
    INTRODUCTION,
    WISH,
    FIRST_MONEY,
    MONEY_EXPLANATION,
    PLAN_TRANSITION,
    PLAN,
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
    WAITING_FOR_HUNGER,
    HUNGER_INTRO,
    HUNGER_FIND_FOOD,
    PHONE_GUIDANCE,
    WAITING_FOR_PHONE,
    PHONE_STORE_GUIDANCE,
    WAITING_FOR_STORE,
    SHOP_PRICE_GUIDANCE,
    SHOP_FOOD_GUIDANCE,
    SHOP_FOOD_SELECTED,
    PURCHASE_READY,
    PURCHASE_STORAGE_HINT,
    FRIDGE_GUIDANCE,
    WAITING_FOR_FRIDGE,
    FRIDGE_FOUND,
    FRIDGE_EXPLANATION,
    FRIDGE_PICK_FOOD,
    WAITING_FOR_FRIDGE_CLOSE,
    TABLE_PROMPT,
    TABLE_GUIDANCE,
    FEEDING,
    FEEDING_DONE,
    BEDTIME_LATE,
    BEDTIME_GUIDANCE,
    WAITING_FOR_BED,
    SECOND_DAY_MORNING,
    SECOND_DAY_WISHES,
    WISH_BOARD_GUIDANCE,
    WAITING_FOR_WEEK_END,
    WEEK_END_INTRO,
    WEEK_SUMMARY_VIEW,
    NEW_WEEK_INTRO,
    NEW_WEEK_PLAN_GUIDANCE,
    COMPLETED,
}

interface FirstRunGuideApi {
    val step: StateFlow<FirstRunOnboardingStep>
    val resetVersion: StateFlow<Int>

    fun moveTo(step: FirstRunOnboardingStep)

    fun completeFirstNeed()

    /** Resets the first-run guide after a debug/demo progress reset. */
    fun reset(skipOnboarding: Boolean)
}
