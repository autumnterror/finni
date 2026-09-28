package github.detrig.feature.gamestate.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "pet_play_effects")
data class PetPlayEffectEntity(
    @PrimaryKey val operationId: String,
    val profileId: String,
    val sessionId: String,
    val gameId: String,
    val happinessDelta: Int,
    val appliedAtMillis: Long,
)

@Dao
interface PetPlayEffectDao {
    @Query("SELECT * FROM pet_play_effects WHERE operationId = :operationId")
    suspend fun find(operationId: String): PetPlayEffectEntity?

    @Query("SELECT sessionId FROM pet_play_effects WHERE gameId = :gameId ORDER BY CAST(sessionId AS INTEGER) DESC LIMIT 1")
    suspend fun latestSessionForGame(gameId: String): String?

    @Query("SELECT * FROM pet_play_effects WHERE gameId IN ('pet-wish-happiness', 'pet-wish-game-unlocked') OR gameId LIKE 'pet-wish-play:%' OR gameId LIKE 'mini-game-launch:%' ORDER BY appliedAtMillis, operationId")
    suspend fun wishActivities(): List<PetPlayEffectEntity>

    @Insert
    suspend fun insert(effect: PetPlayEffectEntity)

    @Query("SELECT COUNT(*) FROM pet_play_effects WHERE gameId IN ('fishing', 'flight')")
    suspend fun completedMiniGameCount(): Long
}
