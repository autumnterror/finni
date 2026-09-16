package github.detrig.feature.room.presentation

import github.detrig.core.mvvm.CoreViewEvent
import github.detrig.feature.room.domain.model.HousePosition

internal sealed interface RoomViewEvent : CoreViewEvent {
    data class SavePosition(val position: HousePosition) : RoomViewEvent
    data class ZonePreviewed(val zoneId: String) : RoomViewEvent
    data object MarketClicked : RoomViewEvent
    data object Load : RoomViewEvent
    data object RetryClicked : RoomViewEvent
    data class ZoneClicked(val zoneId: String) : RoomViewEvent
    data class BuyConfirmed(val zoneId: String) : RoomViewEvent
}
