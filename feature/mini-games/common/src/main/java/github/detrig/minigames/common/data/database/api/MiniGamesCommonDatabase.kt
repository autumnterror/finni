package github.detrig.minigames.common.data.database.api

import androidx.room.Database
import androidx.room.RoomDatabase
import github.detrig.minigames.common.data.database.entity.AnswerOptionEntity
import github.detrig.minigames.common.data.database.entity.QuestionEntity
import github.detrig.minigames.common.data.database.entity.QuestionPackEntity
import github.detrig.minigames.common.data.database.entity.TextAnswerEntity

@Database(
    entities = [
        QuestionPackEntity::class,
        QuestionEntity::class,
        AnswerOptionEntity::class,
        TextAnswerEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class MiniGamesCommonDatabase : RoomDatabase() {

    abstract fun miniGameQuestionsDao(): MiniGameQuestionsDao
}
