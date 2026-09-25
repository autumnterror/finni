package github.detrig.internetbooster.database

import androidx.room.Database
import androidx.room.RoomDatabase
import github.detrig.feature.gamestate.data.local.GameStateDao
import github.detrig.feature.gamestate.data.local.GameStateEntity
import github.detrig.feature.economy.data.local.EconomyDao
import github.detrig.feature.economy.data.local.EconomyStateEntity
import github.detrig.feature.economy.data.local.FinancialOperationEntity
import github.detrig.feature.economy.data.local.ParentHelpStateEntity
import github.detrig.feature.economy.data.local.SavingsGoalEntity
import github.detrig.feature.gamestate.data.local.RoomZoneDao
import github.detrig.feature.gamestate.data.local.RoomZoneEntity
import github.detrig.feature.week.data.local.WeekDao
import github.detrig.feature.week.data.local.WeekStateEntity
import github.detrig.feature.planning.data.local.PlanActualOperationEntity
import github.detrig.feature.planning.data.local.PlanningDao
import github.detrig.feature.planning.data.local.WeeklyPlanEntity
import github.detrig.feature.learning.data.local.AchievementUnlockEntity
import github.detrig.feature.learning.data.local.AchievementXpOutboxEntity
import github.detrig.feature.learning.data.local.LearningActionEntity
import github.detrig.feature.learning.data.local.LearningDao
import github.detrig.feature.learning.data.local.LearningExplanationEntity
import github.detrig.feature.learning.data.local.LearningMetricOccurrenceEntity
import github.detrig.feature.learning.data.local.LearningMetricProgressEntity

@Database(
    entities = [GameStateEntity::class, RoomZoneEntity::class,
        github.detrig.feature.gamestate.data.local.PetPlayEffectEntity::class,
        github.detrig.minigames.fishing.data.FishingProgressEntity::class,
        EconomyStateEntity::class, FinancialOperationEntity::class, SavingsGoalEntity::class, ParentHelpStateEntity::class,
        WeekStateEntity::class, WeeklyPlanEntity::class, PlanActualOperationEntity::class,
        LearningActionEntity::class, LearningMetricOccurrenceEntity::class,
        LearningMetricProgressEntity::class, AchievementUnlockEntity::class,
        LearningExplanationEntity::class, AchievementXpOutboxEntity::class],
    version = 15,
    exportSchema = false,
)
abstract class FinPetDatabase : RoomDatabase() {

    abstract fun gameStateDao(): GameStateDao
    abstract fun economyDao(): EconomyDao
    abstract fun weekDao(): WeekDao
    abstract fun planningDao(): PlanningDao
    abstract fun learningDao(): LearningDao

    abstract fun roomZoneDao(): RoomZoneDao

    abstract fun petPlayEffectDao(): github.detrig.feature.gamestate.data.local.PetPlayEffectDao

    abstract fun fishingDao(): github.detrig.minigames.fishing.data.FishingDao
}
