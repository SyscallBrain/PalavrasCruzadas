package app.palavrascruzadas.core.gen

import app.palavrascruzadas.core.bank.BankEntry
import app.palavrascruzadas.core.bank.ClueBank
import app.palavrascruzadas.core.model.Clue
import app.palavrascruzadas.core.model.Difficulty
import app.palavrascruzadas.core.model.Dir
import app.palavrascruzadas.core.model.Entry
import app.palavrascruzadas.core.model.Pos
import app.palavrascruzadas.core.model.Puzzle
import app.palavrascruzadas.core.model.PuzzleKind
import app.palavrascruzadas.core.rng.Mulberry32
import kotlin.math.max

/**
 * Generates freeform crossword grids.
 * Deterministic: the same [GenSpec] and the same bank always give the same puzzle.
 */
class FreeformGenerator(private val bank: ClueBank) {

    /**
     * [maxAttempts] limits the layout attempts. The benchmark uses 1 to measure the
     * "first attempt" success rate.
     */
    fun generate(spec: GenSpec, usage: ClueUsage = ClueUsage(), maxAttempts: Int = MAX_ATTEMPTS): Puzzle =
        generateFrom(candidates(spec), spec, usage, maxAttempts)

    /**
     * Like [generate], but with the given words instead of the bank. Used by the letter wheel,
     * which can only use sub-words of the base word.
     */
    fun generateFrom(words: List<BankEntry>, spec: GenSpec, usage: ClueUsage = ClueUsage(), maxAttempts: Int = MAX_ATTEMPTS): Puzzle {
        val pool = words.filter { e ->
            e.answer.length in spec.minLen..spec.maxLen &&
                e.answer.length <= max(spec.maxW, spec.maxH) &&
                e.answer !in spec.excludeAnswers &&
                e.clues.any { clueAllowed(it, spec) }
        }
        if (pool.size < spec.minWords) {
            throw GenerationException("Only ${pool.size} candidate answers for $spec")
        }
        val occurrences = occurrencesByLetter(pool)
        val rng = Mulberry32(spec.rngSeed)
        repeat(maxAttempts) {
            val layout = Attempt(spec, pool, occurrences, rng).run()
            if (layout != null) return build(spec, layout, usage, rng)
        }
        throw GenerationException("No layout for seed ${spec.rngSeed} in ${spec.maxW}x${spec.maxH} after $maxAttempts attempts")
    }

    private fun candidates(spec: GenSpec): List<BankEntry> =
        bank.entries.filter { e ->
            val n = e.answer.length
            n in spec.minLen..spec.maxLen &&
                n <= max(spec.maxW, spec.maxH) &&
                e.answer !in spec.excludeAnswers &&
                (spec.themes == null || e.themes.any { it in spec.themes }) &&
                e.clues.any { clueAllowed(it, spec) }
        }

    private fun clueAllowed(clue: Clue, spec: GenSpec): Boolean =
        clue.difficulty in spec.difficulties && (!spec.requireShortClue || clue.short != null)

    private fun occurrencesByLetter(pool: List<BankEntry>): Map<Char, List<Occurrence>> {
        val out = HashMap<Char, MutableList<Occurrence>>()
        pool.forEachIndexed { candidate, entry ->
            entry.answer.forEachIndexed { index, letter ->
                out.getOrPut(letter) { mutableListOf() }.add(Occurrence(candidate, index))
            }
        }
        return out
    }

    private fun build(spec: GenSpec, layout: Layout, usage: ClueUsage, rng: Mulberry32): Puzzle {
        val placed = layout.placed.map { Placed(it.word, Pos(it.start.row - layout.minRow, it.start.col - layout.minCol), it.dir) }
        val width = layout.maxCol - layout.minCol + 1
        val height = layout.maxRow - layout.minRow + 1
        val filled = placed.flatMap { cellsOf(it.word.answer, it.start, it.dir) }.toSet()
        val numbers = Numbering.assign(width, height, filled)

        val entries = placed.map { p ->
            val clue = pickClue(p.word, spec, usage, rng)
            Entry(
                number = numbers.getValue(p.start),
                row = p.start.row,
                col = p.start.col,
                dir = p.dir,
                answer = p.word.answer,
                // With a mandatory short clue (diretas), the entry uses only the short clue.
                clue = if (spec.requireShortClue) clue.short!! else clue.text,
                shortClue = if (spec.requireShortClue) null else clue.short,
            )
        }.sortedWith(compareBy({ it.number }, { it.dir.ordinal }))

        val letters = entries.flatMap { it.answer.toList() }.toSet().size
        val difficulty = spec.difficulties.singleOrNull() ?: Difficulty.MEDIUM
        return Puzzle(
            id = "f-${spec.rngSeed}",
            kind = PuzzleKind.CLASSIC,
            width = width,
            height = height,
            entries = entries,
            keyword = null,
            givens = emptySet(),
            theme = spec.themes?.singleOrNull(),
            difficulty = difficulty,
            targetSeconds = targetSeconds(letters, difficulty),
        )
    }

    /** Picks the least used clue of the requested difficulty; ties are broken by the rng. */
    private fun pickClue(word: BankEntry, spec: GenSpec, usage: ClueUsage, rng: Mulberry32): Clue {
        val options = word.clues.filter { clueAllowed(it, spec) }
        val least = options.minOf { usage.count(it.text) }
        val best = options.filter { usage.count(it.text) == least }
        val chosen = best[rng.nextInt(best.size)]
        usage.record(chosen.text)
        return chosen
    }

    /** Σ distinct letters × difficulty factor (easy 5 s, medium 7 s, hard 9 s), rounded to 15 s. */
    private fun targetSeconds(distinctLetters: Int, difficulty: Difficulty): Int {
        val factor = when (difficulty) {
            Difficulty.EASY -> 5
            Difficulty.MEDIUM -> 7
            Difficulty.HARD -> 9
        }
        val raw = distinctLetters * factor
        return max(15, ((raw + 14) / 15) * 15)
    }

    private class Occurrence(val candidate: Int, val index: Int)

    private class Placed(val word: BankEntry, val start: Pos, val dir: Dir)

    private class Layout(
        val placed: List<Placed>,
        val minRow: Int,
        val minCol: Int,
        val maxRow: Int,
        val maxCol: Int,
    )

    /** One attempt: places words one by one, each crossing a word already placed. */
    private class Attempt(
        private val spec: GenSpec,
        private val pool: List<BankEntry>,
        private val occurrences: Map<Char, List<Occurrence>>,
        private val rng: Mulberry32,
    ) {
        private val grid = HashMap<Pos, Char>()
        private val cover = HashMap<Pos, Int>()     // bit 0 = horizontal, bit 1 = vertical
        private val placed = ArrayList<Placed>()
        private val answers = HashSet<String>()
        private val lemmas = HashSet<String>()
        private var minRow = 0
        private var minCol = 0
        private var maxRow = 0
        private var maxCol = 0

        fun run(): Layout? {
            seed()
            while (placed.size < spec.maxWords) {
                val options = findOptions()
                if (options.isEmpty()) break
                val ranked = options.sortedWith(compareByDescending<Option> { it.score }.thenBy { it.key })
                apply(ranked[rng.nextInt(minOf(3, ranked.size))])
            }
            return if (accepted()) Layout(placed.toList(), minRow, minCol, maxRow, maxCol) else null
        }

        private fun seed() {
            val longest = pool.filter { it.answer.length <= spec.maxW }
                .sortedWith(compareByDescending<BankEntry> { it.answer.length }.thenBy { it.answer })
                .take(SEED_POOL)
            val word = longest[rng.nextInt(longest.size)]
            apply(Option(word, Pos(0, 0), Dir.ACROSS, 0, 0.0, word.answer))
        }

        private fun findOptions(): List<Option> {
            val seen = HashSet<String>()
            val out = ArrayList<Option>()
            for (p in grid.keys.sortedWith(compareBy({ it.row }, { it.col }))) {
                val letter = grid.getValue(p)
                for (occ in occurrences[letter].orEmpty()) {
                    val word = pool[occ.candidate]
                    if (word.answer in answers || word.lemma in lemmas) continue
                    for (dir in Dir.entries) {
                        if (isCovered(p, dir)) continue
                        val start = if (dir == Dir.ACROSS) Pos(p.row, p.col - occ.index) else Pos(p.row - occ.index, p.col)
                        val key = "${word.answer}@${start.row},${start.col},${dir.ordinal}"
                        if (!seen.add(key)) continue
                        val check = check(word.answer, start, dir) ?: continue
                        out.add(check.toOption(word, start, dir, key))
                    }
                }
            }
            return out
        }

        private fun isCovered(p: Pos, dir: Dir): Boolean = (cover[p] ?: 0) and bit(dir) != 0

        /** Checks whether [answer] fits at [start] in direction [dir] without creating accidental words. */
        private fun check(answer: String, start: Pos, dir: Dir): Check? {
            val (dr, dc) = delta(dir)
            val before = Pos(start.row - dr, start.col - dc)
            val after = Pos(start.row + answer.length * dr, start.col + answer.length * dc)
            if (before in grid || after in grid) return null
            var cross = 0
            var fresh = 0
            var r0 = minRow; var r1 = maxRow; var c0 = minCol; var c1 = maxCol
            for (k in answer.indices) {
                val q = Pos(start.row + k * dr, start.col + k * dc)
                val existing = grid[q]
                if (existing != null) {
                    if (existing != answer[k] || isCovered(q, dir)) return null
                    cross++
                } else {
                    fresh++
                    for ((pr, pc) in perpendicular(dir)) {
                        if (Pos(q.row + pr, q.col + pc) in grid) return null
                    }
                }
                r0 = minOf(r0, q.row); r1 = maxOf(r1, q.row)
                c0 = minOf(c0, q.col); c1 = maxOf(c1, q.col)
            }
            if (fresh == 0) return null
            if (c1 - c0 + 1 > spec.maxW || r1 - r0 + 1 > spec.maxH) return null
            val area = (r1 - r0 + 1) * (c1 - c0 + 1)
            return Check(cross, area)
        }

        private fun apply(option: Option) {
            val (dr, dc) = delta(option.dir)
            val answer = option.word.answer
            for (k in answer.indices) {
                val q = Pos(option.start.row + k * dr, option.start.col + k * dc)
                grid.putIfAbsent(q, answer[k])
                cover[q] = (cover[q] ?: 0) or bit(option.dir)
                minRow = minOf(minRow, q.row); maxRow = maxOf(maxRow, q.row)
                minCol = minOf(minCol, q.col); maxCol = maxOf(maxCol, q.col)
            }
            placed.add(Placed(option.word, option.start, option.dir))
            answers.add(answer)
            lemmas.add(option.word.lemma)
        }

        private fun accepted(): Boolean {
            if (placed.size < spec.minWords) return false
            val width = maxCol - minCol + 1
            val height = maxRow - minRow + 1
            val density = grid.size.toDouble() / (width * height)
            val threshold = if (max(width, height) <= 7) DENSITY_SMALL else DENSITY_LARGE
            if (density < threshold) return false
            val crossings = cover.values.count { it == BOTH }
            return crossings.toDouble() / grid.size >= MIN_CROSSING_RATIO
        }

        private fun delta(dir: Dir): Pair<Int, Int> = if (dir == Dir.ACROSS) 0 to 1 else 1 to 0

        private fun perpendicular(dir: Dir): List<Pair<Int, Int>> =
            if (dir == Dir.ACROSS) listOf(-1 to 0, 1 to 0) else listOf(0 to -1, 0 to 1)

        private fun bit(dir: Dir): Int = if (dir == Dir.ACROSS) 1 else 2

        private data class Check(val cross: Int, val area: Int) {
            fun toOption(word: BankEntry, start: Pos, dir: Dir, key: String): Option {
                // Score: more crossings, a more compact grid, across/down balance.
                val score = 3.0 * cross - 0.25 * area
                return Option(word, start, dir, cross, score, key)
            }
        }

        private data class Option(val word: BankEntry, val start: Pos, val dir: Dir, val cross: Int, val score: Double, val key: String)

        companion object {
            private const val BOTH = 3
            private const val SEED_POOL = 20
            private const val DENSITY_SMALL = 0.35
            private const val DENSITY_LARGE = 0.30
            private const val MIN_CROSSING_RATIO = 0.25
        }
    }

    private fun cellsOf(answer: String, start: Pos, dir: Dir): List<Pos> =
        answer.indices.map { k -> if (dir == Dir.ACROSS) Pos(start.row, start.col + k) else Pos(start.row + k, start.col) }

    companion object {
        const val MAX_ATTEMPTS = 200
    }
}
