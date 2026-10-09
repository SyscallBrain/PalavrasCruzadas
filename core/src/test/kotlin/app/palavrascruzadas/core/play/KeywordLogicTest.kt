package app.palavrascruzadas.core.play

import app.palavrascruzadas.core.gen.FreeformGenerator
import app.palavrascruzadas.core.gen.GenSpec
import app.palavrascruzadas.core.gen.KeywordPlacer
import app.palavrascruzadas.core.gen.loadRealBank
import app.palavrascruzadas.core.model.Difficulty
import app.palavrascruzadas.core.model.PuzzleKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeywordLogicTest {
    private fun keywordState(): PlayState {
        val bank = loadRealBank()
        val base = FreeformGenerator(bank).generate(
            GenSpec(maxW = 9, maxH = 9, minWords = 8, maxWords = 11, difficulties = setOf(Difficulty.EASY, Difficulty.MEDIUM), rngSeed = 2),
        )
        val candidates = bank.entries.map { it.answer }.filter { it.length in 5..8 }.sorted()
        val puzzle = KeywordPlacer.place(base, candidates)
        assertTrue(puzzle.kind == PuzzleKind.KEYWORD)
        return PlayState(puzzle)
    }

    @Test
    fun rightGuessMarksTheKeywordAndWrongGuessDoesNotPenalise() {
        val state = keywordState()
        val word = state.puzzle.keyword!!.word
        val (afterWrong, rightWrong) = KeywordLogic.guess(state, word.reversed())
        assertFalse(rightWrong)
        assertFalse(afterWrong.keywordGuessed)
        val (afterRight, right) = KeywordLogic.guess(afterWrong, word.lowercase())
        assertTrue(right)
        assertTrue(afterRight.keywordGuessed)
    }

    @Test
    fun wrongLengthIsNotAGuess() {
        val state = keywordState()
        val word = state.puzzle.keyword!!.word
        val (_, ok) = KeywordLogic.guess(state, word + "A")
        assertFalse(ok)
    }

    @Test
    fun slotsShowOnlyCorrectCells() {
        val state = keywordState()
        val keyword = state.puzzle.keyword!!
        assertTrue(KeywordLogic.slots(state).all { it == null })
        val first = keyword.cells.first()
        val filled = state.copy(typed = mapOf(first to state.solution.getValue(first)))
        assertEquals(state.solution.getValue(first), KeywordLogic.slots(filled).first())
    }

    @Test
    fun indexesAreOneBasedInWordOrder() {
        val state = keywordState()
        val indexes = KeywordLogic.indexes(state.puzzle)
        assertEquals(state.puzzle.keyword!!.word.length, indexes.size)
        assertEquals(1, indexes.getValue(state.puzzle.keyword!!.cells.first()))
    }
}
