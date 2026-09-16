package github.detrig.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

/**
 * Фабрика для создания Room database.
 *
 * Core не знает конкретные Entity и Dao. Он только хранит общий способ создания базы,
 * а конкретный AppDatabase создается в app/module/mediator слое.
 */
object RoomDatabaseFactory {

    fun <T : RoomDatabase> create(
        context: Context,
        databaseClass: Class<T>,
        databaseName: String,
        migrations: Array<Migration> = emptyArray(),
        fallbackToDestructiveMigration: Boolean = false,
    ): T {
        val builder = Room.databaseBuilder(
            context = context,
            klass = databaseClass,
            name = databaseName,
        ).addMigrations(*migrations)

        if (fallbackToDestructiveMigration) {
            builder.fallbackToDestructiveMigration(dropAllTables = true)
        }

        return builder.build()
    }
}

/**
 * Reified-версия создания базы.
 *
 * Пример:
 * ```kotlin
 * val database = RoomDatabaseFactory.create<AppDatabase>(context, "app.db")
 * ```
 */
inline fun <reified T : RoomDatabase> RoomDatabaseFactory.create(
    context: Context,
    databaseName: String,
    migrations: Array<Migration> = emptyArray(),
    fallbackToDestructiveMigration: Boolean = false,
): T {
    return create(
        context = context,
        databaseClass = T::class.java,
        databaseName = databaseName,
        migrations = migrations,
        fallbackToDestructiveMigration = fallbackToDestructiveMigration,
    )
}

