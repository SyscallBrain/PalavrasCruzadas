package app.palavrascruzadas.core.gen

import app.palavrascruzadas.core.model.Difficulty
import app.palavrascruzadas.core.model.PuzzleKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeywordPlacerTest {
    private val bank = loadRealBank()

    @Test
    fun keywordCellsAreDistinctMatchLettersAndAvoidShortWords() {
        val generator = FreeformGenerator(bank)
        val candidates = bank.entries.map { it.answer }.filter { it.length in 5..8 }.sorted()
        for (seed in 1..5) {
            val spec = GenSpec(maxW = 9, maxH = 9, minWords = 8, maxWords = 11, difficulties = setOf(Difficulty.EASY, Difficulty.MEDIUM), rngSeed = seed)
            val base = generator.generate(spec)
            val placed = KeywordPlacer.place(base, candidates)
            assertEquals(PuzzleKind.KEYWORD, placed.kind)
            assertNotNull(placed.keyword)
            val keyword = placed.keyword!!
            assertEquals(keyword.word.length, keyword.cells.size)
            assertEquals(keyword.cells.size, keyword.cells.toSet().size)
            keyword.word.toList().zip(keyword.cells).forEach { (letter, cell) ->
                assertEquals("letter $letter at $cell", letter, placed.solution.getValue(cell))
            }
            val threeLetterCells = placed.entries.filter { it.length == 3 }.flatMap { it.cells }.toSet()
            assertTrue("keyword uses a 3-letter cell", keyword.cells.none { it in threeLetterCells })
            assertTrue("keyword repeats a grid answer", placed.entries.none { it.answer == keyword.word })
        }
    }

    @Test
    fun returnsPuzzleUnchangedWhenNoCandidateFits() {
        val generator = FreeformGenerator(bank)
        val spec = GenSpec(maxW = 5, maxH = 5, minWords = 4, maxWords = 6, difficulties = setOf(Difficulty.EASY), rngSeed = 1)
        val base = generator.generate(spec)
        val placed = KeywordPlacer.place(base, listOf("ABC", "ABCDEFGHIJKLMN"))
        assertEquals(base, placed)
    }
}
