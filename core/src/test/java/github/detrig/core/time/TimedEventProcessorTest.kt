package github.detrig.core.time

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class TimedEventProcessorTest {
    @Test fun sharesOneInstantAcrossIndependentTasks() = runBlocking {
        var clockReads = 0
        val instants = mutableListOf<Long>()
        val processor = TimedEventProcessor(
            clock = WallClock { clockReads++; 123_456L },
            tasks = listOf(
                TimeDrivenTask { instants += it },
                TimeDrivenTask { instants += it },
            ),
        )

        processor.reconcile()

        assertEquals(1, clockReads)
        assertEquals(listOf(123_456L, 123_456L), instants)
    }
}
