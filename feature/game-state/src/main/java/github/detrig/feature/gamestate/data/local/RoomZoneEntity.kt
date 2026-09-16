package github.detrig.feature.gamestate.data.local

import androidx.room.Entity
import androidx.room.ForeignKey

/** Запись означает, что зона принадлежит текущей игре. */
@Entity(
    tableName = "room_zones",
    primaryKeys = ["sessionId", "zoneId"],
    foreignKeys = [ForeignKey(
        entity = GameStateEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class RoomZoneEntity(
    val sessionId: String,
    val zoneId: String,
    val boughtAtMillis: Long,
)
