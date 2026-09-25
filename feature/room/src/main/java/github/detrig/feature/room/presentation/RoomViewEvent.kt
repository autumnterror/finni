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
    data object FeedingClicked : RoomViewEvent
    data class ParentHelpOfferClicked(val offerId: String) : RoomViewEvent
    data object CloseParentHelpDialog : RoomViewEvent
    data object CloseAllowanceNotice : RoomViewEvent
    data object CloseEarlyWeekParentHelpNotice : RoomViewEvent
    data object CloseImpulseWish : RoomViewEvent
    data object FirstRunOnboardingContinue : RoomViewEvent
    data object FirstRunMoneyNoticeClosed : RoomViewEvent
    data class FirstRunDepositSelected(val depositNow: Boolean) : RoomViewEvent
    data object Resumed : RoomViewEvent
    data object Paused : RoomViewEvent
    data object SavePlanClicked : RoomViewEvent
    data object PlanTutorialNext : RoomViewEvent
    data object PlanDialogueFinished : RoomViewEvent
    data object PlanDialogueEditRequested : RoomViewEvent
    data object AchievementsClicked : RoomViewEvent
    data object CloseAchievements : RoomViewEvent
    data object ClosePlanSummary : RoomViewEvent
    data object CloseWeekResult : RoomViewEvent
    data class PlanPercentChanged(val category: PlanCategory, val percent: Int) : RoomViewEvent
    data class PlanReserveChanged(val percent: Int) : RoomViewEvent
    data object Load : RoomViewEvent
    data object RetryClicked : RoomViewEvent
    data class ZoneClicked(val zoneId: String) : RoomViewEvent
    data class BuyConfirmed(val zoneId: String) : RoomViewEvent
    data class SaveZoneAsGoal(val zoneId: String, val title: String) : RoomViewEvent
}
