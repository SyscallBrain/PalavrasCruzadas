package app.palavrascruzadas.core.gen

import app.palavrascruzadas.core.model.Puzzle

/**
 * Stable text representation of a puzzle, used by the test snapshots
 * (see `src/test/resources`).
 */
fun Puzzle.toSnapshot(): String = buildString {
    appendLine("# ${id} ${width}x$height ${kind} ${difficulty} ${targetSeconds}s")
    entries.forEach { e ->
        appendLine("${e.number} ${e.dir} ${e.row},${e.col} ${e.answer} | ${e.clue}")
    }
}
