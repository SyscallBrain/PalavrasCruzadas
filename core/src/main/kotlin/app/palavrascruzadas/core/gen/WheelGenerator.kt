package app.palavrascruzadas.core.gen

import app.palavrascruzadas.core.bank.BankEntry
import app.palavrascruzadas.core.bank.ClueBank
import app.palavrascruzadas.core.model.Difficulty
import app.palavrascruzadas.core.model.Puzzle
import app.palavrascruzadas.core.model.PuzzleKind
import app.palavrascruzadas.core.rng.Mulberry32

/**
 * Letter wheel. [puzzle] is the grid made only of sub-words of the base, base included.
 * [letters] are the base letters shuffled for the wheel; [bonus] are the sub-words left out of the grid.
 */
data class Wheel(val puzzle: Puzzle, val base: String, val letters: List<Char>, val bonus: List<String>)

class WheelGenerator(private val bank: ClueBank) {

    /**
     * Looks for a base word of [baseLength] letters with at least 5 sub-words (3 or more letters) and
     * builds the grid from them only. Throws [GenerationException] if no base works.
     */
    fun generate(seed: Int, baseLength: Int, difficulties: Set<Difficulty>, maxSide: Int = 7, excluded: Set<String> = emptySet()): Wheel {
        val rng = Mulberry32(seed)
        val bases = bank.entries.filter { it.answer.length == baseLength }.map { it.answer }.sorted()
        val order = shuffled(bases, rng)
        for (base in order.take(MAX_BASES)) {
            val subs = subwords(base).filter { it.answer !in excluded }
            if (subs.size < MIN_SUBWORDS) continue
            val spec = GenSpec(
                maxW = maxSide,
                maxH = maxSide,
                minWords = MIN_GRID_WORDS,
                maxWords = MAX_GRID_WORDS,
                difficulties = difficulties,
                rngSeed = seed + base.hashCode(),
                minLen = 3,
            )
            val grid = try {
                FreeformGenerator(bank).generateFrom(subs, spec, maxAttempts = GRID_ATTEMPTS)
            } catch (e: GenerationException) {
                continue
            }
            if (grid.entries.none { it.answer == base }) continue
            val inGrid = grid.entries.map { it.answer }.toSet()
            val bonus = subs.map { it.answer }.filter { it !in inGrid }.sorted()
            return Wheel(
                puzzle = grid.copy(id = "roda-$seed", kind = PuzzleKind.WHEEL),
                base = base,
                letters = shuffled(base.toList(), Mulberry32(seed + 1)),
                bonus = bonus,
            )
        }
        throw GenerationException("No base word of $baseLength letters works for seed $seed")
    }

    /** Bank answers with 3 or more letters whose letter multiset fits inside the base's (accents included). */
    private fun subwords(base: String): List<BankEntry> {
        val available = base.groupingBy { it }.eachCount()
        return bank.entries.filter { e ->
            e.answer.length in 3..base.length &&
                e.answer.groupingBy { it }.eachCount().all { (c, n) -> (available[c] ?: 0) >= n }
        }
    }

    private fun <T> shuffled(items: List<T>, rng: Mulberry32): List<T> {
        val list = items.toMutableList()
        for (i in list.size - 1 downTo 1) {
            val j = rng.nextInt(i + 1)
            val tmp = list[i]
            list[i] = list[j]
            list[j] = tmp
        }
        return list
    }

    companion object {
        const val MIN_SUBWORDS = 5
        const val MIN_GRID_WORDS = 4
        const val MAX_GRID_WORDS = 8
        const val MAX_BASES = 200
        const val GRID_ATTEMPTS = 40
    }
}
