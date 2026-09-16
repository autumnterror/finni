package github.detrig.feature.productmarket.domain

import github.detrig.feature.productmarket.api.MarketTripResult
import github.detrig.products.ProductCatalog
import github.detrig.products.ProductId
import github.detrig.products.ProductQuantity
import java.util.UUID

/** Все решения похода живут вне UI; анимации лишь отображают результат. */
internal class MarketRules(
    val config: MarketConfiguration,
    private val catalog: ProductCatalog,
    private val createId: () -> String = { UUID.randomUUID().toString() },
) {
    init {
        require(config.slots.all { catalog.find(it.productId) != null })
        require(config.requested.all { catalog.find(it.productId) != null })
    }

    fun newTrip(lastResult: MarketTripResult? = null) =
        MarketTrip(createId(), config.routeVersion, config.requested, lastResult = lastResult)

    fun restore(trip: MarketTrip): MarketTrip {
        require(trip.id.isNotBlank() && trip.lap >= 0)
        require(trip.distance.isFinite() && trip.distance >= 0)
        require(trip.activeSeconds.isFinite() && trip.activeSeconds >= 0)
        require(trip.arrivalSeconds.isFinite() && trip.arrivalSeconds >= 0)
        require(trip.cart.all { (id, count) -> catalog.find(id) != null && count in 1..config.maximumQuantity })
        require(trip.requested.all { catalog.find(it.productId) != null && it.quantity <= config.maximumQuantity })
        require(trip.requested.isNotEmpty() && trip.requested.map { it.productId }.distinct().size == trip.requested.size)
        require(trip.phase != MarketPhase.FINISHED || trip.lastResult?.tripId == trip.id)
        if (trip.routeVersion != config.routeVersion) {
            if (trip.phase == MarketPhase.FINISHED) return trip.copy(
                routeVersion = config.routeVersion, distance = config.endDistance, picked = emptySet())
            // Обновилась выкладка: сохраняем собранное, начинаем безопасный новый проход.
            return trip.copy(routeVersion = config.routeVersion, distance = 0.0, lap = trip.lap + 1,
                picked = emptySet(), phase = MarketPhase.WALKING, arrivalSeconds = 0.0)
        }
        require(trip.distance <= config.endDistance)
        require(trip.phase == MarketPhase.WALKING || trip.distance == config.endDistance)
        require(trip.picked.all { id -> config.slots.any { it.instanceId(trip.lap) == id } })
        return trip
    }

    fun advance(trip: MarketTrip, elapsedSeconds: Double): MarketTrip {
        if (!elapsedSeconds.isFinite() || elapsedSeconds <= 0) return trip
        // После задержки кадра не пролетаем мимо целого стеллажа.
        val dt = elapsedSeconds.coerceAtMost(.1)
        return when (trip.phase) {
            MarketPhase.WALKING -> {
                val remaining = config.endDistance - trip.distance
                val factor = .3 + .7 * (remaining / config.brakingDistance).coerceIn(0.0, 1.0)
                val distance = (trip.distance + config.speed * factor * dt).coerceAtMost(config.endDistance)
                trip.copy(distance = distance, activeSeconds = trip.activeSeconds + dt,
                    phase = if (distance >= config.endDistance) MarketPhase.ARRIVED else MarketPhase.WALKING,
                    hintDismissed = trip.hintDismissed || distance >= config.checkoutWorldX)
            }
            MarketPhase.ARRIVED -> {
                val arrival = trip.arrivalSeconds + dt
                trip.copy(arrivalSeconds = arrival,
                    phase = if (arrival >= config.arrivalPauseSeconds) MarketPhase.CHECKOUT else MarketPhase.ARRIVED)
            }
            else -> trip
        }
    }

    fun visible(slot: MarketSlot, distance: Double, viewportWidth: Double): Boolean =
        viewportWidth.isFinite() && viewportWidth > 0 &&
            slot.worldX - distance >= 0 && slot.worldX + slot.width - distance <= viewportWidth

    fun pick(trip: MarketTrip, instanceId: String, viewportWidth: Double): MarketTrip {
        if (trip.phase != MarketPhase.WALKING || instanceId in trip.picked) return trip
        val slot = config.slots.firstOrNull { it.instanceId(trip.lap) == instanceId } ?: return trip
        if (!visible(slot, trip.distance, viewportWidth)) return trip
        val count = trip.cart[slot.productId] ?: 0
        if (count >= config.maximumQuantity) return trip
        return trip.copy(cart = trip.cart + (slot.productId to count + 1),
            picked = trip.picked + instanceId, hintDismissed = true)
    }

    fun remove(trip: MarketTrip, productId: ProductId): MarketTrip {
        if (trip.phase == MarketPhase.FINISHED) return trip
        val count = trip.cart[productId] ?: return trip
        val cart = if (count == 1) trip.cart - productId else trip.cart + (productId to count - 1)
        return trip.copy(cart = cart)
    }

    fun anotherPass(trip: MarketTrip): MarketTrip =
        if (trip.phase != MarketPhase.CHECKOUT) trip else trip.copy(
            phase = MarketPhase.WALKING, distance = 0.0, arrivalSeconds = 0.0,
            lap = trip.lap + 1, picked = emptySet(), hintDismissed = true)

    fun finish(trip: MarketTrip): MarketTrip {
        if (trip.phase != MarketPhase.CHECKOUT) return trip
        return trip.copy(phase = MarketPhase.FINISHED, lastResult = MarketTripResult(
            tripId = trip.id, items = trip.cart.map { ProductQuantity(it.key, it.value) }, missing = trip.missing()))
    }
}
