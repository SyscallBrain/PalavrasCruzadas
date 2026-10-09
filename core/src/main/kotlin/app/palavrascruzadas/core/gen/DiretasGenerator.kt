package app.palavrascruzadas.core.gen

import app.palavrascruzadas.core.bank.ClueBank
import app.palavrascruzadas.core.model.ClueCell
import app.palavrascruzadas.core.model.Dir
import app.palavrascruzadas.core.model.Entry
import app.palavrascruzadas.core.model.Pos
import app.palavrascruzadas.core.model.Puzzle
import app.palavrascruzadas.core.model.PuzzleKind

/**
 * Diretas (arrow-word): a freeform grid where the cell before the start of each word
 * is a clue cell with an arrow. Uses the short clue only. Grids from 5×5 to 9×9, clue cells included.
 */
class DiretasGenerator(private val bank: ClueBank) {

    /** Generates the diretas grid. Throws [GenerationException] if no seed gives a dense enough grid. */
    fun generate(spec: GenSpec, usage: ClueUsage = ClueUsage()): Puzzle {
        require(spec.maxW <= MAX_FREEFORM && spec.maxH <= MAX_FREEFORM) { "Diretas: freeform side at most $MAX_FREEFORM" }
        val freeform = FreeformGenerator(bank).generate(spec.copy(requireShortClue = true), usage)
        return withClueCells(freeform, spec)
    }

    /** Adds a row on top and a column on the left; every word start gets its own clue cell. */
    private fun withClueCells(freeform: Puzzle, spec: GenSpec): Puzzle {
        val entries = freeform.entries.map { e ->
            Entry(
                number = e.number,
                row = e.row + 1,
                col = e.col + 1,
                dir = e.dir,
                answer = e.answer,
                clue = e.clue,
                shortClue = null,
            )
        }
        val width = freeform.width + 1
        val height = freeform.height + 1
        val solutionCells = entries.flatMap { it.cells }.toSet()
        val arrows = HashMap<Pos, MutableSet<Dir>>()
        entries.forEach { e ->
            val before = if (e.dir == Dir.ACROSS) Pos(e.row, e.col - 1) else Pos(e.row - 1, e.col)
            if (before in solutionCells) throw GenerationException("Entry without a free cell before it: ${e.answer}")
            arrows.getOrPut(before) { mutableSetOf() }.add(e.dir)
        }
        val clueCells = arrows.map { (pos, dirs) -> ClueCell(pos, dirs) }
        val density = (solutionCells.size + clueCells.size).toDouble() / (width * height)
        if (density < MIN_DENSITY) throw GenerationException("Density ${"%.2f".format(density)} below $MIN_DENSITY")
        return Puzzle(
            id = freeform.id,
            kind = PuzzleKind.DIRETAS,
            width = width,
            height = height,
            entries = entries,
            keyword = null,
            givens = emptySet(),
            theme = spec.themes?.singleOrNull(),
            difficulty = freeform.difficulty,
            targetSeconds = freeform.targetSeconds,
            clueCells = clueCells,
        )
    }

    companion object {
        const val MAX_FREEFORM = 8
        const val MIN_DENSITY = 0.55
    }
}
