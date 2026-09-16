package github.detrig.feature.productmarket.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MarketTripDao {
    @Query("SELECT * FROM product_market_trip WHERE id = 1")
    suspend fun read(): MarketTripEntity?

    @Query("SELECT * FROM product_market_trip WHERE id = 1")
    fun observe(): Flow<MarketTripEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: MarketTripEntity)
}
