package app.palavrascruzadas.core.play

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeAttackTest {
    @Test
    fun startsWithThreeMinutesAndTicksDown() {
        val start = TimeAttack()
        assertEquals(180, start.timeLeft)
        assertEquals(179, start.tick().timeLeft)
    }

    @Test
    fun wordsAddFiveSecondsEachAndGridsFifteen() {
        val t = TimeAttack().wordsFound(2).gridDone()
        assertEquals(2, t.words)
        assertEquals(1, t.grids)
        assertEquals(180 + 10 + 15, t.timeLeft)
    }

    @Test
    fun endsAtZeroAndNeverGoesNegative() {
        var t = TimeAttack(timeLeft = 1)
        t = t.tick().tick()
        assertEquals(0, t.timeLeft)
        assertTrue(t.isOver)
    }

    @Test
    fun noNewWordsChangesNothing() {
        val t = TimeAttack()
        assertEquals(t, t.wordsFound(0))
    }
}
