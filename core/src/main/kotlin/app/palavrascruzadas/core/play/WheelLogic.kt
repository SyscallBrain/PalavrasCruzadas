package app.palavrascruzadas.core.play

import app.palavrascruzadas.core.model.Pos

/**
 * Letter wheel: a word formed with the wheel's letters. If it is an unsolved entry
 * of the grid, it fills it; if it is a bonus word, it counts it; otherwise nothing happens.
 */
object WheelLogic {

    enum class Outcome { GRID, BONUS, ALREADY, NONE }

    data class Result(val state: PlayState, val bonusFound: Set<String>, val outcome: Outcome)

    fun submit(state: PlayState, word: String, bonusWords: Set<String>, bonusFound: Set<String>): Result {
        val entry = state.puzzle.entries.indexOfFirst { it.answer == word }
        if (entry >= 0) {
            if (entry in state.found) return Result(state, bonusFound, Outcome.ALREADY)
            val cells = state.puzzle.entries[entry].cells
            val letters = cells.mapIndexed { k, pos -> pos to word[k] }.toMap()
            val next = state.copy(
                typed = state.typed + letters,
                locked = state.locked + cells.toSet(),
                found = state.found + entry,
            )
            return Result(next, bonusFound, Outcome.GRID)
        }
        if (word in bonusWords) {
            return if (word in bonusFound) Result(state, bonusFound, Outcome.ALREADY)
            else Result(state, bonusFound + word, Outcome.BONUS)
        }
        return Result(state, bonusFound, Outcome.NONE)
    }

    /** Cells of the formed word, for drawing (the state does not change). */
    fun cellsOf(state: PlayState, word: String): List<Pos> =
        state.puzzle.entries.firstOrNull { it.answer == word }?.cells.orEmpty()
}
