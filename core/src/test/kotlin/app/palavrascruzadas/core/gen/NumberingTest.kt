package app.palavrascruzadas.core.gen

import app.palavrascruzadas.core.model.Pos
import org.junit.Assert.assertEquals
import org.junit.Test

class NumberingTest {

    @Test
    fun `numbers start cells in reading order`() {
        // Across at (0,0)-(0,2) and another at (2,0)-(2,1).
        val filled = setOf(Pos(0, 0), Pos(0, 1), Pos(0, 2), Pos(2, 0), Pos(2, 1))
        val numbers = Numbering.assign(3, 3, filled)
        assertEquals(mapOf(Pos(0, 0) to 1, Pos(2, 0) to 2), numbers)
    }

    @Test
    fun `a cell starting both words gets one number`() {
        // (0,0) starts both an across and a down word: a single number.
        val filled = setOf(Pos(0, 0), Pos(0, 1), Pos(1, 0))
        val numbers = Numbering.assign(3, 3, filled)
        assertEquals(mapOf(Pos(0, 0) to 1), numbers)
    }

    @Test
    fun `single isolated cell gets no number`() {
        assertEquals(emptyMap<Pos, Int>(), Numbering.assign(3, 3, setOf(Pos(1, 1))))
    }

    @Test
    fun `cells on the grid edge count as empty outside`() {
        val filled = setOf(Pos(0, 0), Pos(1, 0))
        assertEquals(true, Numbering.startsDown(Pos(0, 0), filled))
        assertEquals(false, Numbering.startsAcross(Pos(0, 0), filled))
    }
}
