package app.palavrascruzadas.core.gen

/**
 * How many times each clue has been used. The generator prefers the least used ones,
 * so the same clue does not show up in every grid.
 * The object is passed in from outside, so the whole campaign shares the count.
 */
class ClueUsage {
    private val counts = HashMap<String, Int>()

    fun count(text: String): Int = counts[text] ?: 0

    fun record(text: String) {
        counts[text] = count(text) + 1
    }
}
