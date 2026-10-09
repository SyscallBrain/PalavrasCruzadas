package app.palavrascruzadas.core.campaign

import app.palavrascruzadas.core.model.Difficulty
import kotlin.math.roundToInt

/** Level type. */
enum class LevelType { CLASSIC, WHEEL, KEYWORD, DIRETAS, FIT_IT, BOSS }

/**
 * A campaign world. Grid sizes and word counts grow from [sizeFrom]
 * to [sizeTo] and from [wordsFrom] to [wordsTo] across its 40 levels.
 */
data class World(
    val number: Int,
    val name: String,
    val theme: String,
    val sizeFrom: Int,
    val sizeTo: Int,
    val wordsFrom: Int,
    val wordsTo: Int,
    val difficulties: Set<Difficulty>,
)

/** Campaign worlds and level types. */
object WorldTable {
    const val LEVELS_PER_WORLD = 40

    val worlds: List<World> = listOf(
        World(1, "Primeiras Letras", "geral", 5, 6, 4, 6, setOf(Difficulty.EASY)),
        World(2, "À Mesa", "comida", 6, 7, 5, 7, setOf(Difficulty.EASY)),
        World(3, "Bicharada", "animais", 7, 7, 6, 8, setOf(Difficulty.EASY)),
        World(4, "Lá em Casa", "casa", 7, 8, 7, 9, setOf(Difficulty.EASY, Difficulty.MEDIUM)),
        World(5, "Ao Ar Livre", "natureza", 8, 8, 8, 10, setOf(Difficulty.MEDIUM)),
        World(6, "Corpo e Mente", "corpo", 8, 9, 9, 11, setOf(Difficulty.MEDIUM)),
        World(7, "Mar Português", "mar", 9, 9, 10, 12, setOf(Difficulty.MEDIUM)),
        World(8, "Em Movimento", "transportes", 9, 10, 11, 13, setOf(Difficulty.MEDIUM)),
        World(9, "Mundo Fora", "geografia", 10, 10, 12, 14, setOf(Difficulty.MEDIUM, Difficulty.HARD)),
        World(10, "Ofícios", "profissoes", 10, 11, 13, 15, setOf(Difficulty.MEDIUM, Difficulty.HARD)),
        World(11, "Em Jogo", "desporto", 11, 11, 14, 16, setOf(Difficulty.HARD)),
        World(12, "Laboratório", "ciencia", 11, 12, 15, 18, setOf(Difficulty.HARD)),
        World(13, "Palco e Tela", "musica-arte", 12, 12, 16, 20, setOf(Difficulty.HARD)),
        World(14, "Memórias", "historia-pt", 12, 13, 17, 22, setOf(Difficulty.HARD)),
        World(15, "Mestre das Palavras", "lingua", 13, 13, 18, 24, setOf(Difficulty.HARD)),
    )

    /** Type of the level at position 1–40 of a world. */
    fun typeAt(position: Int): LevelType {
        require(position in 1..LEVELS_PER_WORLD) { "Position out of range: $position" }
        return when (position) {
            5, 15, 35 -> LevelType.WHEEL
            10, 30 -> LevelType.KEYWORD
            20 -> LevelType.DIRETAS
            25 -> LevelType.FIT_IT
            LEVELS_PER_WORLD -> LevelType.BOSS
            else -> LevelType.CLASSIC
        }
    }

    /**
     * Grid side at the given position. The boss (position 40) gets +1.
     */
    fun sizeAt(world: World, position: Int): Int {
        val base = interpolate(world.sizeFrom, world.sizeTo, position)
        return if (typeAt(position) == LevelType.BOSS) base + 1 else base
    }

    /** Number of words at the given position. The boss gets +3. */
    fun wordsAt(world: World, position: Int): Int {
        val base = interpolate(world.wordsFrom, world.wordsTo, position)
        return if (typeAt(position) == LevelType.BOSS) base + 3 else base
    }

    private fun interpolate(from: Int, to: Int, position: Int): Int {
        val t = (position - 1) / (LEVELS_PER_WORLD - 1).toDouble()
        return (from + (to - from) * t).roundToInt()
    }
}
