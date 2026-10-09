package app.palavrascruzadas.core.gen

import app.palavrascruzadas.core.bank.ClueBank
import app.palavrascruzadas.core.model.Dir
import app.palavrascruzadas.core.model.Pos
import app.palavrascruzadas.core.model.Puzzle
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/** Real bank compiled by tools/compile_bank.py (the tests' working dir is core/). */
internal fun loadRealBank(): ClueBank {
    val file = File("../app/src/main/assets/clues/pt-PT.json")
    assertTrue("Compiled bank missing: ${file.absolutePath}", file.exists())
    return ClueBank.parse(file.readText(Charsets.UTF_8))
}

/** Checks the freeform properties on a generated puzzle. */
internal fun assertFreeformInvariants(puzzle: Puzzle, spec: GenSpec, bank: ClueBank) {
    val filled = puzzle.entries.flatMap { it.cells }.toSet()
    assertTrue("width ${puzzle.width} > ${spec.maxW}", puzzle.width <= spec.maxW)
    assertTrue("height ${puzzle.height} > ${spec.maxH}", puzzle.height <= spec.maxH)
    assertTrue("too few words", puzzle.entries.size >= spec.minWords)
    assertTrue("too many words", puzzle.entries.size <= spec.maxWords)

    // Every across/down run of 2+ letters must be exactly one entry.
    assertRunsMatch(puzzle, filled, Dir.ACROSS)
    assertRunsMatch(puzzle, filled, Dir.DOWN)

    // Reading-order numbering.
    assertEquals(Numbering.assign(puzzle.width, puzzle.height, filled).let { numbers ->
        puzzle.entries.map { numbers.getValue(Pos(it.row, it.col)) }.sorted()
    }, puzzle.entries.map { it.number }.sorted())

    // Distinct answers and lemmas.
    assertEquals(puzzle.entries.size, puzzle.entries.map { it.answer }.toSet().size)
    val lemmas = puzzle.entries.map { bank.entry(it.answer)!!.lemma }
    assertEquals(lemmas.size, lemmas.toSet().size)

    // Connected: crossing entries form a single group.
    assertConnected(puzzle)
}

private fun assertRunsMatch(puzzle: Puzzle, filled: Set<Pos>, dir: Dir) {
    val expected = puzzle.entries.filter { it.dir == dir }.associate { it.cells.first() to it.length }
    var found = 0
    for (a in 0 until (if (dir == Dir.ACROSS) puzzle.height else puzzle.width)) {
        var b = 0
        val limit = if (dir == Dir.ACROSS) puzzle.width else puzzle.height
        while (b < limit) {
            val p = if (dir == Dir.ACROSS) Pos(a, b) else Pos(b, a)
            val before = if (dir == Dir.ACROSS) Pos(a, b - 1) else Pos(b - 1, a)
            if (p in filled && before !in filled) {
                var e = b
                while (e < limit && (if (dir == Dir.ACROSS) Pos(a, e) else Pos(e, a)) in filled) e++
                if (e - b >= 2) {
                    assertEquals("run at $p is not an entry", expected[p], e - b)
                    found++
                }
                b = e
            } else {
                b++
            }
        }
    }
    assertEquals("$dir entries", expected.size, found)
}

private fun assertConnected(puzzle: Puzzle) {
    val cellsOf = puzzle.entries.map { it.cells.toSet() }
    val seen = BooleanArray(cellsOf.size)
    val queue = ArrayDeque<Int>().apply { add(0) }
    seen[0] = true
    while (queue.isNotEmpty()) {
        val i = queue.removeFirst()
        cellsOf.indices.forEach { j ->
            if (!seen[j] && cellsOf[i].any { it in cellsOf[j] }) {
                seen[j] = true
                queue.add(j)
            }
        }
    }
    assertTrue("entries are not connected", seen.all { it })
}
