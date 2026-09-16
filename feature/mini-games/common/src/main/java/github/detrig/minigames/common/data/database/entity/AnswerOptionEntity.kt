package github.detrig.minigames.common.data.database.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "mini_game_answer_options",
    primaryKeys = ["gameCode", "questionPackId", "questionId", "id"],
    indices = [
        Index(value = ["gameCode", "questionPackId", "questionId"]),
    ],
)
data class AnswerOptionEntity(
    val gameCode: String,
    val questionPackId: String,
    val questionId: String,
    val id: String,
    val text: String,
    val isCorrect: Boolean,
    val orderIndex: Int,
)
