package github.detrig.internetbooster.database

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import github.detrig.core.database.RoomDatabaseFactory
import github.detrig.core.database.RoomTransactionRunner
import github.detrig.core.database.create
import github.detrig.minigames.flight.data.FlightDao
import github.detrig.minigames.flight.data.FlightProgressEntity

@Database(entities = [FlightProgressEntity::class], version = 1, exportSchema = false)
abstract class FlightDatabase : RoomDatabase() {
    abstract fun flightDao(): FlightDao
}

internal class FlightDatabaseModule(context: Context) {
    val database: FlightDatabase by lazy {
        RoomDatabaseFactory.create(context.applicationContext, "fin_pet_flight.db")
    }
    val dao by lazy { database.flightDao() }
    val transactionRunner by lazy { RoomTransactionRunner(database) }
}
