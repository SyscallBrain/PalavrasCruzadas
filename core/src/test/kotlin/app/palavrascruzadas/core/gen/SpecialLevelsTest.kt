package app.palavrascruzadas.core.gen

import app.palavrascruzadas.core.model.Difficulty
import app.palavrascruzadas.core.model.Dir
import app.palavrascruzadas.core.model.PuzzleKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Diretas, letter wheel and fit-it. */
class SpecialLevelsTest {
    private val bank = loadRealBank()

    @Test
    fun diretasHaveAClueCellBeforeEveryEntryAndShortClues() {
        val generator = DiretasGenerator(bank)
        var made = 0
        for (seed in 1..12) {
            val spec = GenSpec(maxW = 7, maxH = 7, minWords = 6, maxWords = 9, difficulties = setOf(Difficulty.EASY, Difficulty.MEDIUM), rngSeed = seed)
            val puzzle = try {
                generator.generate(spec)
            } catch (e: GenerationException) {
                continue
            }
            made++
            assertEquals(PuzzleKind.DIRETAS, puzzle.kind)
            assertTrue(puzzle.clueCells.isNotEmpty())
            puzzle.entries.forEach { e ->
                assertTrue("pista curta longa: ${e.clue}", e.clue.length <= 14)
                val before = if (e.dir == Dir.ACROSS) app.palavrascruzadas.core.model.Pos(e.row, e.col - 1) else app.palavrascruzadas.core.model.Pos(e.row - 1, e.col)
                assertTrue(puzzle.clueCells.any { it.pos == before && e.dir in it.arrows })
            }
            val density = (puzzle.solution.size + puzzle.clueCells.size).toDouble() / (puzzle.width * puzzle.height)
            assertTrue("densidade $density", density >= DiretasGenerator.MIN_DENSITY)
        }
        assertTrue("poucas grelhas de diretas: $made", made >= 6)
    }

    @Test
    fun wheelGridHasTheBaseAndFiveOrMoreSubwords() {
        val generator = WheelGenerator(bank)
        var made = 0
        for (seed in 1..6) {
            val wheel = try {
                generator.generate(seed, baseLength = 6, difficulties = setOf(Difficulty.EASY, Difficulty.MEDIUM))
            } catch (e: GenerationException) {
                continue
            }
            made++
            assertEquals(PuzzleKind.WHEEL, wheel.puzzle.kind)
            assertTrue(wheel.puzzle.entries.any { it.answer == wheel.base })
            assertTrue(wheel.puzzle.entries.size in 4..8)
            assertEquals(wheel.base.toList().sorted(), wheel.letters.sorted())
            assertTrue("subpalavras ${wheel.puzzle.entries.size + wheel.bonus.size}", wheel.puzzle.entries.size + wheel.bonus.size >= WheelGenerator.MIN_SUBWORDS)
        }
        assertTrue("poucas rodas: $made", made >= 3)
    }

    @Test
    fun fitItPuzzlesHaveASingleSolutionAfterReveals() {
        val generator = FreeformGenerator(bank)
        var made = 0
        for (seed in 1..15) {
            val freeform = try {
                generator.generate(GenSpec(maxW = 6, maxH = 6, minWords = 4, maxWords = 6, difficulties = setOf(Difficulty.EASY, Difficulty.MEDIUM), rngSeed = seed))
            } catch (e: GenerationException) {
                continue
            }
            val puzzle = FitIt.build(freeform)
            made++
            assertEquals(PuzzleKind.FIT_IT, puzzle.kind)
            assertEquals("semente $seed", 1, FitIt.solutionCount(puzzle))
        }
        assertTrue("poucos encaixar: $made", made >= 8)
    }
}
