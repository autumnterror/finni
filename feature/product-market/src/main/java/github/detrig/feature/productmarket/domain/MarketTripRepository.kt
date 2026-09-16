package github.detrig.feature.productmarket.domain

import github.detrig.feature.productmarket.api.MarketTripResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

internal interface MarketTripRepository {
    val saveFailed: StateFlow<Boolean>
    suspend fun load(): MarketTrip?
    suspend fun save(trip: MarketTrip)
    fun checkpoint(trip: MarketTrip)
    fun observeLastResult(): Flow<MarketTripResult?>
}
