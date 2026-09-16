package github.detrig.minigames.common.data.database.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "mini_game_question_packs",
    primaryKeys = ["gameCode", "id"],
    indices = [
        Index(value = ["gameCode"]),
    ],
)
data class QuestionPackEntity(
    val id: String,
    val gameCode: String,
    val title: String,
    val description: String?,
    val questionsCount: Int,
    val difficulty: String,
    val bestScore: Int?,
    val accuracyPercent: Int?,
)