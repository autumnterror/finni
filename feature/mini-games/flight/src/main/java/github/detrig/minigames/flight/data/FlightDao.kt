package github.detrig.minigames.flight.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "flight_progress")
data class FlightProgressEntity(@PrimaryKey val profileId: String, val payload: String)

@Dao
interface FlightDao {
    @Query("SELECT * FROM flight_progress WHERE profileId = :profileId")
    suspend fun find(profileId: String): FlightProgressEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: FlightProgressEntity)
}
