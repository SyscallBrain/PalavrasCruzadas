package app.palavrascruzadas.core.play

import app.palavrascruzadas.core.model.Difficulty
import app.palavrascruzadas.core.model.Dir
import app.palavrascruzadas.core.model.Entry
import app.palavrascruzadas.core.model.Pos
import app.palavrascruzadas.core.model.Puzzle
import app.palavrascruzadas.core.model.PuzzleKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayFixesTest {

    private fun accented(): Puzzle = Puzzle(
        id = "t-accent",
        kind = PuzzleKind.CLASSIC,
        width = 3,
        height = 1,
        entries = listOf(Entry(1, 0, 0, Dir.ACROSS, "MÃE", "Progenitora")),
        keyword = null,
        givens = emptySet(),
        theme = null,
        difficulty = Difficulty.EASY,
        targetSeconds = 60,
    )

    @Test
    fun `unaccented letters complete the grid when the option is on`() {
        val start = PlayState.start(accented(), PlayOptions(acceptUnaccented = true))
        val done = Checker.markFound(start.type('M', 'A', 'E'))
        assertEquals('Ã', done.typed[Pos(0, 1)])
        assertTrue(done.isComplete)
    }

    @Test
    fun `unaccented letters stay wrong when the option is off`() {
        val done = Checker.markFound(PlayState.start(accented()).type('M', 'A', 'E'))
        assertEquals('A', done.typed[Pos(0, 1)])
        assertFalse(done.isComplete)
    }

    @Test
    fun `givens start revealed and complete the anchor word`() {
        val puzzle = samplePuzzle().copy(id = "t-fit", kind = PuzzleKind.FIT_IT, givens = setOf(Pos(0, 0), Pos(0, 1), Pos(0, 2)))
        val s = PlayState.start(puzzle)
        assertTrue(Pos(0, 1) in s.revealed)
        assertTrue(0 in s.found)
        assertEquals(0, s.hintPoints)
        // The cursor goes to the first unsolved entry.
        assertNotEquals(0, InputLogic.activeEntry(s))
    }

    @Test
    fun `restoring an old save brings back the givens`() {
        val puzzle = samplePuzzle().copy(id = "t-fit", kind = PuzzleKind.FIT_IT, givens = setOf(Pos(0, 0)))
        val old = SavedGame.capture(sampleState().copy(puzzle = puzzle), null, 10, 0L).copy(revealed = emptyList())
        assertTrue(Pos(0, 0) in old.restore(puzzle, PlayOptions()).revealed)
    }

    @Test
    fun `anagram is shown for the active entry once and survives a save`() {
        val s = InputLogic.select(sampleState(), origin)
        val once = Checker.showAnagram(s)
        assertEquals(setOf(0), once.anagrams)
        assertEquals(1, once.hintPoints)
        assertSame(once, Checker.showAnagram(once))
        val back = SavedGame.capture(once, null, 0, 0L).restore(once.puzzle, PlayOptions())
        assertEquals(setOf(0), back.anagrams)
    }

    @Test
    fun `anagram has the answer letters in another order and is stable`() {
        val s = sampleState()
        val a = Checker.anagramOf(s, 1)
        assertEquals("MESA".toList().sorted(), a.toList().sorted())
        assertNotEquals("MESA", a)
        assertEquals(a, Checker.anagramOf(s, 1))
    }

    @Test
    fun `typing the last empty cell wraps to an empty cell earlier in the word`() {
        // M _ R: R is typed first, then back to M; the middle cell stays empty.
        var s = InputLogic.select(sampleState(), Pos(0, 2)).type('R')
        s = InputLogic.select(s, origin)
        s = s.copy(cursor = Cursor(origin, Dir.ACROSS)).type('M')
        assertEquals(Cursor(Pos(0, 1), Dir.ACROSS), s.cursor)
    }

    @Test
    fun `next entry from a finished entry goes to its neighbour`() {
        // MAR solved; from it, › goes to SAL (3 across) and not to the first unsolved entry.
        val done = Checker.markFound(InputLogic.select(sampleState(), origin).type('M', 'A', 'R'))
        val onMar = done.copy(cursor = Cursor(origin, Dir.ACROSS))
        assertEquals(2, InputLogic.activeEntry(InputLogic.nextEntry(onMar, 1)))
        assertEquals(1, InputLogic.activeEntry(InputLogic.nextEntry(onMar, -1)))
    }
}
