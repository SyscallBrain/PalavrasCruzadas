package app.palavrascruzadas.core.gen

import app.palavrascruzadas.core.model.Pos

/**
 * Reading-order numbering: every cell that starts an across or a down word gets the next number.
 * [filled] are the occupied cells; outside the grid counts as empty.
 */
object Numbering {
    fun assign(width: Int, height: Int, filled: Set<Pos>): Map<Pos, Int> {
        val out = LinkedHashMap<Pos, Int>()
        var next = 0
        for (row in 0 until height) {
            for (col in 0 until width) {
                val p = Pos(row, col)
                if (p in filled && (startsAcross(p, filled) || startsDown(p, filled))) {
                    out[p] = ++next
                }
            }
        }
        return out
    }

    /** Starts an across word: the cell to the left is empty and the one to the right is filled. */
    fun startsAcross(p: Pos, filled: Set<Pos>): Boolean =
        Pos(p.row, p.col - 1) !in filled && Pos(p.row, p.col + 1) in filled

    /** Starts a down word: the cell above is empty and the one below is filled. */
    fun startsDown(p: Pos, filled: Set<Pos>): Boolean =
        Pos(p.row - 1, p.col) !in filled && Pos(p.row + 1, p.col) in filled
}
