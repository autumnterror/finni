package github.detrig.core.database

import androidx.room.RoomDatabase
import androidx.room.withTransaction

/**
 * Обертка для запуска операций внутри Room transaction.
 *
 * Нужна, чтобы repository/use case не зависели от конкретного AppDatabase,
 * если им нужно выполнить несколько DAO-операций атомарно.
 */
class RoomTransactionRunner(
    private val database: RoomDatabase,
) {

    suspend fun <T> runInTransaction(block: suspend () -> T): T {
        return database.withTransaction(block)
    }
}
