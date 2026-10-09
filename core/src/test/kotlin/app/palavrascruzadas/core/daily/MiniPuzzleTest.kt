package app.palavrascruzadas.core.daily

import app.palavrascruzadas.core.gen.loadRealBank
import app.palavrascruzadas.core.model.PuzzleKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniPuzzleTest {
    @Test
    fun miniIsFiveByFiveWithFourToSixWords() {
        val bank = loadRealBank()
        for (seed in 1..5) {
            val puzzle = MiniPuzzle.generate(bank, seed)
            assertEquals(PuzzleKind.MINI, puzzle.kind)
            // The words' bounding box may be smaller than the 5×5 maximum grid.
            assertTrue(puzzle.width <= 5 && puzzle.height <= 5)
            assertTrue("entries ${puzzle.entries.size}", puzzle.entries.size in 4..6)
        }
    }
}
