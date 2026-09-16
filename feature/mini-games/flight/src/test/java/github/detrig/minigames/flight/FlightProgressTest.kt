package github.detrig.minigames.flight

import github.detrig.minigames.flight.domain.*
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class FlightProgressTest {
    private val config = FlightConfig.decode(File("src/main/assets/flight_balance.json").readText())
    private fun session(id: String = "one", score: Int = 12) =
        FlightEngine(config).create(id, "p", "pet", 1, 100)
            .copy(started = true, score = score, tick = 2000, flapCount = 25, outcome = FlightOutcome.LANDED)

    @Test fun resultRecordBadgesAndEffectAreAtomicAndIdempotent() {
        val s = session()
        val initial = FlightProgress(profileId = "p", active = s.copy(outcome = null))
        val done = initial.finish(s, config, 500)
        assertNull(done.active)
        assertEquals(12, done.records[config.rulesVersion]?.score)
        assertEquals(setOf("first_flight", "skilled_pilot"), done.badges)
        assertEquals(1, done.pendingEffects.size)
        assertEquals(done, done.finish(s, config, 900))
    }

    @Test fun zeroIsNotANewRecord() {
        val s = session(score = 0)
        val done = FlightProgress(profileId = "p", active = s).finish(s, config, 100)
        assertTrue(done.records.isEmpty())
        assertFalse(done.lastResult!!.newRecord)
    }

    @Test fun tieKeepsOriginalSessionAndDate() {
        val a = session()
        val first = FlightProgress(profileId = "p", active = a).finish(a, config, 123)
        val b = session("two")
        val second = first.copy(active = b).finish(b, config, 999)
        assertEquals(first.records, second.records)
        assertFalse(second.lastResult!!.newRecord)
        assertTrue(second.lastResult!!.newBadges.isEmpty())
    }

    @Test fun abandonPreservesOldAchievementsWithoutNewEffects() {
        val s = session().copy(outcome = FlightOutcome.ABANDONED)
        val done = FlightProgress(profileId = "p", active = s).finish(s, config, 100)
        assertNull(done.active)
        assertTrue(done.records.isEmpty())
        assertTrue(done.pendingEffects.isEmpty())
    }

    @Test fun differentProfileCannotFinalize() {
        val s = session()
        assertThrows(IllegalArgumentException::class.java) {
            FlightProgress(profileId = "other", active = s).finish(s, config, 100)
        }
    }

    @Test fun trainingCannotProduceResult() {
        val s = session().copy(training = true)
        assertThrows(IllegalArgumentException::class.java) {
            FlightProgress(profileId = "p", active = s).finish(s, config, 100)
        }
    }
}
