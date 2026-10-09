package app.palavrascruzadas.core.play

import app.palavrascruzadas.core.gen.FreeformGenerator
import app.palavrascruzadas.core.gen.GenSpec
import app.palavrascruzadas.core.gen.loadRealBank
import app.palavrascruzadas.core.model.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WheelLogicTest {
    private fun state(): PlayState {
        val puzzle = FreeformGenerator(loadRealBank()).generate(
            GenSpec(maxW = 7, maxH = 7, minWords = 4, maxWords = 6, difficulties = setOf(Difficulty.EASY, Difficulty.MEDIUM), rngSeed = 4),
        )
        return PlayState(puzzle)
    }

    @Test
    fun gridWordFillsItsEntryAndLocksIt() {
        val s = state()
        val word = s.puzzle.entries.first().answer
        val r = WheelLogic.submit(s, word, bonusWords = emptySet(), bonusFound = emptySet())
        assertEquals(WheelLogic.Outcome.GRID, r.outcome)
        assertTrue(0 in r.state.found)
        assertEquals(word.length, r.state.typed.size.coerceAtMost(word.length))
    }

    @Test
    fun wordAlreadyFoundIsNotCountedTwice() {
        val s = state()
        val word = s.puzzle.entries.first().answer
        val once = WheelLogic.submit(s, word, emptySet(), emptySet()).state
        assertEquals(WheelLogic.Outcome.ALREADY, WheelLogic.submit(once, word, emptySet(), emptySet()).outcome)
    }

    @Test
    fun bonusWordIsCountedOnceAndNothingElseChanges() {
        val s = state()
        val r = WheelLogic.submit(s, "BONUS", bonusWords = setOf("BONUS"), bonusFound = emptySet())
        assertEquals(WheelLogic.Outcome.BONUS, r.outcome)
        assertEquals(setOf("BONUS"), r.bonusFound)
        assertEquals(s, r.state)
        assertEquals(WheelLogic.Outcome.ALREADY, WheelLogic.submit(s, "BONUS", setOf("BONUS"), setOf("BONUS")).outcome)
    }

    @Test
    fun unknownWordDoesNothing() {
        val s = state()
        assertEquals(WheelLogic.Outcome.NONE, WheelLogic.submit(s, "XYZXYZ", emptySet(), emptySet()).outcome)
    }
}
