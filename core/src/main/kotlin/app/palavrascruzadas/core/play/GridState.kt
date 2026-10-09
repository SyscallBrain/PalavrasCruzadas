package app.palavrascruzadas.core.play

import app.palavrascruzadas.core.model.Cell
import app.palavrascruzadas.core.model.Pos
import app.palavrascruzadas.core.model.Puzzle

/**
 * Immutable view of a puzzle's grid: cells, correct letters and the entries crossing them.
 * It does not store what the player typed (that lives in [PlayState]).
 */
class GridState(val puzzle: Puzzle) {
    val width: Int get() = puzzle.width
    val height: Int get() = puzzle.height

    val cells: Map<Pos, Cell> = buildCells()

    /** Cells where two entries cross. */
    val crossings: Set<Pos> = cells.values.filter { it.isCrossing }.map { it.pos }.toSet()

    fun cellAt(pos: Pos): Cell? = cells[pos]

    private fun buildCells(): Map<Pos, Cell> {
        val membership = HashMap<Pos, MutableList<Int>>()
        puzzle.entries.forEachIndexed { index, entry ->
            entry.cells.forEach { p -> membership.getOrPut(p) { mutableListOf() }.add(index) }
        }
        return membership.mapValues { (pos, indexes) ->
            Cell(pos, puzzle.solution.getValue(pos), indexes.toList())
        }
    }
}
