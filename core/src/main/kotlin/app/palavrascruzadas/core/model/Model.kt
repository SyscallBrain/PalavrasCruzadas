package app.palavrascruzadas.core.model

/** Cell coordinates: row and column, 0-based. */
data class Pos(val row: Int, val col: Int)

enum class Dir { ACROSS, DOWN }

enum class PuzzleKind { CLASSIC, MINI, KEYWORD, DIRETAS, FIT_IT, WHEEL, BOSS }

/** 1, 2 and 3 in the JSON (see [fromCode]). */
enum class Difficulty(val code: Int) {
    EASY(1), MEDIUM(2), HARD(3);

    companion object {
        fun fromCode(code: Int): Difficulty =
            entries.firstOrNull { it.code == code } ?: throw IllegalArgumentException("Unknown difficulty code: $code")
    }
}

/** Clue of an answer, as it comes from the bank. */
data class Clue(val text: String, val short: String?, val difficulty: Difficulty)

/** Bank theme (`content/themes.json`). */
data class Theme(val id: String, val name: String, val icon: String, val color: String)

/**
 * A grid entry. [answer] is NFC upper case with accents.
 * [number] is the visible number (diretas and fit-it do not use it).
 */
data class Entry(
    val number: Int,
    val row: Int,
    val col: Int,
    val dir: Dir,
    val answer: String,
    val clue: String,
    val shortClue: String? = null,
) {
    val length: Int get() = answer.length

    /** Cells covered by the entry, from start to end. */
    val cells: List<Pos>
        get() = List(answer.length) { i ->
            if (dir == Dir.ACROSS) Pos(row, col + i) else Pos(row + i, col)
        }
}

/** Hidden keyword: [cells] in the order of the letters of [word]. */
data class Keyword(val word: String, val cells: List<Pos>)

/** A puzzle cell: correct letter and the indices of the entries crossing it. */
data class Cell(val pos: Pos, val letter: Char, val entries: List<Int>) {
    val isCrossing: Boolean get() = entries.size > 1
}
