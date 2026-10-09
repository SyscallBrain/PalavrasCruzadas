package app.palavrascruzadas.core.play

import app.palavrascruzadas.core.model.Dir
import app.palavrascruzadas.core.model.Entry
import app.palavrascruzadas.core.model.Pos
import app.palavrascruzadas.core.text.Normalize

/**
 * Cursor and typing rules. Pure functions over [PlayState].
 */
object InputLogic {

    /** Indices of the entries going through [pos], in entry order. */
    fun entriesAt(state: PlayState, pos: Pos): List<Int> =
        state.puzzle.entries.withIndex().filter { (_, e) -> pos in e.cells }.map { it.index }

    /**
     * Tap on a cell: selects that cell's entry. If the cell has only one direction, uses it.
     * If it is already the active cell and has both directions, toggles across/down.
     */
    fun select(state: PlayState, pos: Pos): PlayState {
        val available = entriesAt(state, pos).map { state.puzzle.entries[it].dir }.distinct()
        if (available.isEmpty()) return state
        val current = state.cursor
        val dir = when {
            current != null && current.pos == pos && available.size > 1 -> if (current.dir == Dir.ACROSS) Dir.DOWN else Dir.ACROSS
            current != null && current.pos == pos -> current.dir
            current != null && current.dir in available -> current.dir
            else -> available.first()
        }
        return state.copy(cursor = Cursor(pos, dir))
    }

    /** Index of the active entry (the one through the cursor in the cursor's direction), or null. */
    fun activeEntry(state: PlayState): Int? {
        val cursor = state.cursor ?: return null
        return state.puzzle.entries.indexOfFirst { it.dir == cursor.dir && cursor.pos in it.cells }.takeIf { it >= 0 }
    }

    /** Types a letter in the active cell and moves on. Locked and revealed cells do not change. */
    fun type(state: PlayState, letter: Char): PlayState {
        val cursor = state.cursor ?: return state
        val pos = cursor.pos
        if (pos in state.locked || pos in state.revealed) return advance(state)
        val normalized = Normalize.answer(letter.toString()).firstOrNull() ?: return state
        // "Accept letters without accents": A in an Ã cell counts as correct and the cell shows Ã.
        val expected = state.solution[pos]
        val stored = if (state.options.acceptUnaccented && expected != null &&
            Normalize.base(normalized) == Normalize.base(expected)
        ) expected else normalized
        val updated = state.copy(typed = state.typed + (pos to stored))
        return advance(updated)
    }

    /**
     * Backspace: if the active cell has a letter and is not locked, clears it; otherwise moves back one cell
     * in the entry and clears that one.
     */
    fun backspace(state: PlayState): PlayState {
        val cursor = state.cursor ?: return state
        if (cursor.pos !in state.locked && cursor.pos !in state.revealed && state.typed.containsKey(cursor.pos)) {
            return state.copy(typed = state.typed - cursor.pos)
        }
        val entry = activeEntry(state)?.let { state.puzzle.entries[it] } ?: return state
        val index = entry.cells.indexOf(cursor.pos)
        if (index <= 0) return state
        val previous = entry.cells[index - 1]
        if (previous in state.locked || previous in state.revealed) return state.copy(cursor = Cursor(previous, cursor.dir))
        return state.copy(typed = state.typed - previous, cursor = Cursor(previous, cursor.dir))
    }

    /** ‹ › arrows of the clue bar: previous or next unsolved entry. */
    fun nextEntry(state: PlayState, step: Int): PlayState {
        if (orderedEntries(state).isEmpty()) return state
        // Walks all entries in clue-bar order and stops at the next unsolved one, so that
        // from a solved entry ‹ › go to its neighbour and not back to the first one.
        val all = state.puzzle.entries.indices
            .sortedWith(compareBy({ state.puzzle.entries[it].dir.ordinal }, { state.puzzle.entries[it].number }))
        val start = activeEntry(state)?.let { all.indexOf(it) } ?: (if (step > 0) -1 else 0)
        var k = start
        repeat(all.size) {
            k = ((k + step) % all.size + all.size) % all.size
            if (all[k] !in state.found) return focusEntry(state, all[k])
        }
        return state
    }

    /** Entries in order: across by number, then down. Skips the ones already solved. */
    fun orderedEntries(state: PlayState): List<Int> =
        state.puzzle.entries.indices
            .filter { it !in state.found }
            .sortedWith(compareBy({ state.puzzle.entries[it].dir.ordinal }, { state.puzzle.entries[it].number }))

    /** Puts the cursor at the start of the entry, or on its first empty cell. */
    fun focusEntry(state: PlayState, index: Int): PlayState {
        val entry = state.puzzle.entries[index]
        val pos = firstOpenCell(state, entry)
        return state.copy(cursor = Cursor(pos, entry.dir))
    }

    private fun advance(state: PlayState): PlayState {
        val cursor = state.cursor ?: return state
        val index = activeEntry(state) ?: return state
        val entry = state.puzzle.entries[index]
        val from = entry.cells.indexOf(cursor.pos)
        val next = if (state.options.skipFilled) {
            // After the current cell; if there is none, wrap to the empty cells at the start of the word.
            entry.cells.drop(from + 1).firstOrNull { !isFilled(state, it) }
                ?: entry.cells.take(from).firstOrNull { !isFilled(state, it) }
        } else {
            entry.cells.getOrNull(from + 1)
        }
        if (next != null) return state.copy(cursor = Cursor(next, cursor.dir))
        // End of the entry: move to the next unsolved entry, if any.
        val pending = orderedEntries(state).filter { it != index }
        if (pending.isEmpty()) return state.copy(cursor = Cursor(cursor.pos, cursor.dir))
        val after = pending.firstOrNull { state.puzzle.entries[it].dir == cursor.dir && state.puzzle.entries[it].number > entry.number }
            ?: pending.first()
        return focusEntry(state, after)
    }

    private fun firstOpenCell(state: PlayState, entry: Entry): Pos =
        entry.cells.firstOrNull { !isFilled(state, it) } ?: entry.cells.first()

    private fun isFilled(state: PlayState, pos: Pos): Boolean =
        state.typed.containsKey(pos) || pos in state.revealed
}
