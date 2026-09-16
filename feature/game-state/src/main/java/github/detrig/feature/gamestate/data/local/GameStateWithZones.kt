package github.detrig.feature.gamestate.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class GameStateWithZones(
    @Embedded val state: GameStateEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionId")
    val zones: List<RoomZoneEntity>,
)
