package github.detrig.minigames.fishing.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/** Снимок агрегата: улов, рекорды и активная сессия записываются одной строкой. */
@Entity(tableName = "fishing_progress")
data class FishingProgressEntity(@PrimaryKey val profileId: String, val payload: String)

@Dao
interface FishingDao {
    @Query("SELECT * FROM fishing_progress WHERE profileId = :profileId")
    suspend fun read(profileId: String): FishingProgressEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun write(value: FishingProgressEntity)
}
