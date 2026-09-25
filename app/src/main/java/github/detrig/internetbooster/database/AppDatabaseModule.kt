package github.detrig.internetbooster.database

import android.content.Context
import github.detrig.core.database.RoomDatabaseFactory
import github.detrig.core.database.RoomTransactionRunner
import github.detrig.core.database.create
import github.detrig.feature.gamestate.data.local.GameStateDao
import github.detrig.feature.economy.data.local.EconomyDao
import github.detrig.feature.week.data.local.WeekDao
import github.detrig.feature.planning.data.local.PlanningDao
import github.detrig.feature.gamestate.data.local.RoomZoneDao
import github.detrig.feature.learning.data.local.LearningDao

class AppDatabaseModule(
    private val context: Context,
) {

    val database: FinPetDatabase by lazy {
        RoomDatabaseFactory.create(
            context = context,
            databaseName = DATABASE_NAME,
            fallbackToDestructiveMigration = true,
        )
    }

    val gameStateDao: GameStateDao by lazy {
        database.gameStateDao()
    }

    val economyDao: EconomyDao by lazy { database.economyDao() }
    val weekDao: WeekDao by lazy { database.weekDao() }
    val planningDao: PlanningDao by lazy { database.planningDao() }
    val learningDao: LearningDao by lazy { database.learningDao() }

    val transactionRunner: RoomTransactionRunner by lazy {
        RoomTransactionRunner(database)
    }

    val roomZoneDao: RoomZoneDao by lazy { database.roomZoneDao() }

    val petPlayEffectDao by lazy { database.petPlayEffectDao() }

    val fishingDao by lazy { database.fishingDao() }

    private companion object {
        const val DATABASE_NAME = "fin_pet.db"
    }
}
