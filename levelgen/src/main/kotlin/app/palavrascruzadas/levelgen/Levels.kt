package app.palavrascruzadas.levelgen

import app.palavrascruzadas.core.bank.ClueBank
import app.palavrascruzadas.core.campaign.LevelType
import app.palavrascruzadas.core.campaign.World
import app.palavrascruzadas.core.campaign.WorldTable
import app.palavrascruzadas.core.daily.DailyPuzzle
import app.palavrascruzadas.core.gen.CandidateOrder
import app.palavrascruzadas.core.books.Books
import app.palavrascruzadas.core.gen.ClueUsage
import app.palavrascruzadas.core.gen.FreeformGenerator
import app.palavrascruzadas.core.gen.GenSpec
import app.palavrascruzadas.core.gen.DiretasGenerator
import app.palavrascruzadas.core.gen.FitIt
import app.palavrascruzadas.core.gen.GenerationException
import app.palavrascruzadas.core.gen.WheelGenerator
import app.palavrascruzadas.core.gen.KeywordPlacer
import app.palavrascruzadas.core.model.Difficulty
import app.palavrascruzadas.core.model.Puzzle
import app.palavrascruzadas.core.model.PuzzleKind

/** A pre-generated grid, with the fields the file stores. */
internal data class Level(
    val number: Int,
    val puzzle: Puzzle,
    val type: LevelType,
    val theme: String?,
    /** Wheel only: base word and bonus words. */
    val base: String? = null,
    val bonus: List<String> = emptyList(),
)

/** Each answer appears at most 4 times in the campaign. */
private const val CAMPAIGN_ANSWER_CAP = 4

/** All difficulties: only used by the last campaign relaxation. */
private val ALL_DIFFICULTIES = Difficulty.entries.toSet()

/** Seeds tried per level before giving up (each one is deterministic). */
private const val SEEDS_PER_ATTEMPT = 8

/** Seeds for the relaxations (from the second one on), to limit the cost of failures. */
private const val SEEDS_RELAXED = 3

/** Book themes: the 14 bank themes except `geral`. */
val BOOK_THEMES: List<String> = listOf(
    "comida", "animais", "natureza", "geografia", "desporto", "corpo", "casa",
    "ciencia", "musica-arte", "profissoes", "historia-pt", "transportes", "mar", "lingua",
)

/** Seeds tried per puzzle of a type book. */
private const val BOOK_SEEDS = 40

/** Puzzles per book: 14 × 25 = 350. */
const val BOOK_SIZE = 25

/**
 * Stable seed from text parts (32-bit FNV-1a). Does not use `hashCode`, which
 * is not guaranteed across JDK versions, so that `--check` always gives the same result.
 */
internal fun stableSeed(vararg parts: String): Int {
    var hash = -2128831035
    for (b in parts.joinToString("|").toByteArray(Charsets.UTF_8)) {
        hash = (hash xor (b.toInt() and 0xff)) * 16777619
    }
    return hash
}

/** Generates campaign levels, daily puzzles and books with the same bank and the same rules. */
internal class Generator(private val bank: ClueBank) {
    private val generator = FreeformGenerator(bank)
    private val usage = ClueUsage()
    private val campaignUse = HashMap<String, Int>()
    private val capped = HashSet<String>()

    /** Levels that needed every theme (not only the world's theme plus `geral`). */
    val fallbackLevels = mutableListOf<Int>()

    /** A world and its 40 levels, in order. */
    fun campaign(): List<Pair<World, List<Level>>> = WorldTable.worlds.map { world ->
        val usedInWorld = HashSet<String>()
        val levels = (1..WorldTable.LEVELS_PER_WORLD).map { position ->
            val number = (world.number - 1) * WorldTable.LEVELS_PER_WORLD + position
            val level = campaignLevel(world, position, number, usedInWorld)
            usedInWorld += level.puzzle.entries.map { it.answer }
            level.puzzle.keyword?.let { usedInWorld += it.word }
            level.puzzle.entries.forEach { recordCampaignUse(it.answer) }
            level
        }
        world to levels
    }

    private fun recordCampaignUse(answer: String) {
        val n = (campaignUse[answer] ?: 0) + 1
        campaignUse[answer] = n
        if (n >= CAMPAIGN_ANSWER_CAP) capped += answer
    }

    private fun campaignLevel(world: World, position: Int, number: Int, usedInWorld: Set<String>): Level {
        val type = WorldTable.typeAt(position)
        val size = WorldTable.sizeAt(world, position)
        val words = WorldTable.wordsAt(world, position)
        val excluded = usedInWorld + capped
        // About 60 % of the levels (positions 1–3 of every 5) first try the world's theme only;
        // the others start with the theme plus `geral`.
        // Then come the relaxations: every theme, and finally every difficulty.
        val themeFirst = position % 5 in 1..3
        val mixed = setOf(world.theme, "geral")
        val tiers = buildList {
            if (themeFirst) add(setOf(world.theme) to world.difficulties)
            add(mixed to world.difficulties)
            add(null to world.difficulties)
            add(mixed to ALL_DIFFICULTIES)
            add(null to ALL_DIFFICULTIES)
        }
        val top = if (type == LevelType.BOSS) world.wordsTo + 3 else world.wordsTo
        val minimum = maxOf(world.wordsFrom, words - 2)
        val maximum = maxOf(minimum, minOf(words + 1, top))
        for ((tryIndex, tier) in tiers.withIndex()) {
            val (themes, difficulties) = tier
            // The first attempt uses more seeds; the relaxations use fewer (each failure is expensive).
            val seeds = if (tryIndex == 0) SEEDS_PER_ATTEMPT else SEEDS_RELAXED
            for (k in 0 until seeds) {
                val seed = stableSeed("campaign", number.toString(), k.toString())
                val spec = GenSpec(
                    maxW = size,
                    maxH = size,
                    // Between 2 words below the target and the top of the world's range.
                    minWords = minimum,
                    maxWords = maximum,
                    difficulties = difficulties,
                    themes = themes,
                    rngSeed = seed,
                    excludeAnswers = excluded,
                )
                val built = try {
                    buildFor(type, spec, seed, world, position, excluded)
                } catch (e: GenerationException) {
                    continue
                }
                val puzzle = when (type) {
                    LevelType.KEYWORD -> {
                        val withKeyword = KeywordPlacer.place(built.puzzle, CandidateOrder.keywordCandidates(bank, world.theme, excluded, seed))
                        if (withKeyword.keyword == null) continue
                        withKeyword
                    }
                    LevelType.BOSS -> built.puzzle.copy(kind = PuzzleKind.BOSS)
                    else -> built.puzzle
                }
                if (tryIndex > (if (themeFirst) 1 else 0)) fallbackLevels += number
                return Level(
                    number = number,
                    puzzle = puzzle.copy(id = "c%03d".format(number)),
                    type = type,
                    theme = world.theme,
                    base = built.base,
                    bonus = built.bonus,
                )
            }
        }
        error("Level $number (world ${world.number}, position $position) was not generated by any relaxation")
    }

    /** Result of a generator: the puzzle and, for the wheel, the base and the bonus words. */
    private class Built(val puzzle: Puzzle, val base: String? = null, val bonus: List<String> = emptyList())

    /** Generates the requested type: diretas, wheel, fit-it, or freeform with keyword/boss. */
    private fun buildFor(
        type: LevelType,
        spec: GenSpec,
        seed: Int,
        world: World,
        position: Int,
        excluded: Set<String>,
    ): Built = when (type) {
        LevelType.DIRETAS -> {
            // Diretas grids are 5×5 to 9×9, so the word count follows the side (up to 8 per side).
            val side = minOf(spec.maxW, DiretasGenerator.MAX_FREEFORM)
            val minWords = minOf(spec.minWords, side)
            val maxWords = maxOf(minWords + 1, minOf(spec.maxWords, side + 2))
            Built(DiretasGenerator(bank).generate(spec.copy(maxW = side, maxH = side, minWords = minWords, maxWords = maxWords), usage))
        }
        LevelType.WHEEL -> {
            // If there is no base of the requested length, try shorter bases (the wheel must exist).
            val target = wheelBaseLength(world.number)
            val wheel = (target downTo target - 2).firstNotNullOfOrNull { length ->
                try {
                    WheelGenerator(bank).generate(
                        seed = seed,
                        baseLength = length,
                        difficulties = spec.difficulties,
                        maxSide = spec.maxW,
                        excluded = excluded,
                    )
                } catch (e: GenerationException) {
                    null
                }
            } ?: throw GenerationException("Sem roda para a semente $seed")
            Built(wheel.puzzle, wheel.base, wheel.bonus)
        }
        LevelType.FIT_IT -> Built(FitIt.build(generator.generate(spec, usage)))
        else -> Built(generator.generate(spec, usage))
    }

    /** Wheel base word: 5 letters in the first worlds, 7 in the last ones. */
    private fun wheelBaseLength(world: Int): Int = when {
        world <= 3 -> 5
        world <= 9 -> 6
        else -> 7
    }

    /** Daily: 730 days from 2027-01-01, generated by the same code as the phone. */
    fun daily(): List<Level> = (0 until DailyPuzzle.PRE_GENERATED_DAYS).map { i ->
        val date = DailyPuzzle.START.plusDays(i.toLong())
        val keyword = DailyPuzzle.isKeywordDay(date)
        Level(
            number = i + 1,
            puzzle = DailyPuzzle.generate(bank, date),
            type = if (keyword) LevelType.KEYWORD else LevelType.CLASSIC,
            theme = null,
        )
    }

    /**
     * Type books: wheel, diretas and fit-it, 60 puzzles each, with growing difficulty.
     * They have their own clue-usage state, like the themed books.
     */
    /** Tries the following seeds until one generates (each one is deterministic). */
    private inline fun <T> retrySeeds(seed: Int, attempt: (Int) -> T): T {
        for (k in 0 until BOOK_SEEDS) {
            try {
                return attempt(seed + k * 7919)
            } catch (e: GenerationException) {
                // This seed gave no grid: try the next one.
            }
        }
        throw GenerationException("No grid for seed $seed after $BOOK_SEEDS attempts")
    }

    fun specialBook(kind: String): List<Level> {
        val bookUsage = ClueUsage()
        val all = Books.PUZZLES_PER_SPECIAL
        return (0 until all).map { i ->
            val number = i + 1
            val seed = stableSeed("book", kind, i.toString())
            val built = retrySeeds(seed) { seedK -> when (kind) {
                "roda" -> {
                    val wheel = WheelGenerator(bank).generate(
                        seed = seedK,
                        baseLength = 5 + i * 2 / (all - 1),
                        difficulties = setOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD),
                        maxSide = 7,
                    )
                    Built(wheel.puzzle.copy(id = "roda-%02d".format(number)), wheel.base, wheel.bonus)
                }
                "diretas" -> {
                    val side = 5 + i * 3 / (all - 1)
                    val spec = GenSpec(
                        maxW = side, maxH = side, minWords = side - 2, maxWords = side + 1,
                        difficulties = setOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD), rngSeed = seedK,
                    )
                    Built(DiretasGenerator(bank).generate(spec, bookUsage).copy(id = "diretas-%02d".format(number)))
                }
                "encaixar" -> {
                    val side = 5 + i * 6 / (all - 1)
                    val spec = GenSpec(
                        maxW = side, maxH = side, minWords = side - 1, maxWords = side + 1,
                        difficulties = setOf(Difficulty.EASY, Difficulty.MEDIUM), rngSeed = seedK,
                    )
                    Built(FitIt.build(generator.generate(spec, bookUsage)).copy(id = "encaixar-%02d".format(number)))
                }
                else -> error("Unknown book: $kind")
            } }
            Level(number = number, puzzle = built.puzzle, type = LevelType.CLASSIC, theme = null, base = built.base, bonus = built.bonus)
        }
    }

    /** Books: 25 puzzles per theme, from 7×7 to 11×11, with the requested theme. */
    /** Each book has its own clue-usage state: it depends neither on the campaign nor on the generation order. */
    fun books(theme: String): List<Level> {
        val bookUsage = ClueUsage()
        return (0 until BOOK_SIZE).map { i ->
        val size = 7 + Math.round(4.0 * i / (BOOK_SIZE - 1)).toInt()
        var placed: Level? = null
        for (themes in listOf(setOf(theme), setOf(theme, "geral"))) {
            for (k in 0 until SEEDS_PER_ATTEMPT) {
                val seed = stableSeed("book", theme, i.toString(), k.toString())
                val spec = GenSpec(
                    maxW = size,
                    maxH = size,
                    minWords = size - 1,
                    maxWords = size + 2,
                    difficulties = setOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD),
                    themes = themes,
                    rngSeed = seed,
                )
                val puzzle = try {
                    generator.generate(spec, bookUsage)
                } catch (e: GenerationException) {
                    continue
                }
                placed = Level(
                    number = i + 1,
                    puzzle = puzzle.copy(id = "$theme-%02d".format(i + 1)),
                    type = LevelType.CLASSIC,
                    theme = theme,
                )
                break
            }
            if (placed != null) break
        }
            placed ?: error("Book $theme, puzzle ${i + 1} was not generated")
        }
    }
}
