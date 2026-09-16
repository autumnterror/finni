package github.detrig.feature.gamestate.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RoomZoneDao {
    @Query("SELECT EXISTS(SELECT 1 FROM room_zones WHERE sessionId = :sessionId AND zoneId = :zoneId)")
    suspend fun isOwned(sessionId: String, zoneId: String): Boolean

    @Insert
    suspend fun insert(zone: RoomZoneEntity)
}
