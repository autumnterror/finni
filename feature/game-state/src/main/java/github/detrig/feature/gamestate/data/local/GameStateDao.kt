package github.detrig.feature.gamestate.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import github.detrig.feature.gamestate.domain.model.HungerAlertState

@Dao
interface GameStateDao {

    @Query("SELECT * FROM game_sessions WHERE id = 'current'")
    suspend fun getCurrentState(): GameStateEntity?

    @Query("SELECT * FROM game_sessions WHERE id = 'current'")
    fun observeCurrentState(): Flow<GameStateEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInitialState(state: GameStateEntity)

    @Query("SELECT * FROM experience_grants WHERE grantId = :grantId")
    suspend fun getExperienceGrant(grantId: String): ExperienceGrantEntity?

    @Query("SELECT COALESCE(SUM(amount), 0) FROM experience_grants WHERE profileId = :profileId AND source = :source")
    suspend fun totalExperienceForSource(profileId: String, source: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExperienceGrant(grant: ExperienceGrantEntity): Long

    @Query("UPDATE game_sessions SET totalXp = :totalXp, playerLevel = :playerLevel WHERE id = 'current'")
    suspend fun updateProgression(totalXp: Int, playerLevel: Int): Int

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

    @Query("UPDATE game_sessions SET hunger = MAX(0, hunger - :hungerCost), happiness = MAX(0, happiness - :happinessCost), hungerAlertEpisode = CASE WHEN hunger > 0 AND hunger <= :hungerCost THEN hungerAlertEpisode + 1 ELSE hungerAlertEpisode END WHERE id = 'current'")
    suspend fun decreaseNeedsForDay(hungerCost: Int, happinessCost: Int): Int

    @Query("SELECT hunger, hungerAlertEpisode, hungerAlertDeliveredEpisode FROM game_sessions WHERE id = 'current'")
    suspend fun hungerAlertState(): HungerAlertState?

    @Query("UPDATE game_sessions SET hungerAlertDeliveredEpisode = :episode WHERE id = 'current' AND hunger = 0 AND hungerAlertEpisode = :episode AND hungerAlertDeliveredEpisode < :episode")
    suspend fun markHungerAlertDelivered(episode: Long): Int

    @Query("UPDATE game_sessions SET hungerAlertEpisode = 1 WHERE id = 'current' AND hunger = 0 AND hungerAlertEpisode = 0")
    suspend fun seedZeroHungerAlertEpisode(): Int
}
