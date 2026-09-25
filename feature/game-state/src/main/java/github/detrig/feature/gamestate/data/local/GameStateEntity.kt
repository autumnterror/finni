package github.detrig.feature.gamestate.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_sessions")
data class GameStateEntity(
    @PrimaryKey val id: String,
    val hunger: Int,
    val thirst: Int,
    val happiness: Int,
    val health: Int,
    val playerLevel: Int,
    val hungerCheckpointMillis: Long = 0,
    val happinessCheckpointMillis: Long = 0,
    val hungerAlertEpisode: Long = 0,
    val hungerAlertDeliveredEpisode: Long = 0,
) {
    companion object {
        const val CURRENT_STATE_ID = "current"
    }
}
