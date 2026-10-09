package app.palavrascruzadas.core.daily

import app.palavrascruzadas.core.bank.ClueBank
import app.palavrascruzadas.core.gen.FreeformGenerator
import app.palavrascruzadas.core.gen.GenSpec
import app.palavrascruzadas.core.gen.GenerationException
import app.palavrascruzadas.core.model.Difficulty
import app.palavrascruzadas.core.model.Puzzle
import app.palavrascruzadas.core.model.PuzzleKind

/** Quick mini: 5×5, 4 to 6 words, easy. Generated on the phone. */
object MiniPuzzle {
    private const val MAX_SEEDS = 50

    fun generate(bank: ClueBank, seed: Int): Puzzle {
        val generator = FreeformGenerator(bank)
        for (k in 0 until MAX_SEEDS) {
            val spec = GenSpec(
                maxW = 5,
                maxH = 5,
                minWords = 4,
                maxWords = 6,
                difficulties = setOf(Difficulty.EASY),
                rngSeed = seed + k,
            )
            val puzzle = try {
                generator.generate(spec)
            } catch (e: GenerationException) {
                continue
            }
            return puzzle.copy(id = "mini-$seed", kind = PuzzleKind.MINI)
        }
        throw GenerationException("Nenhum mini para a semente $seed")
    }
}
