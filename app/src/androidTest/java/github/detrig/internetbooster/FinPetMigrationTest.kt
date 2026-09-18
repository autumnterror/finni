package github.detrig.internetbooster

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import github.detrig.internetbooster.database.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class FinPetMigrationTest {
    @Test fun versionSevenPreservesWalletPetPurchasedZonesAndTransactions() = verifyMigration(7)
    @Test fun versionSixPreservesWalletAndPet() = verifyMigration(6)
    @Test fun versionEightFishingPreservesItsSaveAndPlayReceipts() = verifyMigration(8, hasFishingSave = true)
    @Test fun versionEightFlightPreviewAddsFishingWithoutResettingProfile() = verifyMigration(8)

    @Test fun economyBranchVersionSevenKeepsEconomyAndAddsMasterTables() {
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val name = "migration-economy-branch-${UUID.randomUUID()}.db"
            val old = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null)
            old.execSQL("CREATE TABLE game_sessions (id TEXT NOT NULL PRIMARY KEY, hunger INTEGER NOT NULL, thirst INTEGER NOT NULL, happiness INTEGER NOT NULL, health INTEGER NOT NULL, playerLevel INTEGER NOT NULL)")
            old.execSQL("INSERT INTO game_sessions VALUES ('current', 61, 47, 83, 91, 3)")
            old.execSQL("CREATE TABLE economy_state (id TEXT NOT NULL PRIMARY KEY, availableRub INTEGER NOT NULL, savingsRub INTEGER NOT NULL, debtRub INTEGER NOT NULL, periodicAmountRub INTEGER NOT NULL, periodicPeriodMillis INTEGER NOT NULL, nextPeriodicAtMillis INTEGER NOT NULL)")
            old.execSQL("INSERT INTO economy_state VALUES ('current', 321, 45, 20, 500, 604800000, 123456789)")
            old.execSQL("CREATE TABLE financial_operations (id TEXT NOT NULL PRIMARY KEY, typeCode TEXT NOT NULL, amountRub INTEGER NOT NULL, timestampMillis INTEGER NOT NULL, availableDeltaRub INTEGER NOT NULL, savingsDeltaRub INTEGER NOT NULL, debtDeltaRub INTEGER NOT NULL, beforeAvailableRub INTEGER NOT NULL, beforeSavingsRub INTEGER NOT NULL, beforeDebtRub INTEGER NOT NULL, afterAvailableRub INTEGER NOT NULL, afterSavingsRub INTEGER NOT NULL, afterDebtRub INTEGER NOT NULL, reasonId TEXT, metadata TEXT)")
            old.execSQL("CREATE TABLE savings_goals (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, targetRub INTEGER NOT NULL, metadata TEXT, isActive INTEGER NOT NULL)")
            old.version = 7
            old.close()

            val db = Room.databaseBuilder(context, FinPetDatabase::class.java, name)
                .addMigrations(
                    FinPetMigrations.FROM_7_TO_8,
                    FinPetMigrations.FROM_8_TO_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12,
                    MIGRATION_12_13,
                )
                .build()
            try {
                assertEquals(83, db.gameStateDao().getCurrentState()!!.happiness)
                val economy = db.economyDao().getState()!!
                assertEquals(321L, economy.availableRub)
                assertEquals(45L, economy.savingsRub)
                assertEquals(20L, economy.debtRub)
                assertFalse(db.roomZoneDao().isOwned("current", "flight"))
                assertEquals(13, db.openHelper.readableDatabase.version)
                assertNotNull(db.weekDao().getState())
                db.planningDao().getPlan(2)
            } finally {
                db.close()
                context.deleteDatabase(name)
            }
        }
    }

    private fun verifyMigration(from: Int, hasFishingSave: Boolean = false) = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration-fishing-${UUID.randomUUID()}.db"
        val old = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null)
        old.execSQL("CREATE TABLE game_sessions (id TEXT NOT NULL PRIMARY KEY, balanceRub INTEGER NOT NULL, nextAllowanceAmountRub INTEGER NOT NULL, nextAllowanceAtMillis INTEGER NOT NULL, hunger INTEGER NOT NULL, thirst INTEGER NOT NULL, happiness INTEGER NOT NULL, health INTEGER NOT NULL, playerLevel INTEGER NOT NULL)")
        old.execSQL("INSERT INTO game_sessions VALUES ('current', 123, 500, 123456789, 61, 47, 83, 91, 3)")
        if (from >= 7) {
            old.execSQL("CREATE TABLE room_zones (sessionId TEXT NOT NULL, zoneId TEXT NOT NULL, boughtAtMillis INTEGER NOT NULL, PRIMARY KEY(sessionId, zoneId), FOREIGN KEY(sessionId) REFERENCES game_sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            old.execSQL("CREATE TABLE game_transactions (sessionId TEXT NOT NULL, operationId TEXT NOT NULL, amountRub INTEGER NOT NULL, balanceAfterRub INTEGER NOT NULL, createdAtMillis INTEGER NOT NULL, PRIMARY KEY(sessionId, operationId), FOREIGN KEY(sessionId) REFERENCES game_sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            old.execSQL("INSERT INTO room_zones VALUES ('current', 'fishing', 42)")
            old.execSQL("INSERT INTO game_transactions VALUES ('current', 'room-zone:fishing', -400, 123, 42)")
        }
        val fishingPayload = """{"profileId":"current","recordGrams":1000}"""
        if (from == 8) {
            old.execSQL("CREATE TABLE pet_play_effects (operationId TEXT NOT NULL PRIMARY KEY, profileId TEXT NOT NULL, sessionId TEXT NOT NULL, gameId TEXT NOT NULL, happinessDelta INTEGER NOT NULL, appliedAtMillis INTEGER NOT NULL)")
            old.execSQL("INSERT INTO pet_play_effects VALUES ('current:round', 'current', 'round', 'fishing', 3, 42)")
            if (hasFishingSave) {
                old.execSQL("CREATE TABLE fishing_progress (profileId TEXT NOT NULL PRIMARY KEY, payload TEXT NOT NULL)")
                old.execSQL("INSERT INTO fishing_progress VALUES (?, ?)", arrayOf("current", fishingPayload))
            }
        }
        old.version = from
        old.close()
        val db = Room.databaseBuilder(context, FinPetDatabase::class.java, name)
            .addMigrations(
                FinPetMigrations.FROM_6_TO_7,
                FinPetMigrations.FROM_7_TO_8,
                FinPetMigrations.FROM_8_TO_9,
                MIGRATION_9_10,
                MIGRATION_10_11,
                MIGRATION_11_12,
                MIGRATION_12_13,
            )
            .build()
        try {
            val state = db.gameStateDao().getCurrentState()!!
            assertEquals(83, state.happiness)
            val economy = db.economyDao().getState()!!
            assertEquals(123L, economy.availableRub)
            assertEquals(500L, economy.periodicAmountRub)
            assertEquals(123456789L, economy.nextPeriodicAtMillis)
            assertEquals(from >= 7, db.roomZoneDao().isOwned("current", "fishing"))
            assertEquals(if (hasFishingSave) fishingPayload else null, db.fishingDao().read("current")?.payload)
            if (from == 8) assertEquals(3, db.petPlayEffectDao().find("current:round")?.happinessDelta)
            else assertNull(db.petPlayEffectDao().find("current:round"))
            assertEquals(13, db.openHelper.readableDatabase.version)
            assertNotNull(db.weekDao().getState())
            db.planningDao().getPlan(2)
            db.openHelper.readableDatabase.query("SELECT count(*) FROM financial_operations").use {
                it.moveToFirst(); assertEquals(if (from >= 7) 1 else 0, it.getInt(0))
            }
            if (from >= 7) {
                val operation = db.economyDao().getOperation("room-zone:fishing")!!
                assertEquals("debit", operation.typeCode)
                assertEquals(400L, operation.amountRub)
                assertEquals(123L, operation.afterAvailableRub)
            }
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
