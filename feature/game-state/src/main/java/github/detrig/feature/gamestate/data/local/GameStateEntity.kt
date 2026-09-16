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
) {
    companion object {
        const val CURRENT_STATE_ID = "current"
    }
}
