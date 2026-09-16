package github.detrig.feature.productmarket.domain

import github.detrig.feature.productmarket.api.MarketTripResult
import github.detrig.products.ProductId
import github.detrig.products.ProductQuantity

internal enum class MarketPhase { WALKING, ARRIVED, CHECKOUT, FINISHED }

internal data class MarketTrip(
    val id: String,
    val routeVersion: Int,
    val requested: List<ProductQuantity>,
    val phase: MarketPhase = MarketPhase.WALKING,
    val distance: Double = 0.0,
    val activeSeconds: Double = 0.0,
    val arrivalSeconds: Double = 0.0,
    val lap: Int = 0,
    val cart: Map<ProductId, Int> = emptyMap(),
    val picked: Set<String> = emptySet(),
    val hintDismissed: Boolean = false,
    val lastResult: MarketTripResult? = null,
) {
    val itemCount: Int get() = cart.values.sum()
    fun missing(): List<ProductQuantity> = requested.mapNotNull {
        val missing = it.quantity - (cart[it.productId] ?: 0)
        if (missing > 0) ProductQuantity(it.productId, missing) else null
    }
    fun extra(id: ProductId): Int =
        ((cart[id] ?: 0) - (requested.firstOrNull { it.productId == id }?.quantity ?: 0)).coerceAtLeast(0)
}
