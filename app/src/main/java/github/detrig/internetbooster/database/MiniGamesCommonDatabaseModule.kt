package github.detrig.internetbooster.database

import android.content.Context
import github.detrig.core.database.RoomDatabaseFactory
import github.detrig.core.database.create
import github.detrig.minigames.common.data.database.api.MiniGameQuestionsDao
import github.detrig.minigames.common.data.database.api.MiniGamesCommonDatabase

class MiniGamesCommonDatabaseModule(
    private val context: Context,
) {

    private val database: MiniGamesCommonDatabase by lazy {
        RoomDatabaseFactory.create(
            context = context,
            databaseName = DATABASE_NAME,
            fallbackToDestructiveMigration = true,
        )
    }

    val miniGameQuestionsDao: MiniGameQuestionsDao by lazy {
        database.miniGameQuestionsDao()
    }

    private companion object {
        const val DATABASE_NAME = "fin_pet_mini_games_common.db"
    }
}
