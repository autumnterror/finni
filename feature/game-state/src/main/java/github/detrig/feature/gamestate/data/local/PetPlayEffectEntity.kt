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
    @Insert
    suspend fun insert(effect: PetPlayEffectEntity)
}
