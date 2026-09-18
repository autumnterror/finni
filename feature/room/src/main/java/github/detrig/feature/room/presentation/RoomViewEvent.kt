package github.detrig.feature.room.presentation

import github.detrig.core.mvvm.CoreViewEvent
import github.detrig.feature.room.domain.model.HousePosition
import github.detrig.feature.planning.domain.PlanCategory

internal sealed interface RoomViewEvent : CoreViewEvent {
    data class SavePosition(val position: HousePosition) : RoomViewEvent
    data class ZonePreviewed(val zoneId: String) : RoomViewEvent
    data object MarketClicked : RoomViewEvent
    data object BedClicked : RoomViewEvent
    data object CalendarClicked : RoomViewEvent
    data object PiggyBankClicked : RoomViewEvent
    data object SavePlanClicked : RoomViewEvent
    data object ClosePlanSummary : RoomViewEvent
    data class PlanPercentChanged(val category: PlanCategory, val percent: Int) : RoomViewEvent
    data object Load : RoomViewEvent
    data object RetryClicked : RoomViewEvent
    data class ZoneClicked(val zoneId: String) : RoomViewEvent
    data class BuyConfirmed(val zoneId: String) : RoomViewEvent
    data class SaveZoneAsGoal(val zoneId: String, val title: String) : RoomViewEvent
}
