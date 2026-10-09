package app.palavrascruzadas.core.gen

import app.palavrascruzadas.core.model.Puzzle
import app.palavrascruzadas.core.model.Entry
import app.palavrascruzadas.core.model.Pos
import app.palavrascruzadas.core.model.PuzzleKind

/**
 * Fit-it. Starting from a freeform grid without clues, reveals the longest
 * word and then letters wherever there is more than one solution, until the solution is unique.
 */
object FitIt {
    private const val CLUE_PLACEHOLDER = "Encaixe"
    private const val MAX_REVEALS = 200

    /** Fit-it puzzle with a unique solution, built from the [freeform] grid (each entry = one word). */
    fun build(freeform: Puzzle): Puzzle {
        val solution = freeform.solution
        val anchor = freeform.entries.maxBy { it.length }
        val givens = HashSet(anchor.cells)
        var reveals = 0
        while (true) {
            val found = Solver(freeform.entries, givens.associateWith { solution.getValue(it) }).solutions(limit = 2)
            if (found.size == 1) break
            check(found.size >= 2) { "Sem solução com as letras reveladas" }
            if (++reveals > MAX_REVEALS) throw GenerationException("Solution still not unique after $MAX_REVEALS letters")
            val a = found[0]
            val b = found[1]
            val differ = solution.keys.filter { letterAt(freeform.entries, a, it) != letterAt(freeform.entries, b, it) }
            check(differ.isNotEmpty()) { "Duas soluções iguais na grelha" }
            givens += differ.first()
        }
        return freeform.copy(
            kind = PuzzleKind.FIT_IT,
            entries = freeform.entries.map { it.copy(clue = CLUE_PLACEHOLDER, shortClue = null) },
            givens = givens,
        )
    }

    /** How many solutions the fit-it has with the letters already revealed (up to [limit]). Used in tests. */
    fun solutionCount(puzzle: Puzzle, limit: Int = 2): Int =
        Solver(puzzle.entries, puzzle.givens.associateWith { puzzle.solution.getValue(it) }).solutions(limit).size

    private fun letterAt(entries: List<Entry>, assignment: List<Int>, pos: Pos): Char? {
        entries.forEachIndexed { i, e ->
            val k = e.cells.indexOf(pos)
            if (k >= 0) return entries[assignment[i]].answer[k]
        }
        return null
    }

    /**
     * Assignment search: each entry gets one of the grid's words of the same length,
     * without repeats; letters must match the crossings and the revealed cells.
     * Returns, per entry, the index of the assigned word (up to [limit] solutions).
     */
    private class Solver(private val entries: List<Entry>, private val givens: Map<Pos, Char>) {
        private val words = entries.map { it.answer }
        private val order = entries.indices.sortedByDescending { entries[it].length }

        fun solutions(limit: Int): List<List<Int>> {
            val out = ArrayList<List<Int>>()
            val board = HashMap<Pos, Char>(givens)
            val assigned = IntArray(entries.size) { -1 }
            val used = BooleanArray(words.size)
            search(0, board, assigned, used, out, limit)
            return out
        }

        private fun search(
            depth: Int,
            board: HashMap<Pos, Char>,
            assigned: IntArray,
            used: BooleanArray,
            out: MutableList<List<Int>>,
            limit: Int,
        ) {
            if (out.size >= limit) return
            if (depth == order.size) {
                out += assigned.toList()
                return
            }
            val slot = order[depth]
            val cells = entries[slot].cells
            for (w in words.indices) {
                if (used[w] || words[w].length != cells.size) continue
                val placed = ArrayList<Pos>()
                var ok = true
                for ((k, pos) in cells.withIndex()) {
                    val letter = words[w][k]
                    val current = board[pos]
                    if (current == null) {
                        board[pos] = letter
                        placed += pos
                    } else if (current != letter) {
                        ok = false
                        break
                    }
                }
                if (ok) {
                    used[w] = true
                    assigned[slot] = w
                    search(depth + 1, board, assigned, used, out, limit)
                    used[w] = false
                    assigned[slot] = -1
                }
                placed.forEach { board.remove(it) }
                if (out.size >= limit) return
            }
        }
    }
}
