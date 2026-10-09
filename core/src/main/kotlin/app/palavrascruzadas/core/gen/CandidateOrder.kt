package app.palavrascruzadas.core.gen

import app.palavrascruzadas.core.bank.ClueBank
import app.palavrascruzadas.core.rng.Mulberry32

/**
 * Order of keyword candidates. Theme answers first, then
 * the others, each group shuffled by the seed. Used by `levelgen` and by the phone: the same
 * puzzle must come out identical on both sides.
 */
object CandidateOrder {

    fun keywordCandidates(bank: ClueBank, theme: String?, excluded: Set<String>, seed: Int): List<String> {
        val pool = bank.entries.filter { it.answer.length in 5..8 && it.answer !in excluded }.map { it.answer }
        val themed = if (theme == null) emptyList() else bank.entries
            .filter { it.answer.length in 5..8 && it.answer !in excluded && theme in it.themes }
            .map { it.answer }
        val rest = pool.filter { it !in themed.toSet() }
        return shuffled(themed, seed) + shuffled(rest, seed + 1)
    }

    /** Fisher–Yates over the sorted list: the same seed gives the same order on any machine. */
    private fun shuffled(items: List<String>, seed: Int): List<String> {
        val rng = Mulberry32(seed)
        val list = items.sorted().toMutableList()
        for (i in list.size - 1 downTo 1) {
            val j = rng.nextInt(i + 1)
            val tmp = list[i]
            list[i] = list[j]
            list[j] = tmp
        }
        return list
    }
}
