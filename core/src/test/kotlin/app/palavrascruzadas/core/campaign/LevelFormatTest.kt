package app.palavrascruzadas.core.campaign

import app.palavrascruzadas.core.model.PuzzleKind
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class LevelFormatTest {
    private fun world(n: Int): WorldLevels {
        val file = File("../app/src/main/assets/levels/world_%02d.json".format(n))
        return LevelFormat.parseWorld(file.readText(Charsets.UTF_8))
    }

    @Test
    fun worldOneHasFortyLevelsNumberedOneToForty() {
        val w = world(1)
        assertEquals(1, w.number)
        assertEquals("geral", w.theme)
        assertEquals((1..40).toList(), w.levels.map { it.number })
        assertEquals((1..40).toList(), w.levels.map { it.position })
    }

    @Test
    fun worldTwoNumbersStartAtFortyOne() {
        assertEquals(41, world(2).levels.first().number)
    }

    @Test
    fun typesFollowThePositionAndKeywordLevelsHaveAKeyword() {
        val w = world(1)
        assertEquals(LevelType.KEYWORD, w.levels[9].type)
        assertEquals(PuzzleKind.KEYWORD, w.levels[9].puzzle.kind)
        assertEquals(true, w.levels[9].puzzle.keyword != null)
        assertEquals(LevelType.BOSS, w.levels[39].type)
        assertEquals(PuzzleKind.BOSS, w.levels[39].puzzle.kind)
    }

    @Test
    fun wheelDiretasAndFitItLevelsAreRealKindsNotPlaceholders() {
        val w = world(1)
        assertEquals(PuzzleKind.WHEEL, w.levels[4].puzzle.kind)
        assertEquals(PuzzleKind.DIRETAS, w.levels[19].puzzle.kind)
        assertEquals(PuzzleKind.FIT_IT, w.levels[24].puzzle.kind)
        assertEquals(false, w.levels.any { it.todoF9 })
    }

    @Test
    fun everyLevelParsesIntoAValidPuzzle() {
        for (n in 1..15) {
            val w = world(n)
            w.levels.forEach { level ->
                assertEquals("mundo $n posição ${level.position}", level.puzzle.entries.size > 0, true)
            }
        }
    }
}
