package app.palavrascruzadas.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PuzzleTest {

    // MAR (across at 0,0), MESA (down at 0,0), SAL (across at 2,0).
    // Cells: (0,0)M (0,1)A (0,2)R (1,0)E (2,0)S (3,0)A (2,1)A (2,2)L
    private val mar = Entry(1, 0, 0, Dir.ACROSS, "MAR", "Mamífero marinho ou ponto de partida")
    private val mesa = Entry(1, 0, 0, Dir.DOWN, "MESA", "Móvel para comer")
    private val sal = Entry(3, 2, 0, Dir.ACROSS, "SAL", "Tempero branco")

    private fun puzzle(
        entries: List<Entry> = listOf(mar, mesa, sal),
        keyword: Keyword? = null,
        givens: Set<Pos> = emptySet(),
        width: Int = 5,
        height: Int = 5,
    ) = Puzzle(
        id = "t-1",
        kind = PuzzleKind.CLASSIC,
        width = width,
        height = height,
        entries = entries,
        keyword = keyword,
        givens = givens,
        theme = null,
        difficulty = Difficulty.EASY,
        targetSeconds = 60,
    )

    @Test
    fun `coherent puzzle builds and exposes its solution`() {
        val p = puzzle()
        assertEquals(8, p.solution.size)
        assertEquals('M', p.solution[Pos(0, 0)])
        assertEquals('S', p.solution[Pos(2, 0)])
        assertEquals('L', p.solution[Pos(2, 2)])
    }

    @Test
    fun `crossing with different letters is rejected`() {
        // MOLA goes down with L at (2,0), but SAL needs S.
        val mola = mesa.copy(answer = "MOLA", clue = "Pista")
        val e = assertThrows(IllegalArgumentException::class.java) { puzzle(listOf(mar, mola, sal)) }
        assertEquals(true, e.message!!.contains("Crossing mismatch"))
    }

    @Test
    fun `entry outside the grid is rejected`() {
        val outside = mar.copy(col = 4) // 4,5,6 in a 5-column grid
        val e = assertThrows(IllegalArgumentException::class.java) { puzzle(listOf(outside)) }
        assertEquals(true, e.message!!.contains("outside"))
    }

    @Test
    fun `letters outside the alphabet are rejected`() {
        val hifen = mar.copy(answer = "M-R")
        assertThrows(IllegalArgumentException::class.java) { puzzle(listOf(hifen)) }
    }

    @Test
    fun `answer not in NFC uppercase is rejected`() {
        val nfd = mar.copy(answer = "MÃR") // Ã decomposto
        assertThrows(IllegalArgumentException::class.java) { puzzle(listOf(nfd)) }
    }

    @Test
    fun `duplicate entry is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { puzzle(listOf(mar, mar)) }
    }

    @Test
    fun `keyword that matches the solution is accepted`() {
        val kw = Keyword("MAR", listOf(Pos(0, 0), Pos(0, 1), Pos(0, 2)))
        assertEquals("MAR", puzzle(keyword = kw).keyword!!.word)
    }

    @Test
    fun `keyword that does not match the solution is rejected`() {
        val kw = Keyword("MOR", listOf(Pos(0, 0), Pos(0, 1), Pos(0, 2)))
        assertThrows(IllegalArgumentException::class.java) { puzzle(keyword = kw) }
    }

    @Test
    fun `given outside any entry is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { puzzle(givens = setOf(Pos(4, 4))) }
    }

    @Test
    fun `difficulty codes map to enum values`() {
        assertEquals(Difficulty.MEDIUM, Difficulty.fromCode(2))
        assertThrows(IllegalArgumentException::class.java) { Difficulty.fromCode(9) }
    }
}
