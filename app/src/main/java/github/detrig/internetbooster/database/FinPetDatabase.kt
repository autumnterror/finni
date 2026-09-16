package github.detrig.internetbooster.database

import androidx.room.Database
import androidx.room.RoomDatabase
import github.detrig.feature.gamestate.data.local.GameStateDao
import github.detrig.feature.gamestate.data.local.GameStateEntity
import github.detrig.feature.economy.data.local.EconomyDao
import github.detrig.feature.economy.data.local.EconomyStateEntity
import github.detrig.feature.economy.data.local.FinancialOperationEntity
import github.detrig.feature.economy.data.local.SavingsGoalEntity
import github.detrig.feature.gamestate.data.local.RoomZoneDao
import github.detrig.feature.gamestate.data.local.RoomZoneEntity

@Database(
    entities = [GameStateEntity::class, RoomZoneEntity::class,
        github.detrig.feature.gamestate.data.local.PetPlayEffectEntity::class,
        github.detrig.minigames.fishing.data.FishingProgressEntity::class,
        EconomyStateEntity::class, FinancialOperationEntity::class, SavingsGoalEntity::class],
    version = 10,
    exportSchema = false,
)
abstract class FinPetDatabase : RoomDatabase() {

    abstract fun gameStateDao(): GameStateDao
    abstract fun economyDao(): EconomyDao

    abstract fun roomZoneDao(): RoomZoneDao

    abstract fun petPlayEffectDao(): github.detrig.feature.gamestate.data.local.PetPlayEffectDao

    abstract fun fishingDao(): github.detrig.minigames.fishing.data.FishingDao
}
