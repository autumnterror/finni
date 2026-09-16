package github.detrig.minigames.flight

import github.detrig.minigames.flight.domain.*
import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class FlightEngineTest {
    private val config = FlightConfig.decode(File("src/main/assets/flight_balance.json").readText())
    private val engine = FlightEngine(config)
    private fun initial(seed: Int = 1, training: Boolean = false) =
        engine.create("session", "profile", "pet", seed, 100, training)

    @Test fun readyDoesNotSpendTimeAndFirstTapStarts() {
        val ready = initial()
        assertEquals(ready, engine.advance(ready, 1000))
        val flying = engine.flap(ready)
        assertTrue(flying.started)
        assertEquals(1, flying.flapCount)
        assertEquals(config.physics.flapVelocity, flying.velocity, 0.0)
        assertTrue(engine.step(flying).y < ready.y)
    }

    @Test fun flapReplacesVelocityAndRejectsRepeatedCallback() {
        val once = engine.flap(initial())
        assertEquals(once, engine.flap(once))
        val later = engine.advance(once, 12)
        assertEquals(config.physics.flapVelocity, engine.flap(later).velocity, 0.0)
        assertEquals(2, engine.flap(later).flapCount)
    }

    @Test fun noInputLandsWithoutNegativeScore() {
        val end = engine.advance(engine.flap(initial()), 1200)
        assertEquals(FlightOutcome.LANDED, end.outcome)
        assertEquals(0, end.score)
        assertEquals(end, engine.advance(end, 1000))
        assertEquals(end, engine.flap(end))
    }

    @Test fun passedGateAwardsOnceAfterWholeHitbox() {
        val s = initial().copy(started = true, y = 280.0,
            gates = listOf(FlightGate(0, 17.0, 280.0, 220.0)))
        val passed = engine.step(s)
        assertEquals(1, passed.score)
        assertEquals(1, engine.advance(passed, 10).score)
    }

    @Test fun collisionWinsOverPointAndDeadline() {
        val s = initial().copy(started = true, tick = 7199, y = config.world.bottom - config.world.radius + 1,
            gates = listOf(FlightGate(0, 17.0, 280.0, 220.0)))
        val next = engine.step(s)
        assertEquals(FlightOutcome.LANDED, next.outcome)
        assertEquals(0, next.score)
    }

    @Test fun roundFinishesAtExactActiveDeadline() {
        val s = initial().copy(started = true, tick = 7199, y = 280.0)
        val next = engine.step(s)
        assertEquals(7200, next.tick)
        assertEquals(FlightOutcome.FINISHED, next.outcome)
    }

    @Test fun circleCornerUsesDistanceInsteadOfBoundingSquare() {
        assertFalse(FlightEngine.circleHitsRect(0.0, 0.0, 5.0, 4.0, 4.0, 10.0, 10.0))
        assertTrue(FlightEngine.circleHitsRect(0.0, 0.0, 5.0, 3.0, 4.0, 10.0, 10.0))
    }

    @Test fun trainingRescuesAndDoesNotAwardMissedGate() {
        val s = initial(training = true).copy(started = true, y = 100.0,
            gates = listOf(FlightGate(0, 80.0, 280.0, 220.0)))
        val rescued = engine.step(s)
        assertNull(rescued.outcome)
        assertTrue(rescued.gates.first().missed)
        val crossed = engine.step(rescued.copy(gates = listOf(rescued.gates.first().copy(x = 21.0))))
        assertEquals(0, crossed.score)
    }

    @Test fun serializedCheckpointPreservesRngAndFuture() {
        var s = engine.flap(initial(seed = -84575))
        repeat(800) { s = pilot(s) }
        val restored = Json.decodeFromString<FlightSession>(Json.encodeToString(s))
        var a = s
        var b = restored
        repeat(1000) { a = pilot(a); b = pilot(b) }
        assertEquals(a, b)
    }

    @Test fun physicalGroupingDoesNotChangeTrajectory() {
        var a = engine.flap(initial())
        var b = a
        repeat(20) {
            a = engine.flap(a); b = engine.flap(b)
            a = engine.advance(a, 60)
            repeat(15) { b = engine.advance(b, 4) }
        }
        assertEquals(a, b)
    }

    @Test fun frameRatesHaveEqualTickCountAndLongStallIsBounded() {
        val counts = listOf(30, 60, 90, 120, 144).map { rate ->
            val clock = FlightFrameClock(120, 250)
            clock.frame(0)
            (1..rate * 10).sumOf { clock.frame(it * 1_000_000_000L / rate).ticks }
        }
        assertEquals(listOf(1200, 1200, 1200, 1200, 1200), counts)
        val clock = FlightFrameClock(120, 100)
        clock.frame(0)
        assertEquals(12, clock.frame(1_000_000_000).ticks)
        assertEquals(2, clock.frame(1_016_666_667).ticks)
        clock.reset()
        assertEquals(0, clock.frame(2_000_000_000).ticks)
    }

    @Test fun initialGatesAreWideAndDoNotChangeWhenStageChanges() {
        val s = initial().copy(started = true, tick = 2399)
        assertEquals(config.gates.firstGap, s.gates.first().gap, 0.0)
        assertEquals(config.stages.first().gap, s.gates[1].gap, 0.0)
        assertEquals(s.gates.first().gap, engine.advance(s, 2).gates.first().gap, 0.0)
    }

    @Test fun thousandSeedsReachFinishWithSimpleController() {
        var minimumScore = Int.MAX_VALUE
        repeat(1000) { seed ->
            var s = engine.flap(initial(seed = seed))
            repeat(config.round.durationTicks) { s = pilot(s) }
            assertEquals("seed=$seed tick=${s.tick} y=${s.y} score=${s.score}", FlightOutcome.FINISHED, s.outcome)
            minimumScore = minOf(minimumScore, s.score)
            assertTrue(s.gates.size <= 5)
        }
        println("1000 seeds completed; minimum score=$minimumScore")
    }

    @Test fun runtimeAndSpecConfigurationMatch() {
        assertEquals(File("../../../docs/pet-flight/balance.v1.json").readText(),
            File("src/main/assets/flight_balance.json").readText())
    }

    private fun pilot(s: FlightSession): FlightSession {
        val gate = s.gates.firstOrNull { it.x + config.gates.width >= config.world.petX - config.world.radius }
        val target = gate?.center ?: config.world.startY
        val flap = s.velocity >= 0 && s.y >= target + 18
        return engine.step(if (flap) engine.flap(s) else s)
    }
}
