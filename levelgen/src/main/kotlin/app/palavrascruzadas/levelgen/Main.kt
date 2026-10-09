package app.palavrascruzadas.levelgen

import app.palavrascruzadas.core.bank.ClueBank
import app.palavrascruzadas.core.campaign.World
import app.palavrascruzadas.core.gen.FreeformGenerator
import app.palavrascruzadas.core.gen.GenSpec
import app.palavrascruzadas.core.gen.GenerationException
import app.palavrascruzadas.core.gen.toSnapshot
import app.palavrascruzadas.core.model.Difficulty
import app.palavrascruzadas.core.text.Alphabet
import java.io.File
import kotlin.system.exitProcess

// Level generation CLI: campaign | daily | books | all | --check, plus bench and snapshot.
fun main(args: Array<String>) {
    when (args.firstOrNull()) {
        "bench" -> bench(seeds = args.getOrNull(1)?.toIntOrNull() ?: 200)
        "snapshot" -> print(snapshot(ClueBank.parse(findBank().readText(Charsets.UTF_8))))
        "campaign" -> write(setOf("world_"), "campaign")
        "daily" -> write(setOf("daily.json"), "daily")
        "books" -> write(setOf("books/"), "books")
        "all" -> write(setOf("world_", "daily.json", "books/"), "all")
        "--check" -> exitProcess(check())
        else -> {
            println("Usage: levelgen <campaign | daily | books | all | --check | bench [seeds] | snapshot>")
            exitProcess(if (args.isEmpty()) 0 else 1)
        }
    }
}

/** Level assets: `app/src/main/assets/levels/`, relative to the project root. */
private fun levelsDir(): File = File(findRoot(), "app/src/main/assets/levels")

/** Generates the requested files and writes them to the assets. Prints campaign statistics. */
private fun write(prefixes: Set<String>, label: String) {
    val files = buildFiles(prefixes, label)
    val dir = levelsDir()
    for ((path, content) in files) {
        val target = File(dir, path)
        target.parentFile.mkdirs()
        target.writeText(content, Charsets.UTF_8)
    }
    println("Wrote ${files.size} files to ${dir.path}: ${files.keys.sorted().joinToString()}")
}

/**
 * Compares the in-memory generation with the committed assets (CI). Returns 0 when they match.
 * Also fails if the assets grow past 4 MB.
 */
private fun check(): Int {
    val files = buildFiles(setOf("world_", "daily.json", "books/"), "check")
    val dir = levelsDir()
    val different = files.filter { (path, content) ->
        val existing = File(dir, path)
        !existing.exists() || existing.readText(Charsets.UTF_8) != content
    }.keys
    val sizeBytes = files.values.sumOf { it.toByteArray(Charsets.UTF_8).size.toLong() }
    println("Assets: ${files.size} files, ${sizeBytes / 1024} KiB")
    if (different.isNotEmpty()) {
        println("DIFFERENCES (regenerate with `levelgen all` and review the diff): ${different.sorted().joinToString()}")
        return 1
    }
    if (sizeBytes > 4L * 1024 * 1024) {
        println("Assets above 4 MB")
        return 1
    }
    println("--check: no differences")
    return 0
}

/**
 * Generates the files of a set of prefixes (`world_`, `daily.json`, `books/`).
 * Each group is generated from the same bank and with the same clue-usage state.
 */
private fun buildFiles(prefixes: Set<String>, label: String): Map<String, String> {
    val bank = ClueBank.parse(findBank().readText(Charsets.UTF_8))
    val generator = Generator(bank)
    val files = linkedMapOf<String, String>()
    val started = System.nanoTime()
    if ("world_" in prefixes) {
        val worlds = generator.campaign()
        for ((world, levels) in worlds) {
            files["world_%02d.json".format(world.number)] = worldFile(world, levels)
        }
        printCampaignStats(bank, worlds, generator.fallbackLevels)
    }
    if ("daily.json" in prefixes) files["daily.json"] = dailyFile(generator.daily())
    if ("books/" in prefixes) {
        for (theme in BOOK_THEMES) files["books/$theme.json"] = bookFile(theme, generator.books(theme))
        for (kind in app.palavrascruzadas.core.books.Books.SPECIAL) files["books/$kind.json"] = bookFile(kind, generator.specialBook(kind))
    }
    println("$label: ${files.size} files in ${(System.nanoTime() - started) / 1_000_000_000.0} s")
    return files
}

/** Campaign statistics: fallbacks, theme share and maximum uses of answers and clues. */
private fun printCampaignStats(bank: ClueBank, worlds: List<Pair<World, List<Level>>>, fallbacks: List<Int>) {
    val levels = worlds.flatMap { it.second }
    println("Campaign: ${levels.size} levels, ${fallbacks.size} with every theme, ${levels.count { it.puzzle.kind == app.palavrascruzadas.core.model.PuzzleKind.WHEEL }} wheel, ${levels.count { it.puzzle.kind == app.palavrascruzadas.core.model.PuzzleKind.DIRETAS }} diretas, ${levels.count { it.puzzle.kind == app.palavrascruzadas.core.model.PuzzleKind.FIT_IT }} fit-it")
    for ((world, lvls) in worlds) {
        val answers = lvls.flatMap { it.puzzle.entries.map { e -> e.answer } }
        val themed = answers.count { a -> bank.entry(a)?.themes?.contains(world.theme) == true }
        println("  world %2d %-22s answers %4d, on theme %3d%%".format(world.number, world.name, answers.size, 100 * themed / answers.size.coerceAtLeast(1)))
    }
    val answerUse = levels.flatMap { it.puzzle.entries.map { e -> e.answer } }.groupingBy { it }.eachCount()
    // Fit-it shows no clues (all of them carry the "Encaixe" marker), so it is left out.
    val clueUse = levels.filter { it.puzzle.kind != app.palavrascruzadas.core.model.PuzzleKind.FIT_IT }.flatMap { it.puzzle.entries.map { e -> e.clue } }.groupingBy { it }.eachCount()
    println("  max uses of one answer in the campaign: ${answerUse.values.maxOrNull()} (limit 4)")
    println("  max uses of one clue in the campaign: ${clueUse.values.maxOrNull()} (limit 2)")
}

/** Finds the project root (the folder with settings.gradle.kts), walking up from the current directory. */
private fun findRoot(): File {
    var dir: File? = File("").absoluteFile
    while (dir != null) {
        if (File(dir, "settings.gradle.kts").exists()) return dir
        dir = dir.parentFile
    }
    error("Project root not found above ${File("").absolutePath}")
}

/**
 * Freeform generator benchmark: per size, how many seeds produce
 * a puzzle on the first attempt and how many within the normal limit. At the end, the letters
 * least frequent in the bank, which is where crossing words are missing.
 */
private fun bench(seeds: Int) {
    val bank = ClueBank.parse(findBank().readText(Charsets.UTF_8))
    val generator = FreeformGenerator(bank)
    println("Bank: ${bank.size} answers")
    println("size   1st attempt   within limit  mean ms")
    for (size in listOf(5, 7, 9, 11, 13)) {
        var first = 0
        var within = 0
        val started = System.nanoTime()
        for (seed in 1..seeds) {
            val spec = GenSpec(
                maxW = size,
                maxH = size,
                minWords = size - 1,
                maxWords = size + 2,
                difficulties = setOf(Difficulty.EASY, Difficulty.MEDIUM),
                rngSeed = seed,
            )
            if (tryGenerate(generator, spec, maxAttempts = 1)) first++
            if (tryGenerate(generator, spec, maxAttempts = 200)) within++
        }
        val millis = (System.nanoTime() - started) / 1_000_000.0 / (seeds * 2)
        println(String.format("%2dx%-2d  %5.1f %%      %5.1f %%    %6.1f", size, size, 100.0 * first / seeds, 100.0 * within / seeds, millis))
    }
    println("\nLeast frequent letters in the answers (how many answers contain them):")
    val counts = Alphabet.CHARS.associateWith { c -> bank.entries.count { c in it.answer } }
    counts.entries.sortedBy { it.value }.take(10).forEach { (c, n) -> println("  $c  $n") }
    println("\nAnswers by length (3–5 letters):")
    for (n in 3..5) println("  $n letters: ${bank.entries.count { it.answer.length == n }}")
}

private fun tryGenerate(generator: FreeformGenerator, spec: GenSpec, maxAttempts: Int): Boolean =
    try {
        generator.generate(spec, maxAttempts = maxAttempts)
        true
    } catch (e: GenerationException) {
        false
    }

/** Finds the compiled bank from the current directory, walking up to the project root. */
private fun findBank(): File {
    var dir: File? = File("").absoluteFile
    while (dir != null) {
        val candidate = File(dir, "app/src/main/assets/clues/pt-PT.json")
        if (candidate.exists()) return candidate
        dir = dir.parentFile
    }
    error("Compiled bank app/src/main/assets/clues/pt-PT.json not found above ${File("").absolutePath}")
}

/** Prints the five reference grids (seeds 1–5, 9×9) in the test snapshot format. */
internal fun snapshot(bank: ClueBank): String {
    val generator = FreeformGenerator(bank)
    return (1..5).joinToString("\n") { seed ->
        val spec = GenSpec(maxW = 9, maxH = 9, minWords = 8, maxWords = 11, difficulties = setOf(Difficulty.EASY, Difficulty.MEDIUM), rngSeed = seed)
        generator.generate(spec).toSnapshot()
    }
}
