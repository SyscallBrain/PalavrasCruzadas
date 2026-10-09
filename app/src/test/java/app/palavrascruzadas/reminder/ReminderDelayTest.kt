package app.palavrascruzadas.reminder

import java.time.Duration
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderDelayTest {
    @Test
    fun laterTodayWhenTheHourHasNotPassed() {
        val now = LocalDateTime.of(2026, 10, 9, 18, 30)
        assertEquals(Duration.ofMinutes(90), ReminderScheduler.delayUntilHour(now, 20))
    }

    @Test
    fun tomorrowWhenTheHourHasAlreadyPassed() {
        val now = LocalDateTime.of(2026, 10, 9, 21, 0)
        assertEquals(Duration.ofHours(23), ReminderScheduler.delayUntilHour(now, 20))
    }

    @Test
    fun exactlyOnTheHourGoesToTomorrow() {
        val now = LocalDateTime.of(2026, 10, 9, 20, 0)
        assertEquals(Duration.ofHours(24), ReminderScheduler.delayUntilHour(now, 20))
    }
}
