package app.palavrascruzadas.core.text

/** Game alphabet: A–Z plus the Portuguese accented letters. No hyphen, space, apostrophe or Ü. */
object Alphabet {
    const val CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZÁÀÂÃÉÊÍÓÔÕÚÇ"

    private val set: Set<Char> = CHARS.toSet()

    fun contains(c: Char): Boolean = c in set

    /** True if the (already normalised) string is not empty and only has alphabet letters. */
    fun isValidAnswer(s: String): Boolean = s.isNotEmpty() && s.all { it in set }
}
