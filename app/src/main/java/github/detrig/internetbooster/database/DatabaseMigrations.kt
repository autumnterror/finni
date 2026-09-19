package github.detrig.internetbooster.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val hasLegacyWallet = db.hasColumn("game_sessions", "balanceRub")
        val hasLegacyRoomZones = db.hasTable("room_zones")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `economy_state` (
                `id` TEXT NOT NULL, `availableRub` INTEGER NOT NULL, `savingsRub` INTEGER NOT NULL,
                `debtRub` INTEGER NOT NULL, `periodicAmountRub` INTEGER NOT NULL,
                `periodicPeriodMillis` INTEGER NOT NULL, `nextPeriodicAtMillis` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        if (hasLegacyWallet) {
            db.execSQL("""
                INSERT OR IGNORE INTO economy_state
                    (id, availableRub, savingsRub, debtRub, periodicAmountRub, periodicPeriodMillis, nextPeriodicAtMillis)
                SELECT 'current', balanceRub, 0, 0, nextAllowanceAmountRub, 604800000, nextAllowanceAtMillis
                FROM game_sessions WHERE id = 'current'
            """.trimIndent())
        }
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `financial_operations` (
                `id` TEXT NOT NULL, `typeCode` TEXT NOT NULL, `amountRub` INTEGER NOT NULL,
                `timestampMillis` INTEGER NOT NULL, `availableDeltaRub` INTEGER NOT NULL,
                `savingsDeltaRub` INTEGER NOT NULL, `debtDeltaRub` INTEGER NOT NULL,
                `beforeAvailableRub` INTEGER NOT NULL, `beforeSavingsRub` INTEGER NOT NULL,
                `beforeDebtRub` INTEGER NOT NULL, `afterAvailableRub` INTEGER NOT NULL,
                `afterSavingsRub` INTEGER NOT NULL, `afterDebtRub` INTEGER NOT NULL,
                `reasonId` TEXT, `metadata` TEXT, PRIMARY KEY(`id`)
            )
        """.trimIndent())
        if (db.hasTable("game_transactions")) {
            db.execSQL("""
                INSERT OR IGNORE INTO financial_operations (
                    id, typeCode, amountRub, timestampMillis,
                    availableDeltaRub, savingsDeltaRub, debtDeltaRub,
                    beforeAvailableRub, beforeSavingsRub, beforeDebtRub,
                    afterAvailableRub, afterSavingsRub, afterDebtRub,
                    reasonId, metadata
                )
                SELECT
                    operationId,
                    CASE WHEN amountRub < 0 THEN 'debit' ELSE 'credit' END,
                    ABS(amountRub), createdAtMillis,
                    amountRub, 0, 0,
                    balanceAfterRub - amountRub, 0, 0,
                    balanceAfterRub, 0, 0,
                    operationId, 'source=legacy-game-transaction'
                FROM game_transactions
            """.trimIndent())
            db.execSQL("DROP TABLE game_transactions")
        }
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `savings_goals` (
                `id` TEXT NOT NULL, `title` TEXT NOT NULL, `targetRub` INTEGER NOT NULL,
                `metadata` TEXT, `isActive` INTEGER NOT NULL, PRIMARY KEY(`id`)
            )
        """.trimIndent())
        if (hasLegacyWallet) {
            if (hasLegacyRoomZones) {
                db.execSQL("CREATE TEMP TABLE room_zones_backup AS SELECT sessionId, zoneId, boughtAtMillis FROM room_zones")
                db.execSQL("DROP TABLE room_zones")
            }
            db.execSQL("""
                CREATE TABLE `game_sessions_new` (
                    `id` TEXT NOT NULL, `hunger` INTEGER NOT NULL, `thirst` INTEGER NOT NULL,
                    `happiness` INTEGER NOT NULL, `health` INTEGER NOT NULL, `playerLevel` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
            """.trimIndent())
            db.execSQL("""
                INSERT INTO game_sessions_new (id, hunger, thirst, happiness, health, playerLevel)
                SELECT id, hunger, thirst, happiness, health, playerLevel FROM game_sessions
            """.trimIndent())
            db.execSQL("DROP TABLE game_sessions")
            db.execSQL("ALTER TABLE game_sessions_new RENAME TO game_sessions")
        }
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `room_zones` (
                `sessionId` TEXT NOT NULL, `zoneId` TEXT NOT NULL, `boughtAtMillis` INTEGER NOT NULL,
                PRIMARY KEY(`sessionId`, `zoneId`),
                FOREIGN KEY(`sessionId`) REFERENCES `game_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        if (hasLegacyWallet && hasLegacyRoomZones) {
            db.execSQL("INSERT INTO room_zones (sessionId, zoneId, boughtAtMillis) SELECT sessionId, zoneId, boughtAtMillis FROM room_zones_backup")
            db.execSQL("DROP TABLE room_zones_backup")
        }
    }
}

private fun SupportSQLiteDatabase.hasTable(name: String): Boolean =
    query("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?", arrayOf(name)).use { it.moveToFirst() }

private fun SupportSQLiteDatabase.hasColumn(table: String, column: String): Boolean =
    query("PRAGMA table_info(`$table`)").use { cursor ->
        val nameIndex = cursor.getColumnIndex("name")
        generateSequence { if (cursor.moveToNext()) cursor else null }
            .any { it.getString(nameIndex) == column }
    }

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `week_state` (`id` TEXT NOT NULL, `absoluteDay` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("INSERT OR IGNORE INTO week_state (id, absoluteDay) VALUES ('current', 1)")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `weekly_plans` (`weekNumber` INTEGER NOT NULL, `availableRub` INTEGER NOT NULL, `mandatoryPercent` INTEGER NOT NULL, `wantsPercent` INTEGER NOT NULL, `savingsPercent` INTEGER NOT NULL, PRIMARY KEY(`weekNumber`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `plan_actual_operations` (`operationId` TEXT NOT NULL, `weekNumber` INTEGER NOT NULL, `categoryCode` TEXT NOT NULL, `amountRub` INTEGER NOT NULL, PRIMARY KEY(`operationId`))")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `parent_help_state` (
                `id` TEXT NOT NULL,
                `offerId` TEXT NOT NULL,
                `receivedRub` INTEGER NOT NULL,
                `totalRepaymentRub` INTEGER NOT NULL,
                `remainingRub` INTEGER NOT NULL,
                `paymentsRemaining` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `learning_actions` (
                `profileId` TEXT NOT NULL,
                `actionId` TEXT NOT NULL,
                `actionType` TEXT NOT NULL,
                `gamePeriod` INTEGER NOT NULL,
                `sourceOperationId` TEXT,
                `payloadFingerprint` TEXT NOT NULL,
                `catalogVersion` INTEGER NOT NULL,
                PRIMARY KEY(`profileId`, `actionId`)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_learning_actions_profileId_actionType` ON `learning_actions` (`profileId`, `actionType`)")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `learning_metric_occurrences` (
                `profileId` TEXT NOT NULL,
                `metricId` TEXT NOT NULL,
                `actionId` TEXT NOT NULL,
                `gamePeriod` INTEGER NOT NULL,
                PRIMARY KEY(`profileId`, `metricId`, `actionId`)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_learning_metric_occurrences_profileId_metricId_gamePeriod` ON `learning_metric_occurrences` (`profileId`, `metricId`, `gamePeriod`)")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `learning_metric_progress` (
                `profileId` TEXT NOT NULL,
                `metricId` TEXT NOT NULL,
                `progressSteps` INTEGER NOT NULL,
                `qualifyingRepeats` INTEGER NOT NULL,
                `distinctPeriods` INTEGER NOT NULL,
                `currentStreak` INTEGER NOT NULL,
                `lastQualifyingPeriod` INTEGER,
                PRIMARY KEY(`profileId`, `metricId`)
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `achievement_unlocks` (
                `profileId` TEXT NOT NULL,
                `achievementId` TEXT NOT NULL,
                `sourceActionId` TEXT NOT NULL,
                `unlockedAtGamePeriod` INTEGER NOT NULL,
                `xpGrantId` TEXT NOT NULL,
                PRIMARY KEY(`profileId`, `achievementId`)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_achievement_unlocks_profileId_sourceActionId` ON `achievement_unlocks` (`profileId`, `sourceActionId`)")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `learning_explanations` (
                `profileId` TEXT NOT NULL,
                `explanationId` TEXT NOT NULL,
                PRIMARY KEY(`profileId`, `explanationId`)
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `achievement_xp_outbox` (
                `grantId` TEXT NOT NULL,
                `profileId` TEXT NOT NULL,
                `achievementId` TEXT NOT NULL,
                `amount` INTEGER NOT NULL,
                `delivered` INTEGER NOT NULL,
                PRIMARY KEY(`grantId`)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_achievement_xp_outbox_profileId_delivered` ON `achievement_xp_outbox` (`profileId`, `delivered`)")
    }
}
