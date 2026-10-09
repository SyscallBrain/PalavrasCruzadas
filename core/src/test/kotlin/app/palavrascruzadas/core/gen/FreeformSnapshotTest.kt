package app.palavrascruzadas.core.gen

import app.palavrascruzadas.core.model.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Regression: the five reference grids (seeds 1–5, 9×9) must not change
 * without the change being reviewed. To update them, run
 * `levelgen/build/install/levelgen/bin/levelgen snapshot > core/src/test/resources/freeform-snapshot.txt`.
 */
class FreeformSnapshotTest {

    @Test
    fun `five reference grids match the snapshot`() {
        val generator = FreeformGenerator(loadRealBank())
        val actual = (1..5).joinToString("\n") { seed ->
            val spec = GenSpec(maxW = 9, maxH = 9, minWords = 8, maxWords = 11, difficulties = setOf(Difficulty.EASY, Difficulty.MEDIUM), rngSeed = seed)
            generator.generate(spec).toSnapshot()
        }
        val expected = javaClass.getResourceAsStream("/freeform-snapshot.txt")!!.bufferedReader(Charsets.UTF_8).readText()
        assertEquals(expected.trimEnd(), actual.trimEnd())
    }
}
