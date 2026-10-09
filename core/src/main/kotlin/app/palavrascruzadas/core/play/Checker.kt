package app.palavrascruzadas.core.play

import app.palavrascruzadas.core.model.Pos
import app.palavrascruzadas.core.rng.Mulberry32
import app.palavrascruzadas.core.text.Normalize

/**
 * Checking, word confirmation and hints.
 * Each action returns a new [PlayState]; the hint counters are updated here.
 */
object Checker {

    /** Cell with a typed letter that does not match the solution (empty cells do not count). */
    fun isWrong(state: PlayState, pos: Pos): Boolean {
        val typed = state.typed[pos] ?: return false
        if (state.isCorrect(pos)) return false
        if (!state.options.acceptUnaccented) return true
        return !Normalize.matches(typed.toString(), state.solution.getValue(pos).toString(), lenient = true)
    }

    /**
     * Confirms complete, correct words: coloured and locked if [PlayOptions.confirmWords].
     * Without confirmation, correct words are not locked (the game only ends when the whole grid is right).
     */
    fun markFound(state: PlayState): PlayState {
        var next = state
        state.puzzle.entries.indices.forEach { index ->
            if (index in next.found) return@forEach
            val cells = next.puzzle.entries[index].cells
            if (cells.all { next.isCorrect(it) }) {
                next = next.copy(
                    found = next.found + index,
                    locked = if (next.options.confirmWords) next.locked + cells else next.locked,
                )
            }
        }
        return next
    }

    /** Check word (free): returns the wrong cells of the active entry. Counts towards the stars if there are errors. */
    fun verifyWord(state: PlayState): Pair<PlayState, List<Pos>> {
        val index = InputLogic.activeEntry(state) ?: return state to emptyList()
        val wrong = state.puzzle.entries[index].cells.filter { isWrong(state, it) }
        val next = if (wrong.isEmpty()) state else state.copy(wordErrorChecks = state.wordErrorChecks + 1)
        return next to wrong
    }

    /** Check grid: costs 1 hint. Returns the wrong cells of the whole grid. */
    fun verifyGrid(state: PlayState): Pair<PlayState, List<Pos>> {
        val wrong = state.solution.keys.filter { isWrong(state, it) }.sortedWith(compareBy({ it.row }, { it.col }))
        return state.copy(gridChecks = state.gridChecks + 1) to wrong
    }

    /** Reveal letter (active cell). Counts as a hint for the stars. */
    fun revealLetter(state: PlayState): PlayState {
        val pos = state.cursor?.pos ?: return state
        if (state.isCorrect(pos)) return state
        return markFound(
            state.copy(
                revealed = state.revealed + pos,
                typed = state.typed - pos,
                letterHints = state.letterHints + 1,
            ),
        )
    }

    /** Reveal word (active entry). Counts with weight 2 for the stars. */
    fun revealWord(state: PlayState): PlayState {
        val index = InputLogic.activeEntry(state) ?: return state
        val cells = state.puzzle.entries[index].cells.filter { !state.isCorrect(it) }
        if (cells.isEmpty()) return state
        return markFound(
            state.copy(
                revealed = state.revealed + cells,
                typed = state.typed - cells.toSet(),
                wordReveals = state.wordReveals + 1,
            ),
        )
    }

    /**
     * Anagram of the active entry's answer, shown in the clue bar until the word is solved
     * (counts as a hint). No effect if the entry is already solved or its anagram is already shown.
     */
    fun showAnagram(state: PlayState): PlayState {
        val index = InputLogic.activeEntry(state) ?: return state
        if (index in state.found || index in state.anagrams) return state
        return state.copy(anagramHints = state.anagramHints + 1, anagrams = state.anagrams + index)
    }

    /**
     * The answer's letters shuffled, always in the same order for the same puzzle and entry.
     * Avoids returning the answer itself when another order exists.
     */
    fun anagramOf(state: PlayState, index: Int): String {
        val answer = state.puzzle.entries[index].answer
        val rng = Mulberry32(state.puzzle.id.hashCode() * 31 + index)
        repeat(8) {
            val letters = answer.toMutableList()
            for (i in letters.indices.reversed()) {
                val j = rng.nextInt(i + 1)
                val tmp = letters[i]; letters[i] = letters[j]; letters[j] = tmp
            }
            val candidate = letters.joinToString("")
            if (candidate != answer) return candidate
        }
        return answer.reversed()
    }
}
