package github.detrig.feature.productmarket.data

import github.detrig.feature.productmarket.api.MarketTripResult
import github.detrig.feature.productmarket.domain.MarketPhase
import github.detrig.feature.productmarket.domain.MarketTrip
import github.detrig.products.ProductId
import github.detrig.products.ProductQuantity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class MarketTripCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(trip: MarketTrip): String = json.encodeToString(StoredTrip(
        format = 1, id = trip.id, routeVersion = trip.routeVersion, phase = trip.phase.name,
        distance = trip.distance, activeSeconds = trip.activeSeconds, arrivalSeconds = trip.arrivalSeconds,
        lap = trip.lap, requested = trip.requested.associate { it.productId.value to it.quantity },
        cart = trip.cart.mapKeys { it.key.value }, picked = trip.picked, hintDismissed = trip.hintDismissed,
        result = trip.lastResult?.let { StoredResult(it.tripId,
            it.items.associate { q -> q.productId.value to q.quantity },
            it.missing.associate { q -> q.productId.value to q.quantity }) },
    ))

    fun decode(value: String): MarketTrip {
        val stored = json.decodeFromString<StoredTrip>(value)
        require(stored.format == 1) { "Unsupported market save format" }
        return MarketTrip(stored.id, stored.routeVersion, stored.requested.quantities(),
            MarketPhase.valueOf(stored.phase), stored.distance, stored.activeSeconds, stored.arrivalSeconds,
            stored.lap, stored.cart.mapKeys { ProductId(it.key) }, stored.picked, stored.hintDismissed,
            stored.result?.let { MarketTripResult(it.tripId, it.items.quantities(), it.missing.quantities()) })
    }

    private fun Map<String, Int>.quantities() = map { ProductQuantity(ProductId(it.key), it.value) }

    @Serializable
    private data class StoredTrip(
        val format: Int,
        val id: String,
        val routeVersion: Int,
        val phase: String,
        val distance: Double,
        val activeSeconds: Double,
        val arrivalSeconds: Double,
        val lap: Int,
        val requested: Map<String, Int>,
        val cart: Map<String, Int>,
        val picked: Set<String>,
        val hintDismissed: Boolean,
        val result: StoredResult? = null,
    )

    @Serializable
    private data class StoredResult(val tripId: String, val items: Map<String, Int>, val missing: Map<String, Int>)
}
