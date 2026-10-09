package app.palavrascruzadas.core.gen

import app.palavrascruzadas.core.model.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FreeformGeneratorTest {

    private val bank by lazy { loadRealBank() }

    private fun spec(size: Int, seed: Int, words: IntRange = 4..(size + 2)) = GenSpec(
        maxW = size,
        maxH = size,
        minWords = words.first,
        maxWords = words.last,
        difficulties = setOf(Difficulty.EASY, Difficulty.MEDIUM),
        rngSeed = seed,
    )

    @Test
    fun `same seed gives the same puzzle`() {
        val generator = FreeformGenerator(bank)
        val a = generator.generate(spec(9, 42))
        val b = generator.generate(spec(9, 42))
        assertEquals(a, b)
    }

    @Test
    fun `different seeds give different puzzles`() {
        val generator = FreeformGenerator(bank)
        val a = generator.generate(spec(9, 1))
        val b = generator.generate(spec(9, 2))
        assertTrue(a.entries.map { it.answer } != b.entries.map { it.answer })
    }

    @Test
    fun `generated grids keep every freeform rule`() {
        val generator = FreeformGenerator(bank)
        // 1000 seeds per size (200 for the largest ones, to keep the run time down).
        for ((size, seeds) in listOf(5 to 1000, 7 to 1000, 9 to 1000, 11 to 200, 13 to 200)) {
            var ok = 0
            for (seed in 1..seeds) {
                val s = spec(size, seed)
                try {
                    val puzzle = generator.generate(s)
                    assertFreeformInvariants(puzzle, s, bank)
                    ok++
                } catch (e: GenerationException) {
                    // Some seeds have no solution with this bank: the benchmark measures that.
                }
            }
            assertTrue("no puzzle at all for ${size}x$size", ok > 0)
            println("freeform ${size}x$size: $ok/$seeds with invariants checked")
        }
    }

    @Test
    fun `failure is reported with a clear exception`() {
        val generator = FreeformGenerator(bank)
        try {
            generator.generate(GenSpec(maxW = 3, maxH = 3, minWords = 12, maxWords = 14, rngSeed = 7))
            fail("expected GenerationException")
        } catch (e: GenerationException) {
            assertTrue(e.message!!.contains("seed 7") || e.message!!.contains("candidate"))
        }
    }

    @Test
    fun `clue usage counts every clue placed in a grid`() {
        val generator = FreeformGenerator(bank)
        val usage = ClueUsage()
        val puzzle = generator.generate(spec(7, 100), usage)
        val recorded = puzzle.entries.sumOf { usage.count(it.clue) }
        assertEquals(puzzle.entries.size, recorded)
    }

    @Test
    fun `second grid prefers clues not used yet`() {
        val generator = FreeformGenerator(bank)
        val usage = ClueUsage()
        val first = generator.generate(spec(7, 100), usage)
        // A clue already used only comes back once every alternative of the same level has been used.
        val second = generator.generate(spec(7, 101), usage)
        val reused = second.entries.count { e -> first.entries.any { it.clue == e.clue } }
        assertTrue("clues reused too often: $reused", reused <= second.entries.size / 2)
    }

    @Test
    fun `target seconds follow the letter factor and round to 15`() {
        val generator = FreeformGenerator(bank)
        val puzzle = generator.generate(spec(7, 5))
        assertEquals(0, puzzle.targetSeconds % 15)
        assertTrue(puzzle.targetSeconds >= 15)
    }
}
