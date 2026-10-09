package app.palavrascruzadas.core.play

import app.palavrascruzadas.core.model.Difficulty
import app.palavrascruzadas.core.model.Dir
import app.palavrascruzadas.core.model.Entry
import app.palavrascruzadas.core.model.Pos
import app.palavrascruzadas.core.model.Puzzle
import app.palavrascruzadas.core.model.PuzzleKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GridStateTest {

    private val puzzle = Puzzle(
        id = "t-grid",
        kind = PuzzleKind.CLASSIC,
        width = 5,
        height = 5,
        entries = listOf(
            Entry(1, 0, 0, Dir.ACROSS, "MAR", "Pista A"),
            Entry(1, 0, 0, Dir.DOWN, "MESA", "Pista B"),
            Entry(3, 2, 0, Dir.ACROSS, "SAL", "Pista C"),
        ),
        keyword = null,
        givens = emptySet(),
        theme = null,
        difficulty = Difficulty.EASY,
        targetSeconds = 60,
    )

    @Test
    fun `cells list the entries that cross them`() {
        val grid = GridState(puzzle)
        assertEquals(setOf(Pos(0, 0), Pos(2, 0)), grid.crossings)
        assertEquals(listOf(0, 1), grid.cellAt(Pos(0, 0))!!.entries)
        assertEquals('S', grid.cellAt(Pos(2, 0))!!.letter)
    }

    @Test
    fun `non crossing cell belongs to one entry`() {
        val cell = GridState(puzzle).cellAt(Pos(0, 2))!!
        assertEquals(listOf(0), cell.entries)
        assertTrue(!cell.isCrossing)
    }

    @Test
    fun `empty cell is absent`() {
        assertEquals(null, GridState(puzzle).cellAt(Pos(4, 4)))
    }
}
