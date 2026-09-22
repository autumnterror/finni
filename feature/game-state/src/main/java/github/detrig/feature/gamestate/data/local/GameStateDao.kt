package github.detrig.feature.gamestate.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface GameStateDao {

    @Query("SELECT * FROM game_sessions WHERE id = 'current'")
    suspend fun getCurrentState(): GameStateEntity?

    @Query("SELECT * FROM game_sessions WHERE id = 'current'")
    fun observeCurrentState(): Flow<GameStateEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInitialState(state: GameStateEntity)

    @Transaction
    @Query("SELECT * FROM game_sessions WHERE id = 'current'")
    suspend fun getCurrentStateWithZones(): GameStateWithZones?

    @Transaction
    @Query("SELECT * FROM game_sessions WHERE id = 'current'")
    fun observeCurrentStateWithZones(): Flow<GameStateWithZones?>

    @Query("UPDATE game_sessions SET happiness = MIN(100, happiness + :delta) WHERE id = 'current'")
    suspend fun increaseHappiness(delta: Int): Int

    @Query("UPDATE game_sessions SET hunger = MIN(100, hunger + :delta) WHERE id = 'current'")
    suspend fun increaseHunger(delta: Int): Int
}
