package github.detrig.minigames.common.data.database.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "mini_game_text_answers",
    primaryKeys = ["gameCode", "questionPackId", "questionId", "answer"],
    indices = [
        Index(value = ["gameCode", "questionPackId", "questionId"]),
    ],
)
data class TextAnswerEntity(
    val gameCode: String,
    val questionPackId: String,
    val questionId: String,
    val answer: String,
    val orderIndex: Int,
)
