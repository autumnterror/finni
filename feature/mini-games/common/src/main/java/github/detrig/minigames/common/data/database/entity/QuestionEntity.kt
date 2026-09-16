package github.detrig.minigames.common.data.database.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "mini_game_questions",
    primaryKeys = ["gameCode", "questionPackId", "id"],
    indices = [
        Index(value = ["gameCode", "questionPackId"]),
    ],
)
data class QuestionEntity(
    val id: String,
    val gameCode: String,
    val questionPackId: String,
    val text: String,
    val answerType: String,
    val explanation: String?,
    val orderIndex: Int,
)