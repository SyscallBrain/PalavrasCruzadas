package app.palavrascruzadas.core.play

import app.palavrascruzadas.core.model.Dir
import app.palavrascruzadas.core.model.Pos
import app.palavrascruzadas.core.model.Puzzle
import kotlinx.serialization.Serializable

/** A letter typed in a cell. Stored as text so the JSON does not depend on `Char`. */
@Serializable
data class SavedLetter(val row: Int, val col: Int, val letter: String)

@Serializable
data class SavedPos(val row: Int, val col: Int)

@Serializable
data class SavedCursor(val row: Int, val col: Int, val dir: String)

/**
 * A game in progress. Only stores what the player did; the puzzle
 * is loaded again from the level files by [puzzleId]. Options are the current settings.
 */
@Serializable
data class SavedGame(
    val puzzleId: String,
    val levelNumber: Int?,
    val typed: List<SavedLetter>,
    val revealed: List<SavedPos>,
    val locked: List<SavedPos>,
    val found: List<Int>,
    val cursor: SavedCursor?,
    val letterHints: Int,
    val anagramHints: Int,
    val wordReveals: Int,
    val gridChecks: Int,
    val wordErrorChecks: Int,
    val elapsedSeconds: Int,
    val updatedAt: Long,
    val keywordGuessed: Boolean = false,
    val anagrams: List<Int> = emptyList(),
) {

    /** Rebuilds the [PlayState] on top of the given puzzle. Fails if it is not the saved puzzle. */
    fun restore(puzzle: Puzzle, options: PlayOptions): PlayState {
        require(puzzle.id == puzzleId) { "Saved game is for $puzzleId, not ${puzzle.id}" }
        return PlayState(
            puzzle = puzzle,
            typed = typed.associate { Pos(it.row, it.col) to it.letter.single() },
            // The puzzle's given cells are always added (games saved by older versions did not have them).
            revealed = revealed.map { Pos(it.row, it.col) }.toSet() + puzzle.givens,
            locked = locked.map { Pos(it.row, it.col) }.toSet(),
            found = found.toSet(),
            cursor = cursor?.let { Cursor(Pos(it.row, it.col), Dir.valueOf(it.dir)) },
            letterHints = letterHints,
            anagramHints = anagramHints,
            wordReveals = wordReveals,
            gridChecks = gridChecks,
            wordErrorChecks = wordErrorChecks,
            keywordGuessed = keywordGuessed,
            anagrams = anagrams.toSet(),
            options = options,
        )
    }

    companion object {
        /** Snapshot of a game in progress. [updatedAt] is the instant in milliseconds (injected for tests). */
        fun capture(state: PlayState, levelNumber: Int?, elapsedSeconds: Int, updatedAt: Long): SavedGame = SavedGame(
            puzzleId = state.puzzle.id,
            levelNumber = levelNumber,
            typed = state.typed.map { (pos, letter) -> SavedLetter(pos.row, pos.col, letter.toString()) }.sortedWith(compareBy({ it.row }, { it.col })),
            revealed = state.revealed.map { SavedPos(it.row, it.col) }.sortedWith(compareBy({ it.row }, { it.col })),
            locked = state.locked.map { SavedPos(it.row, it.col) }.sortedWith(compareBy({ it.row }, { it.col })),
            found = state.found.sorted(),
            cursor = state.cursor?.let { SavedCursor(it.pos.row, it.pos.col, it.dir.name) },
            letterHints = state.letterHints,
            anagramHints = state.anagramHints,
            wordReveals = state.wordReveals,
            gridChecks = state.gridChecks,
            wordErrorChecks = state.wordErrorChecks,
            elapsedSeconds = elapsedSeconds,
            updatedAt = updatedAt,
            keywordGuessed = state.keywordGuessed,
            anagrams = state.anagrams.sorted(),
        )
    }
}
