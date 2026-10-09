package app.palavrascruzadas.core.play

import app.palavrascruzadas.core.model.Pos
import app.palavrascruzadas.core.model.Puzzle
import app.palavrascruzadas.core.text.Normalize

/** Hidden keyword: its cells, the index of each one and the guess. */
object KeywordLogic {

    /** Index (1, 2, …) of each keyword cell, in letter order. */
    fun indexes(puzzle: Puzzle): Map<Pos, Int> =
        puzzle.keyword?.cells?.mapIndexed { i, pos -> pos to i + 1 }?.toMap().orEmpty()

    /** Letter of each keyword cell, or null while the cell is not correct. */
    fun slots(state: PlayState): List<Char?> =
        state.puzzle.keyword?.cells.orEmpty().map { pos ->
            if (state.isCorrect(pos)) state.solution.getValue(pos) else null
        }

    /**
     * Guesses the keyword. A right guess marks it as guessed (the +1 hint is granted elsewhere).
     * A wrong guess has no penalty. Returns the state and whether it was right.
     */
    fun guess(state: PlayState, text: String): Pair<PlayState, Boolean> {
        val keyword = state.puzzle.keyword ?: return state to false
        if (state.keywordGuessed) return state to true
        val typed = Normalize.answer(text)
        val right = typed.length == keyword.word.length &&
            Normalize.matches(typed, keyword.word, state.options.acceptUnaccented)
        return if (right) state.copy(keywordGuessed = true) to true else state to false
    }
}
