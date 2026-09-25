package github.detrig.core.time

import org.junit.Assert.assertEquals
import org.junit.Test

class ElapsedIntervalsTest {
    @Test fun keepsRemainderAcrossChecks() {
        val first = elapsedIntervals(1_000, 9_500, 4_000)
        assertEquals(2, first.count)
        assertEquals(9_000, first.checkpointMillis)
        assertEquals(1, elapsedIntervals(first.checkpointMillis, 13_000, 4_000).count)
    }

    @Test fun initializesMissingCheckpointAndIgnoresClockRollback() {
        assertEquals(ElapsedIntervals(0, 10_000), elapsedIntervals(0, 10_000, 4_000))
        assertEquals(ElapsedIntervals(0, 10_000), elapsedIntervals(10_000, 5_000, 4_000))
    }
}
