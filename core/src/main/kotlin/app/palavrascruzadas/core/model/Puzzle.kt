package app.palavrascruzadas.core.model

import app.palavrascruzadas.core.text.Alphabet
import app.palavrascruzadas.core.text.Normalize

/**
 * A complete, consistent puzzle. Construction fails with [IllegalArgumentException]
 * if entries fall outside the grid, crossings have different letters, letters are outside
 * the alphabet or the keyword does not match the solution.
 */
data class Puzzle(
    val id: String,
    val kind: PuzzleKind,
    val width: Int,
    val height: Int,
    val entries: List<Entry>,
    val keyword: Keyword?,
    val givens: Set<Pos>,
    val theme: String?,
    val difficulty: Difficulty,
    val targetSeconds: Int,
    /** Diretas clue cells: not part of the solution; the arrow tells which way the entry starts. */
    val clueCells: List<ClueCell> = emptyList(),
) {
    /** Correct letter of each filled cell, computed from the entries. */
    val solution: Map<Pos, Char> = solutionOf(entries)

    init {
        validate()
    }

    private fun validate() {
        require(id.isNotBlank()) { "Puzzle without id" }
        require(width in 1..MAX_SIDE && height in 1..MAX_SIDE) {
            "Invalid size ${width}x$height in $id (max $MAX_SIDE)"
        }
        require(entries.isNotEmpty()) { "Puzzle $id has no entries" }
        require(targetSeconds > 0) { "Puzzle $id has non-positive targetSeconds" }

        solution.keys.forEach { p ->
            require(inside(p)) { "Cell $p is outside the ${width}x$height grid in $id" }
        }
        givens.forEach { p ->
            require(inside(p)) { "Given $p is outside the grid in $id" }
            require(p in solution) { "Given $p is not part of any entry in $id" }
        }
        keyword?.let { k ->
            require(k.word.isNotEmpty()) { "Empty keyword in $id" }
            require(k.cells.size == k.word.length) {
                "Keyword ${k.word} has ${k.cells.size} cells, expected ${k.word.length} in $id"
            }
            require(k.cells.toSet().size == k.cells.size) { "Keyword cells repeat in $id" }
            k.cells.forEachIndexed { i, p ->
                require(solution[p] == k.word[i]) {
                    "Keyword ${k.word}: letter ${i + 1} at $p is ${solution[p]}, expected ${k.word[i]} in $id"
                }
            }
        }
    }

    private fun inside(p: Pos): Boolean = p.row in 0 until height && p.col in 0 until width

    init {
        val clueAt = HashMap<Pos, Set<Dir>>()
        clueCells.forEach { c ->
            require(inside(c.pos)) { "Clue cell ${c.pos} is outside the grid in $id" }
            require(c.pos !in solution) { "Clue cell ${c.pos} is part of an entry in $id" }
            require(c.arrows.isNotEmpty()) { "Clue cell ${c.pos} has no arrows in $id" }
            require(clueAt.put(c.pos, c.arrows) == null) { "Clue cell ${c.pos} repeats in $id" }
            c.arrows.forEach { dir ->
                val target = if (dir == Dir.ACROSS) Pos(c.pos.row, c.pos.col + 1) else Pos(c.pos.row + 1, c.pos.col)
                require(entries.any { it.dir == dir && Pos(it.row, it.col) == target }) {
                    "Clue cell ${c.pos} points $dir but no entry starts at $target in $id"
                }
            }
        }
        if (kind == PuzzleKind.DIRETAS) {
            entries.forEach { e ->
                val before = if (e.dir == Dir.ACROSS) Pos(e.row, e.col - 1) else Pos(e.row - 1, e.col)
                require(clueAt[before]?.contains(e.dir) == true) {
                    "Entry '${e.answer}' has no clue cell before it in $id"
                }
            }
        }
    }

    companion object {
        const val MAX_SIDE = 30

        private fun solutionOf(entries: List<Entry>): Map<Pos, Char> {
            val out = HashMap<Pos, Char>()
            val starts = HashSet<Pair<Pos, Dir>>()
            entries.forEach { e ->
                require(e.answer.length >= 2) { "Answer too short: '${e.answer}'" }
                require(Alphabet.isValidAnswer(e.answer)) { "Characters outside the alphabet in '${e.answer}'" }
                require(Normalize.answer(e.answer) == e.answer) { "'${e.answer}' is not NFC uppercase" }
                require(e.clue.isNotBlank()) { "Empty clue for '${e.answer}'" }
                require(starts.add(Pair(Pos(e.row, e.col), e.dir))) {
                    "Duplicate entry '${e.answer}' at ${e.row},${e.col} ${e.dir}"
                }
                e.cells.forEachIndexed { i, p ->
                    val letter = e.answer[i]
                    val previous = out.put(p, letter)
                    require(previous == null || previous == letter) {
                        "Crossing mismatch at $p: '$previous' vs '$letter' (entry '${e.answer}')"
                    }
                }
            }
            return out
        }
    }
}

/** Diretas clue cell: [arrows] are the directions of the entries that start right after it. */
data class ClueCell(val pos: Pos, val arrows: Set<Dir>)
