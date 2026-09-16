package github.detrig.feature.productmarket

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import github.detrig.feature.productmarket.data.*
import github.detrig.feature.productmarket.domain.*
import github.detrig.products.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@Database(entities = [MarketTripEntity::class], version = 1, exportSchema = false)
abstract class MarketTestDatabase : RoomDatabase() {
    abstract fun trips(): MarketTripDao
}

@RunWith(AndroidJUnit4::class)
class MarketStorageTest {
    @Test fun processReopenRestoresPositionCartAndFinishedResult() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "market-storage-test.db"
        context.deleteDatabase(databaseName)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val rules = MarketRules(MarketConfiguration(), DefaultProductCatalog()) { "stored-trip" }
        val codec = MarketTripCodec()
        var database = Room.databaseBuilder(context, MarketTestDatabase::class.java, databaseName).build()
        try {
            val trip = rules.newTrip().copy(distance = 222.25, activeSeconds = 4.8, lap = 2,
                cart = mapOf(ProductIds.Carrot to 2, ProductIds.Berries to 1), picked = setOf("2:0:0", "2:0:4"))
            MarketTripRepositoryImpl(database.trips(), codec, scope).save(trip)
            database.close()
            database = Room.databaseBuilder(context, MarketTestDatabase::class.java, databaseName).build()
            val restored = RestoreMarketTripInteractor(MarketTripRepositoryImpl(database.trips(), codec, scope), rules)()
            assertEquals(trip, restored)
            val finished = rules.finish(restored.copy(phase = MarketPhase.CHECKOUT, distance = rules.config.endDistance))
            MarketTripRepositoryImpl(database.trips(), codec, scope).save(finished)
            database.close()
            database = Room.databaseBuilder(context, MarketTestDatabase::class.java, databaseName).build()
            assertEquals(finished, MarketTripRepositoryImpl(database.trips(), codec, scope).load())
        } finally {
            database.close()
            scope.cancel()
            context.deleteDatabase(databaseName)
        }
    }
}
