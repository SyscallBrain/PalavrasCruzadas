package app.palavrascruzadas.core.daily

import app.palavrascruzadas.core.bank.ClueBank
import app.palavrascruzadas.core.gen.CandidateOrder
import app.palavrascruzadas.core.gen.FreeformGenerator
import app.palavrascruzadas.core.gen.GenSpec
import app.palavrascruzadas.core.gen.GenerationException
import app.palavrascruzadas.core.gen.KeywordPlacer
import app.palavrascruzadas.core.model.Difficulty
import app.palavrascruzadas.core.model.Puzzle
import app.palavrascruzadas.core.model.PuzzleKind
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Daily challenge. The first 730 days from 2027-01-01 are pre-generated
 * (`levels/daily.json`); after that the phone generates the same puzzle from the same seed.
 * Each day uses its own clue-usage state, so the result does not depend on previous days.
 */
object DailyPuzzle {
    val START: LocalDate = LocalDate.of(2027, 1, 1)
    const val PRE_GENERATED_DAYS = 730
    private const val MAX_SEEDS = 8

    /** Index in the pre-generated file, or null if the day is not there. */
    fun preGeneratedIndex(date: LocalDate): Int? {
        val index = ChronoUnit.DAYS.between(START, date)
        return if (index in 0 until PRE_GENERATED_DAYS) index.toInt() else null
    }

    /** On Sundays the daily puzzle has a keyword. */
    fun isKeywordDay(date: LocalDate): Boolean = date.dayOfWeek == DayOfWeek.SUNDAY

    fun id(date: LocalDate): String = "d" + date.toString().replace("-", "")

    /** Generates the day's puzzle: 9×9 medium, with a keyword on Sundays. Same code in `levelgen` and on the phone. */
    fun generate(bank: ClueBank, date: LocalDate): Puzzle {
        val generator = FreeformGenerator(bank)
        val base = dailySeedNumber(date)
        for (k in 0 until MAX_SEEDS) {
            val seed = (base + k).toInt()
            val spec = GenSpec(
                maxW = 9,
                maxH = 9,
                minWords = 8,
                maxWords = 11,
                difficulties = setOf(Difficulty.MEDIUM),
                rngSeed = seed,
            )
            val generated = try {
                generator.generate(spec)
            } catch (e: GenerationException) {
                continue
            }
            val keyword = isKeywordDay(date)
            val puzzle = if (keyword) {
                KeywordPlacer.place(generated, CandidateOrder.keywordCandidates(bank, null, emptySet(), seed))
            } else {
                generated
            }
            if (keyword && puzzle.keyword == null) continue
            return puzzle.copy(
                id = id(date),
                kind = if (keyword) PuzzleKind.KEYWORD else PuzzleKind.CLASSIC,
            )
        }
        throw GenerationException("No grid for the daily challenge of $date")
    }
}
