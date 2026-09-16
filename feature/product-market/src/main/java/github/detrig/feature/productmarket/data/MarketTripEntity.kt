package github.detrig.feature.productmarket.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Одна ограниченная по размеру запись черновика и последнего результата. */
@Entity(tableName = "product_market_trip")
data class MarketTripEntity(
    @PrimaryKey val id: Int = 1,
    val snapshotJson: String,
)
