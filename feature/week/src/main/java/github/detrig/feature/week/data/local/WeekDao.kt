package github.detrig.feature.week.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WeekDao {
    @Query("SELECT * FROM week_state WHERE id = 'current'")
    suspend fun getState(): WeekStateEntity?

    @Query("SELECT * FROM week_state WHERE id = 'current'")
    fun observeState(): Flow<WeekStateEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInitial(state: WeekStateEntity): Long

    @Query("UPDATE week_state SET absoluteDay = :nextDay WHERE id = 'current' AND absoluteDay = :expectedDay")
    suspend fun advance(expectedDay: Long, nextDay: Long): Int
}
