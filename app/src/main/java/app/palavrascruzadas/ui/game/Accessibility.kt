package app.palavrascruzadas.ui.game

import app.palavrascruzadas.core.model.Dir
import app.palavrascruzadas.core.model.Pos
import app.palavrascruzadas.core.play.PlayState

/** Spoken names of the accented keys, for TalkBack ("a com til"). */
private val ACCENT_NAMES = mapOf(
    'Á' to "a com acento agudo", 'À' to "a com acento grave", 'Â' to "a com acento circunflexo", 'Ã' to "a com til",
    'É' to "e com acento agudo", 'Ê' to "e com acento circunflexo", 'Í' to "i com acento agudo",
    'Ó' to "o com acento agudo", 'Ô' to "o com acento circunflexo", 'Õ' to "o com til",
    'Ú' to "u com acento agudo", 'Ç' to "c com cedilha",
)

/** Description of a key of the in-app keyboard. */
fun keyDescription(letter: Char): String = ACCENT_NAMES[letter] ?: letter.lowercaseChar().toString()

/** Description of a grid cell: entry, position in the word and what is written there. */
fun cellDescription(play: PlayState, pos: Pos): String {
    val entries = play.puzzle.entries.filter { pos in it.cells }
    if (entries.isEmpty()) return "Casa sem letra"
    // The across entry wins, as in the clue bar.
    val entry = entries.firstOrNull { it.dir == Dir.ACROSS } ?: entries.first()
    val index = entry.cells.indexOf(pos) + 1
    val dir = if (entry.dir == Dir.ACROSS) "horizontal" else "vertical"
    val written = play.typed[pos] ?: play.revealed.takeIf { pos in it }?.let { play.solution[pos] }
    val state = written?.let { keyDescription(it) } ?: "vazia"
    return "${entry.number} $dir, letra $index de ${entry.length}, $state"
}

/** Grid summary for TalkBack: size, words and how many are correct. */
fun gridDescription(play: PlayState): String =
    "Grelha de ${play.puzzle.width} por ${play.puzzle.height}, ${play.puzzle.entries.size} palavras, ${play.found.size} certas"
