package github.detrig.feature.week.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class WeekStateTest {
    @Test fun sevenDaysFormOneWeek() {
        assertEquals(1L, WeekState(1).weekNumber)
        assertEquals(1, WeekState(1).dayOfWeek)
        assertEquals(7, WeekState(1).daysUntilAllowance)
        assertEquals(1L, WeekState(7).weekNumber)
        assertEquals(7, WeekState(7).dayOfWeek)
        assertEquals(1, WeekState(7).daysUntilAllowance)
        assertEquals(2L, WeekState(8).weekNumber)
        assertEquals(1, WeekState(8).dayOfWeek)
        assertEquals(3L, WeekState(15).weekNumber)
    }

    @Test fun dayMustBePositive() {
        assertThrows(IllegalArgumentException::class.java) { WeekState(0) }
    }
}
