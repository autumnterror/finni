package github.detrig.feature.gamesession.presentation.formatter

import org.junit.Assert.assertEquals
import org.junit.Test

class GameSessionFormattersTest {

    @Test
    fun allowanceCountdownShowsCurrentMoment() {
        assertEquals("сейчас", 1_000L.toAllowanceCountdownText(nowMillis = 1_000L))
    }

    @Test
    fun allowanceCountdownShowsHoursAndDays() {
        val nowMillis = 1_000L

        assertEquals(
            "через 3 ч",
            (nowMillis + 3L * 60 * 60 * 1_000).toAllowanceCountdownText(nowMillis),
        )
        assertEquals(
            "через 2 дня",
            (nowMillis + 2L * 24 * 60 * 60 * 1_000).toAllowanceCountdownText(nowMillis),
        )
    }
}
