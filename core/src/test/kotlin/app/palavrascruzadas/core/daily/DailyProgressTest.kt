package app.palavrascruzadas.core.daily

import app.palavrascruzadas.core.play.HintWallet
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyProgressTest {
    private val monday = LocalDate.of(2027, 1, 4)

    @Test
    fun streakCountsConsecutiveDaysAndRestartsAfterAGap() {
        var p = DailyProgress()
        var w = HintWallet(0)
        for (offset in 0..2L) {
            val (next, wallet) = p.complete(monday.plusDays(offset), 3, w)
            p = next
            w = wallet
        }
        assertEquals(3, p.streak)
        val (afterGap, _) = p.complete(monday.plusDays(5), 2, w)
        assertEquals(1, afterGap.streak)
        assertEquals(3, afterGap.maxStreak)
    }

    @Test
    fun streakShowsZeroAfterTwoMissedDays() {
        val (p, _) = DailyProgress().complete(monday, 3, HintWallet(0))
        assertEquals(1, p.displayedStreak(monday))
        assertEquals(1, p.displayedStreak(monday.plusDays(1)))
        assertEquals(0, p.displayedStreak(monday.plusDays(2)))
    }

    @Test
    fun dailyRewardIsGivenOncePerDay() {
        val (p, w1) = DailyProgress().complete(monday, 3, HintWallet(10))
        assertEquals(12, w1.balance)
        val (_, w2) = p.complete(monday, 1, w1)
        assertEquals(12, w2.balance)
    }

    @Test
    fun repeatingADayKeepsTheBestStars() {
        val (p, w) = DailyProgress().complete(monday, 1, HintWallet(10))
        val (again, _) = p.complete(monday, 3, w)
        assertEquals(3, again.done.getValue(monday.toString()))
        assertTrue(again.isDone(monday))
        assertFalse(again.isDone(monday.plusDays(1)))
    }

    @Test
    fun sevenDayStreakGivesFiveHints() {
        var p = DailyProgress()
        var w = HintWallet(0)
        for (offset in 0..6L) {
            val (next, wallet) = p.complete(monday.plusDays(offset), 3, w)
            p = next
            w = wallet
        }
        // 7 days × 2 daily reward + 5 for the streak.
        assertEquals(7 * 2 + 5, w.balance)
    }
}
