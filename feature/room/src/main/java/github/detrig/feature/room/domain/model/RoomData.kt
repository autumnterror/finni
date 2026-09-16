package github.detrig.feature.room.domain.model

internal data class RoomData(
    val zones: List<RoomZone>,
    val progress: RoomProgress,
)
