package github.detrig.feature.productmarket.domain

import github.detrig.feature.productmarket.data.MarketTripCodec
import github.detrig.products.*
import org.junit.Assert.*
import org.junit.Test

class MarketRulesTest {
    private val config = MarketConfiguration()
    private val rules = MarketRules(config, DefaultProductCatalog()) { "trip-1" }
    private fun checkout() = rules.newTrip().copy(phase = MarketPhase.CHECKOUT, distance = config.endDistance)

    @Test fun shelvesContainTwelveItemsAndRequestsRecurNearEnd() {
        assertEquals(8, config.shelves.size)
        assertEquals(96, config.slots.size)
        assertTrue(config.shelves.all { it.products.size == 12 })
        assertEquals(12, config.slots.take(12).map { it.instanceId(0) }.distinct().size)
        for (need in config.requested) assertTrue(config.shelves.last().products.contains(need.productId))
    }

    @Test fun doubleTapCannotPickSameInstanceButCanPickTwoCarrots() {
        val first = config.slots[0]
        val second = config.slots[4]
        val trip = rules.pick(rules.newTrip(), first.instanceId(0), 360.0)
        assertEquals(trip, rules.pick(trip, first.instanceId(0), 360.0))
        val two = rules.pick(trip, second.instanceId(0), 360.0)
        assertEquals(2, two.cart[ProductIds.Carrot])
        assertFalse(two.missing().any { it.productId == ProductIds.Carrot })
    }

    @Test fun clippedAndOffscreenProductsCannotBePicked() {
        val trip = rules.newTrip()
        assertEquals(trip, rules.pick(trip, config.slots[3].instanceId(0), 300.0))
        assertEquals(trip, rules.pick(trip, config.slots[12].instanceId(0), 360.0))
        assertEquals(trip, rules.pick(trip, "not-an-instance", 360.0))
    }

    @Test fun extrasAreAllowedAndRemovalReopensListNeed() {
        val trip = rules.pick(rules.newTrip(), config.slots[2].instanceId(0), 360.0)
        assertEquals(1, trip.extra(ProductIds.Berries))
        assertTrue(rules.remove(trip, ProductIds.Berries).cart.isEmpty())
        val complete = checkout().copy(cart = mapOf(ProductIds.Groats to 1, ProductIds.Carrot to 2, ProductIds.Apple to 1))
        assertTrue(complete.missing().isEmpty())
        assertEquals(listOf(ProductQuantity(ProductIds.Carrot, 1)), rules.remove(complete, ProductIds.Carrot).missing())
    }

    @Test fun capIsAppliedWithoutConsumingAnInstance() {
        val trip = rules.newTrip().copy(cart = mapOf(ProductIds.Carrot to config.maximumQuantity))
        assertEquals(trip, rules.pick(trip, config.slots[0].instanceId(0), 360.0))
    }

    @Test fun largeFrameCannotSkipShelfOrAdvanceWhileAtCheckout() {
        val next = rules.advance(rules.newTrip(), 60.0)
        assertEquals(config.speed * .1, next.distance, .00001)
        assertEquals(checkout(), rules.advance(checkout(), .1))
        assertEquals(next, rules.advance(next, Double.NaN))
    }

    @Test fun continuousRouteBrakesAndReachesCounterBeforeSheet() {
        var trip = rules.newTrip()
        var frames = 0
        while (trip.phase == MarketPhase.WALKING && frames < 10_000) {
            val before = trip
            trip = rules.advance(trip, .02)
            assertTrue(trip.distance >= before.distance)
            assertTrue(trip.distance - before.distance <= config.speed * .02 + .00001)
            if (before.distance > config.endDistance - 40) {
                assertTrue(trip.distance - before.distance < config.speed * .02)
            }
            frames++
        }
        assertEquals(MarketPhase.ARRIVED, trip.phase)
        assertEquals(config.endDistance, trip.distance, 0.0)
        assertTrue(trip.activeSeconds in 40.0..46.0)
        repeat(6) { trip = rules.advance(trip, .1) }
        assertEquals(MarketPhase.ARRIVED, trip.phase)
        repeat(2) { trip = rules.advance(trip, .1) }
        assertEquals(MarketPhase.CHECKOUT, trip.phase)
        assertEquals(config.endDistance, trip.distance, 0.0)
    }

    @Test fun anotherPassKeepsCartButHasFreshInstances() {
        val before = checkout().copy(cart = mapOf(ProductIds.Carrot to 1), picked = setOf("0:0:0"))
        val next = rules.anotherPass(before)
        assertEquals(before.cart, next.cart)
        assertEquals(1, next.lap)
        assertTrue(next.picked.isEmpty())
        assertEquals(0.0, next.distance, 0.0)
        assertEquals(2, rules.pick(next, config.slots[0].instanceId(1), 360.0).cart[ProductIds.Carrot])
    }

    @Test fun emptyFinishIsAllowedAndIdempotent() {
        val finished = rules.finish(checkout())
        assertEquals(MarketPhase.FINISHED, finished.phase)
        assertEquals("trip-1", finished.lastResult?.tripId)
        assertEquals(config.requested, finished.lastResult?.missing)
        assertTrue(requireNotNull(finished.lastResult).items.isEmpty())
        assertEquals(finished, rules.finish(finished))
        assertEquals(finished, rules.remove(finished, ProductIds.Carrot))
        assertEquals(rules.newTrip(), rules.finish(rules.newTrip()))
    }

    @Test fun codecRestoresFullTripAndLastResultWithoutOfflineMovement() {
        val previous = requireNotNull(rules.finish(checkout()).lastResult)
        val trip = rules.pick(rules.newTrip(previous), config.slots[0].instanceId(0), 360.0)
            .copy(distance = 172.5, activeSeconds = 3.75, arrivalSeconds = .3)
        val codec = MarketTripCodec()
        assertEquals(trip, rules.restore(codec.decode(codec.encode(trip))))
    }

    @Test fun routeUpdatePreservesGoodsAndRejectsCorruptSave() {
        val old = rules.newTrip().copy(routeVersion = 2, distance = 700.0, cart = mapOf(ProductIds.Apple to 1))
        val restored = rules.restore(old)
        assertEquals(old.cart, restored.cart)
        assertEquals(0.0, restored.distance, 0.0)
        assertEquals(1, restored.lap)
        assertThrows(IllegalArgumentException::class.java) { rules.restore(old.copy(distance = Double.NaN)) }
        assertThrows(IllegalArgumentException::class.java) {
            rules.restore(old.copy(cart = mapOf(ProductId("unknown") to 1)))
        }
        assertThrows(Exception::class.java) { MarketTripCodec().decode("{broken") }
    }

    @Test fun routeUpdateDoesNotReopenAlreadyFinishedTrip() {
        val finished = rules.finish(checkout()).copy(routeVersion = 2)
        val restored = rules.restore(finished)
        assertEquals(MarketPhase.FINISHED, restored.phase)
        assertEquals(finished.lastResult, restored.lastResult)
    }
}
