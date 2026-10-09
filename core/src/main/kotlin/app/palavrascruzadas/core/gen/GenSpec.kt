package app.palavrascruzadas.core.gen

import app.palavrascruzadas.core.model.Difficulty

/**
 * Parameters of a freeform grid.
 * [rngSeed] together with the other fields fully determines the puzzle.
 */
data class GenSpec(
    val maxW: Int,
    val maxH: Int,
    val minWords: Int,
    val maxWords: Int,
    val difficulties: Set<Difficulty> = Difficulty.entries.toSet(),
    val themes: Set<String>? = null,
    val minLen: Int = 3,
    val maxLen: Int = 13,
    val rngSeed: Int,
    val excludeAnswers: Set<String> = emptySet(),
    /** Only answers with a short clue `s` (diretas). The entry uses the short clue. */
    val requireShortClue: Boolean = false,
) {
    init {
        require(maxW in 3..30 && maxH in 3..30) { "Grid size out of range: ${maxW}x$maxH" }
        require(minWords >= 2 && maxWords >= minWords) { "Invalid word counts $minWords..$maxWords" }
        require(difficulties.isNotEmpty()) { "At least one difficulty is required" }
        require(minLen in 3..maxLen) { "Invalid length range $minLen..$maxLen" }
    }
}

/** Thrown when no grid satisfies the [GenSpec] with the available answers. */
class GenerationException(message: String) : RuntimeException(message)
