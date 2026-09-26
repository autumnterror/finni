package github.detrig.feature.room.presentation

import github.detrig.core.mvvm.CoreViewEvent
import github.detrig.feature.room.domain.model.HousePosition
import github.detrig.feature.planning.domain.PlanCategory

internal sealed interface RoomViewEvent : CoreViewEvent {
    data class SavePosition(val position: HousePosition) : RoomViewEvent
    data class ZonePreviewed(val zoneId: String) : RoomViewEvent
    data object MarketClicked : RoomViewEvent
    data object BedClicked : RoomViewEvent
    data object SleepConfirmed : RoomViewEvent
    data object SleepPostponed : RoomViewEvent
    data object CalendarClicked : RoomViewEvent
    data object PiggyBankClicked : RoomViewEvent
    data object TestsClicked : RoomViewEvent
    data object WardrobeClicked : RoomViewEvent
    data object FoodClicked : RoomViewEvent
    data object DishesClicked : RoomViewEvent
    data object OpenSavingsFromRecoveryPrompt : RoomViewEvent
    data object DismissSavingsRecoveryPrompt : RoomViewEvent
    data class ParentHelpOfferClicked(val offerId: String) : RoomViewEvent
    data object ClaimParentHelpDialog : RoomViewEvent
    data object CloseParentHelpDialog : RoomViewEvent
    data object CloseParentHelpPhonePrompt : RoomViewEvent
    data object FeedingClicked : RoomViewEvent
    data object CloseAllowanceNotice : RoomViewEvent
    data object CloseEarlyWeekParentHelpNotice : RoomViewEvent
    data object CloseDayTransitionNotice : RoomViewEvent
    data object CloseImpulseWish : RoomViewEvent
    data object FirstRunOnboardingContinue : RoomViewEvent
    data object FirstRunOpenPhone : RoomViewEvent
    data object FirstRunOpenFridge : RoomViewEvent
    data object FirstRunOpenTable : RoomViewEvent
    data object FirstRunShowWeekSummary : RoomViewEvent
    data object FirstRunStartNewWeekPlan : RoomViewEvent
    data object FirstRunMoneyNoticeClosed : RoomViewEvent
    data class FirstRunDepositSelected(val depositNow: Boolean) : RoomViewEvent
    data object Resumed : RoomViewEvent
    data object Paused : RoomViewEvent
    data object SavePlanClicked : RoomViewEvent
    data object PlanTutorialNext : RoomViewEvent
    data object PlanDialogueFinished : RoomViewEvent
    data object PlanDialogueEditRequested : RoomViewEvent
    data object MenuClicked : RoomViewEvent
    data object CloseMenu : RoomViewEvent
    data object ToggleMenuAchievements : RoomViewEvent
    data object ShowAllAchievements : RoomViewEvent
    data object CloseAchievements : RoomViewEvent
    data object ParentCabinetClicked : RoomViewEvent
    data class ParentAnswerChanged(val answer: String) : RoomViewEvent
    data object ParentAnswerSubmitted : RoomViewEvent
    data object CloseParentGate : RoomViewEvent
    data object CloseParentCabinet : RoomViewEvent
    data object ClosePlanSummary : RoomViewEvent
    data object CloseWeekResult : RoomViewEvent
    data object WeekSummaryTutorialNext : RoomViewEvent
    data object CloseFirstGamePurchaseFeedback : RoomViewEvent
    data class FirstRunViewReadyGame(val zoneId: String) : RoomViewEvent
    data object CloseFirstWeekNeedHint : RoomViewEvent
    data object CloseFirstWeekGoalHint : RoomViewEvent
    data class PlanPercentChanged(val category: PlanCategory, val percent: Int) : RoomViewEvent
    data class PlanReserveChanged(val percent: Int) : RoomViewEvent
    data object Load : RoomViewEvent
    data object RetryClicked : RoomViewEvent
    data class ZoneClicked(val zoneId: String) : RoomViewEvent
    data class BuyConfirmed(val zoneId: String, val useSavings: Boolean = false) : RoomViewEvent
    data class SaveZoneAsGoal(val zoneId: String, val title: String) : RoomViewEvent
}
