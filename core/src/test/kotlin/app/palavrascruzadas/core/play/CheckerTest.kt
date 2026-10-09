package app.palavrascruzadas.core.play

import app.palavrascruzadas.core.model.Pos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckerTest {

    @Test
    fun `completed correct word is confirmed and locked`() {
        val s = Checker.markFound(InputLogic.select(sampleState(), origin).type('M', 'A', 'R'))
        assertEquals(setOf(0), s.found)
        assertEquals(setOf(Pos(0, 0), Pos(0, 1), Pos(0, 2)), s.locked)
    }

    @Test
    fun `without confirm option words are not locked`() {
        val s = Checker.markFound(
            InputLogic.select(sampleState(PlayOptions(confirmWords = false)), origin).type('M', 'A', 'R'),
        )
        assertEquals(setOf(0), s.found)
        assertTrue(s.locked.isEmpty())
    }

    @Test
    fun `verify word reports wrong letters and counts one error check`() {
        val typed = InputLogic.select(InputLogic.select(sampleState(), origin).type('M', 'O', 'R'), origin)
        val (s, wrong) = Checker.verifyWord(typed)
        assertEquals(listOf(Pos(0, 1)), wrong)
        assertEquals(1, s.wordErrorChecks)
    }

    @Test
    fun `verify word without errors costs nothing for stars`() {
        val typed = InputLogic.select(InputLogic.select(sampleState(), origin).type('M', 'A', 'R'), origin)
        val (s, wrong) = Checker.verifyWord(typed)
        assertTrue(wrong.isEmpty())
        assertEquals(0, s.hintPoints)
    }

    @Test
    fun `verify grid costs one hint and lists every wrong cell`() {
        val typed = InputLogic.select(sampleState(), origin).type('M', 'O')
        val (s, wrong) = Checker.verifyGrid(typed)
        assertEquals(1, s.gridChecks)
        assertTrue(Pos(0, 1) in wrong)
    }

    @Test
    fun `reveal letter counts as a hint and makes the cell correct`() {
        val s = Checker.revealLetter(InputLogic.select(sampleState(), origin))
        assertEquals(1, s.letterHints)
        assertTrue(s.isCorrect(origin))
    }

    @Test
    fun `reveal word weighs two hint points and completes the word`() {
        val s = Checker.revealWord(InputLogic.select(sampleState(), origin))
        assertEquals(1, s.wordReveals)
        assertEquals(2, s.hintPoints)
        assertEquals(setOf(0), s.found)
    }

    @Test
    fun `complete grid is detected`() {
        var s = sampleState()
        s = s.copy(typed = s.solution.toMap())
        assertTrue(s.isComplete)
    }
}
