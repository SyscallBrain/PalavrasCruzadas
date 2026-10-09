package app.palavrascruzadas.core.gen

import app.palavrascruzadas.core.model.Keyword
import app.palavrascruzadas.core.model.Pos
import app.palavrascruzadas.core.model.Puzzle
import app.palavrascruzadas.core.model.PuzzleKind
import kotlin.math.abs

/**
 * Hidden keyword. Picks a 5–8 letter answer whose letters
 * can be assigned to distinct grid cells, preferring spread-out cells and avoiding
 * cells of 3-letter words.
 */
object KeywordPlacer {
    private const val MIN_LEN = 5
    private const val MAX_LEN = 8

    /** Search node limit per candidate: avoids blow-ups on dense grids. */
    private const val NODE_LIMIT = 20_000

    /**
     * Returns the puzzle with the first keyword from [candidates] (in the given order) that fits,
     * or the puzzle unchanged if none fits. Candidates outside 5–8 letters or
     * that are already answers in the grid are skipped.
     */
    fun place(puzzle: Puzzle, candidates: List<String>): Puzzle {
        val answers = puzzle.entries.map { it.answer }.toSet()
        val blocked = puzzle.entries.filter { it.length == 3 }.flatMap { it.cells }.toSet()
        val free = puzzle.solution.keys.filter { it !in blocked }
        for (word in candidates) {
            if (word.length !in MIN_LEN..MAX_LEN || word in answers) continue
            val cells = assign(word, puzzle.solution, free) ?: continue
            return puzzle.copy(kind = PuzzleKind.KEYWORD, keyword = Keyword(word, cells))
        }
        return puzzle
    }

    /** Cells for each letter of the word, or null if there is no distinct assignment. */
    private fun assign(word: String, solution: Map<Pos, Char>, free: List<Pos>): List<Pos>? {
        val options = word.map { letter -> free.filter { solution[it] == letter } }
        if (options.any { it.isEmpty() }) return null
        // Start with the letters that have the fewest cells: the search fails sooner.
        val order = word.indices.sortedBy { options[it].size }
        val chosen = arrayOfNulls<Pos>(word.length)
        var nodes = 0

        fun search(depth: Int): Boolean {
            if (depth == order.size) return true
            if (++nodes > NODE_LIMIT) return false
            val index = order[depth]
            val placed = chosen.filterNotNull()
            // Most spread out first: the cell farthest from the ones already chosen.
            val ranked = options[index]
                .filter { it !in placed }
                .sortedByDescending { p -> placed.minOfOrNull { distance(it, p) } ?: 0 }
            for (p in ranked) {
                chosen[index] = p
                if (search(depth + 1)) return true
                chosen[index] = null
            }
            return false
        }

        return if (search(0)) chosen.map { it!! } else null
    }

    private fun distance(a: Pos, b: Pos): Int = abs(a.row - b.row) + abs(a.col - b.col)
}
