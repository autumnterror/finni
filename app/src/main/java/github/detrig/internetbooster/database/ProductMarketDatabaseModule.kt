package github.detrig.internetbooster.database

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import github.detrig.core.database.RoomDatabaseFactory
import github.detrig.core.database.create
import github.detrig.feature.productmarket.data.MarketTripDao
import github.detrig.feature.productmarket.data.MarketTripEntity

@Database(entities = [MarketTripEntity::class], version = 1, exportSchema = true)
internal abstract class ProductMarketDatabase : RoomDatabase() {
    abstract fun tripDao(): MarketTripDao
}

/** Отдельное сохранение похода, не меняющее схему профиля и баланс игрока. */
internal class ProductMarketDatabaseModule(private val context: Context) {
    private val database: ProductMarketDatabase by lazy {
        RoomDatabaseFactory.create(context = context, databaseName = "finpet_product_market.db")
    }
    val tripDao: MarketTripDao by lazy { database.tripDao() }
}
