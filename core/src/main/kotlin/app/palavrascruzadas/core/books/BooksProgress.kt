package app.palavrascruzadas.core.books

import kotlinx.serialization.Serializable

/** Themed books: 14 themes with 25 puzzles each. Stars per puzzle keep the best result. */
object Books {
    const val PUZZLES_PER_BOOK = 25
    const val PUZZLES_PER_SPECIAL = 60

    val THEMES: List<String> = listOf(
        "comida", "animais", "natureza", "geografia", "desporto", "corpo", "casa",
        "ciencia", "musica-arte", "profissoes", "historia-pt", "transportes", "mar", "lingua",
    )

    /** Type books: wheel, diretas and fit-it, 60 puzzles each. */
    val SPECIAL: List<String> = listOf("roda", "diretas", "encaixar")

    fun puzzlesIn(book: String): Int = if (book in SPECIAL) PUZZLES_PER_SPECIAL else PUZZLES_PER_BOOK
}

/** Books progress: theme → puzzle number (1–25) → best stars. */
@Serializable
data class BooksProgress(val stars: Map<String, Map<Int, Int>> = emptyMap()) {

    fun complete(theme: String, number: Int, earned: Int): BooksProgress {
        val book = stars.getOrDefault(theme, emptyMap())
        val best = maxOf(book[number] ?: 0, earned)
        return copy(stars = this.stars + (theme to (book + (number to best))))
    }

    fun doneCount(theme: String): Int = stars[theme]?.size ?: 0

    fun starTotal(theme: String): Int = stars[theme]?.values?.sum() ?: 0
}
