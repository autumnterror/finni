package github.detrig.internetbooster.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object FinPetMigrations {
    val FROM_6_TO_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS room_zones (sessionId TEXT NOT NULL, zoneId TEXT NOT NULL, boughtAtMillis INTEGER NOT NULL, PRIMARY KEY(sessionId, zoneId), FOREIGN KEY(sessionId) REFERENCES game_sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE TABLE IF NOT EXISTS game_transactions (sessionId TEXT NOT NULL, operationId TEXT NOT NULL, amountRub INTEGER NOT NULL, balanceAfterRub INTEGER NOT NULL, createdAtMillis INTEGER NOT NULL, PRIMARY KEY(sessionId, operationId), FOREIGN KEY(sessionId) REFERENCES game_sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        }
    }

    val FROM_7_TO_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS fishing_progress (profileId TEXT NOT NULL PRIMARY KEY, payload TEXT NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS pet_play_effects (operationId TEXT NOT NULL PRIMARY KEY, profileId TEXT NOT NULL, sessionId TEXT NOT NULL, gameId TEXT NOT NULL, happinessDelta INTEGER NOT NULL, appliedAtMillis INTEGER NOT NULL)")
        }
    }

    /** В раннем preview полёта версия 8 ещё не содержала таблицу рыбалки. */
    val FROM_8_TO_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS fishing_progress (profileId TEXT NOT NULL PRIMARY KEY, payload TEXT NOT NULL)")
        }
    }
}
