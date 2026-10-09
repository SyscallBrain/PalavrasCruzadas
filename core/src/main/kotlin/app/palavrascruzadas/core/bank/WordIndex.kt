package app.palavrascruzadas.core.bank

/**
 * Answer index used to find crossings quickly:
 * by length and by (length, position, letter).
 */
class WordIndex(words: Collection<String>) {
    private val byLength: Map<Int, List<String>>
    private val byLetterAt: Map<Triple<Int, Int, Char>, List<String>>

    init {
        val lengths = HashMap<Int, MutableList<String>>()
        val letters = HashMap<Triple<Int, Int, Char>, MutableList<String>>()
        for (word in words) {
            lengths.getOrPut(word.length) { mutableListOf() }.add(word)
            word.forEachIndexed { i, c ->
                letters.getOrPut(Triple(word.length, i, c)) { mutableListOf() }.add(word)
            }
        }
        byLength = lengths
        byLetterAt = letters
    }

    /** Answers with exactly [length] letters. */
    fun ofLength(length: Int): List<String> = byLength[length].orEmpty()

    /** Answers with [length] letters whose position [index] (0-based) is [letter]. */
    fun withLetterAt(length: Int, index: Int, letter: Char): List<String> =
        byLetterAt[Triple(length, index, letter)].orEmpty()
}
