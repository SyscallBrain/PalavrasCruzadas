package app.palavrascruzadas.core.text

import java.text.Normalizer
import java.util.Locale

/** Normalisation of all text entering the game (assets, on-screen keyboard, physical keyboard). */
object Normalize {
    private val PT: Locale = Locale.forLanguageTag("pt-PT")

    /** NFC, pt-PT upper case, trimmed. */
    fun answer(raw: String): String =
        Normalizer.normalize(raw, Normalizer.Form.NFC).trim().uppercase(PT)

    /** Letter without its diacritic (Ã → A, Ç → C). */
    fun base(c: Char): Char =
        Normalizer.normalize(c.toString(), Normalizer.Form.NFD)[0]

    /**
     * Compares the player's input with the answer, cell by cell.
     * With [lenient] on, an unaccented letter accepts the accented version (A in an Ã cell).
     * An accented letter never accepts the unaccented version (Ã in an A cell).
     */
    fun matches(typed: String, expected: String, lenient: Boolean = false): Boolean {
        val t = answer(typed)
        val e = answer(expected)
        if (t.length != e.length) return false
        return t.indices.all { i -> charMatches(t[i], e[i], lenient) }
    }

    private fun charMatches(typed: Char, expected: Char, lenient: Boolean): Boolean =
        typed == expected || (lenient && typed == base(expected))
}
