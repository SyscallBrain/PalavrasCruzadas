package app.palavrascruzadas.core.play

import app.palavrascruzadas.core.model.Dir
import app.palavrascruzadas.core.model.Pos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InputLogicTest {

    @Test
    fun `tapping a crossing cell selects the first direction, tapping again toggles`() {
        var s = InputLogic.select(sampleState(), origin)
        assertEquals(Cursor(origin, Dir.ACROSS), s.cursor)
        s = InputLogic.select(s, origin)
        assertEquals(Cursor(origin, Dir.DOWN), s.cursor)
    }

    @Test
    fun `tapping a single-direction cell uses that direction`() {
        val s = InputLogic.select(sampleState(), Pos(0, 2))
        assertEquals(Dir.ACROSS, s.cursor!!.dir)
    }

    @Test
    fun `typing fills the active cell and moves to the next empty cell`() {
        val s = InputLogic.select(sampleState(), origin).type('M')
        assertEquals('M', s.typed[origin])
        assertEquals(Cursor(Pos(0, 1), Dir.ACROSS), s.cursor)
    }

    @Test
    fun `finishing a word moves to the next pending entry`() {
        val s = InputLogic.select(sampleState(), origin).type('M', 'A', 'R')
        assertEquals(Cursor(Pos(2, 0), Dir.ACROSS), s.cursor)
    }

    @Test
    fun `backspace on an empty cell clears the previous letter`() {
        val s = InputLogic.select(sampleState(), origin).type('M', 'A')
        val back = InputLogic.backspace(s)
        assertNull(back.typed[Pos(0, 1)])
        assertEquals('M', back.typed[origin])
        assertEquals(Pos(0, 1), back.cursor!!.pos)
    }

    @Test
    fun `backspace does not erase locked cells`() {
        val locked = sampleState().copy(
            typed = mapOf(origin to 'M'),
            locked = setOf(origin),
            cursor = Cursor(origin, Dir.ACROSS),
        )
        assertEquals('M', InputLogic.backspace(locked).typed[origin])
    }

    @Test
    fun `next and previous entry cycle through pending entries`() {
        val s = InputLogic.select(sampleState(), origin)
        val order = InputLogic.orderedEntries(s)
        assertEquals(listOf(0, 2, 1), order) // across by number (MAR, SAL), then down (MESA)
        val next = InputLogic.nextEntry(s, 1)
        assertEquals(Dir.ACROSS, next.cursor!!.dir)
        assertEquals(Pos(2, 0), next.cursor!!.pos)
        val wrapped = InputLogic.nextEntry(s, -1)
        assertEquals(Dir.DOWN, wrapped.cursor!!.dir)
    }

    @Test
    fun `accented letter is stored in NFC uppercase`() {
        val s = InputLogic.select(sampleState(), origin).type('ã')
        assertEquals('Ã', s.typed[origin])
    }
}
