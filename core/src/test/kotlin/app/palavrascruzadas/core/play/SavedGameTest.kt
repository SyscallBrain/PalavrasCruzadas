package app.palavrascruzadas.core.play

import app.palavrascruzadas.core.gen.FreeformGenerator
import app.palavrascruzadas.core.gen.GenSpec
import app.palavrascruzadas.core.gen.loadRealBank
import app.palavrascruzadas.core.model.Difficulty
import app.palavrascruzadas.core.model.Pos
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class SavedGameTest {
    private fun puzzle() = FreeformGenerator(loadRealBank()).generate(
        GenSpec(maxW = 7, maxH = 7, minWords = 6, maxWords = 8, difficulties = setOf(Difficulty.EASY, Difficulty.MEDIUM), rngSeed = 3),
    ).copy(id = "c001")

    @Test
    fun restoreGivesBackTheSameStateAfterAJsonRoundTrip() {
        val p = puzzle()
        val first = p.solution.entries.first()
        val state = PlayState(
            puzzle = p,
            typed = mapOf(first.key to first.value),
            revealed = setOf(first.key),
            letterHints = 1,
            wordErrorChecks = 2,
            cursor = Cursor(first.key, p.entries.first().dir),
            found = setOf(0),
        )
        val saved = SavedGame.capture(state, levelNumber = 1, elapsedSeconds = 42, updatedAt = 1_000L)
        val json = Json.encodeToString(saved)
        val loaded = Json.decodeFromString<SavedGame>(json)
        val restored = loaded.restore(p, PlayOptions())
        assertEquals(state.typed, restored.typed)
        assertEquals(state.revealed, restored.revealed)
        assertEquals(state.found, restored.found)
        assertEquals(state.cursor, restored.cursor)
        assertEquals(state.letterHints, restored.letterHints)
        assertEquals(state.wordErrorChecks, restored.wordErrorChecks)
        assertEquals(42, loaded.elapsedSeconds)
    }

    @Test(expected = IllegalArgumentException::class)
    fun restoreRejectsAnotherPuzzle() {
        val saved = SavedGame.capture(PlayState(puzzle()), levelNumber = 1, elapsedSeconds = 0, updatedAt = 0L)
        saved.restore(puzzle().copy(id = "c999"), PlayOptions())
    }

    @Test
    fun positionsAreStoredInOrder() {
        val p = puzzle()
        val cells = p.solution.keys.take(3).sortedWith(compareByDescending<Pos> { it.row }.thenByDescending { it.col })
        val state = PlayState(puzzle = p, typed = cells.associateWith { 'A' })
        val saved = SavedGame.capture(state, null, 0, 0L)
        assertEquals(cells.sortedWith(compareBy({ it.row }, { it.col })).map { it.row }, saved.typed.map { it.row })
    }
}
