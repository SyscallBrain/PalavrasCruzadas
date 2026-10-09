package app.palavrascruzadas.levelgen

import app.palavrascruzadas.core.bank.ClueBank
import app.palavrascruzadas.core.books.Books
import app.palavrascruzadas.core.campaign.LevelFormat
import app.palavrascruzadas.core.campaign.LevelSpec
import app.palavrascruzadas.core.campaign.LevelType
import app.palavrascruzadas.core.campaign.WorldTable
import app.palavrascruzadas.core.gen.FitIt
import app.palavrascruzadas.core.model.Dir
import app.palavrascruzadas.core.model.Pos
import app.palavrascruzadas.core.model.PuzzleKind
import java.io.File
import java.time.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests over the committed level assets. They read the files
 * in app/src/main/assets/levels/ and the compiled bank; the tests' working dir is levelgen/.
 * Puzzles are read with [LevelFormat], which already validates each one (crossings, clue cells…).
 */
class LevelAssetsTest {
    private val levelsDir = File("../app/src/main/assets/levels")
    private val bank = ClueBank.parse(File("../app/src/main/assets/clues/pt-PT.json").readText(Charsets.UTF_8))

    private fun raw(path: String): JsonObject = Json.parseToJsonElement(File(levelsDir, path).readText(Charsets.UTF_8)).jsonObject

    private fun world(n: Int): List<LevelSpec> = LevelFormat.parseWorld(File(levelsDir, "world_%02d.json".format(n)).readText(Charsets.UTF_8)).levels

    private fun book(theme: String): List<LevelSpec> = LevelFormat.parseSpecs(File(levelsDir, "books/$theme.json").readText(Charsets.UTF_8))

    private fun allCampaign(): List<LevelSpec> = (1..15).flatMap { world(it) }

    @Test
    fun campaignHasFifteenWorldsOfFortyLevels() {
        val levels = allCampaign()
        assertEquals(600, levels.size)
        assertEquals((1..600).toList(), levels.map { it.number })
    }

    @Test
    fun kindsFollowSection82AndNoLevelIsPlaceholder() {
        for (spec in allCampaign()) {
            val expected = when (spec.position) {
                5, 15, 35 -> PuzzleKind.WHEEL
                20 -> PuzzleKind.DIRETAS
                25 -> PuzzleKind.FIT_IT
                10, 30 -> PuzzleKind.KEYWORD
                WorldTable.LEVELS_PER_WORLD -> PuzzleKind.BOSS
                else -> PuzzleKind.CLASSIC
            }
            assertEquals("nível ${spec.number}", expected, spec.puzzle.kind)
            assertEquals(false, spec.todoF9)
        }
    }

    @Test
    fun wheelLevelsHaveABaseAndBonusWordsOutsideTheGrid() {
        for (spec in allCampaign().filter { it.puzzle.kind == PuzzleKind.WHEEL }) {
            val base = spec.base!!
            assertTrue("nível ${spec.number}: base na grelha", spec.puzzle.entries.any { it.answer == base })
            assertTrue("nível ${spec.number}: 4 a 8 entradas", spec.puzzle.entries.size in 4..8)
            assertTrue("nível ${spec.number}: bónus fora da grelha", spec.bonus.none { b -> spec.puzzle.entries.any { it.answer == b } })
        }
    }

    @Test
    fun diretasHaveAClueCellBeforeEachEntry() {
        for (spec in allCampaign().filter { it.puzzle.kind == PuzzleKind.DIRETAS }) {
            spec.puzzle.entries.forEach { e ->
                val before = if (e.dir == Dir.ACROSS) Pos(e.row, e.col - 1) else Pos(e.row - 1, e.col)
                assertTrue("nível ${spec.number}: casa-pista antes de ${e.answer}", spec.puzzle.clueCells.any { it.pos == before })
            }
            assertTrue("nível ${spec.number}: lado até 9", spec.puzzle.width <= 9 && spec.puzzle.height <= 9)
        }
    }

    @Test
    fun encaixarPuzzlesHaveASingleSolution() {
        val fit = allCampaign().filter { it.puzzle.kind == PuzzleKind.FIT_IT }.map { it.puzzle } +
            (listOf("roda", "diretas", "encaixar")).flatMap { book(it) }.filter { it.puzzle.kind == PuzzleKind.FIT_IT }.map { it.puzzle }
        assertTrue("há encaixar", fit.isNotEmpty())
        fit.forEach { puzzle ->
            assertEquals("${puzzle.id}: solução única", 1, FitIt.solutionCount(puzzle))
        }
    }

    @Test
    fun everyAnswerAndClueComesFromTheBank() {
        for (spec in allCampaign()) {
            if (spec.puzzle.kind == PuzzleKind.FIT_IT) continue
            for (entry in spec.puzzle.entries) {
                val bankEntry = bank.entry(entry.answer)
                assertTrue("resposta fora do banco: ${entry.answer}", bankEntry != null)
                assertTrue(
                    "pista fora do banco para ${entry.answer}",
                    bankEntry!!.clues.any { it.text == entry.clue || it.short == entry.clue },
                )
            }
        }
    }

    @Test
    fun answersAreUniqueWithinEachLevelAndEachWorld() {
        for (n in 1..15) {
            val levels = world(n)
            val worldAnswers = mutableListOf<String>()
            for (level in levels) {
                val answers = level.puzzle.entries.map { it.answer }
                assertEquals("repetida no nível ${level.number}", answers.size, answers.toSet().size)
                worldAnswers += answers
            }
            assertEquals("resposta repetida no mundo $n", worldAnswers.size, worldAnswers.toSet().size)
        }
    }

    @Test
    fun answersAreUsedAtMostFourTimesInTheCampaign() {
        val counts = HashMap<String, Int>()
        allCampaign().forEach { spec -> spec.puzzle.entries.forEach { counts[it.answer] = (counts[it.answer] ?: 0) + 1 } }
        assertTrue("resposta usada mais de 4 vezes: ${counts.maxByOrNull { it.value }}", counts.values.max() <= 4)
    }

    @Test
    fun keywordCellsSpellTheWordOnTheGrid() {
        for (spec in allCampaign() + book("roda") + book("diretas")) {
            val keyword = spec.puzzle.keyword ?: continue
            assertEquals("${spec.puzzle.id}: casas da palavra-chave", keyword.word.length, keyword.cells.size)
        }
        assertTrue("há palavras-chave", allCampaign().count { it.puzzle.keyword != null } >= 30)
    }

    @Test
    fun dailyHasSevenHundredThirtyDaysWithKeywordOnSundays() {
        val puzzles = LevelFormat.parsePuzzles(File(levelsDir, "daily.json").readText(Charsets.UTF_8))
        assertEquals(730, puzzles.size)
        val start = LocalDate.of(2027, 1, 1)
        puzzles.forEachIndexed { i, puzzle ->
            val date = start.plusDays(i.toLong())
            assertEquals("d" + date.toString().replace("-", ""), puzzle.id)
            val expected = if (date.dayOfWeek == java.time.DayOfWeek.SUNDAY) PuzzleKind.KEYWORD else PuzzleKind.CLASSIC
            assertEquals("data $date", expected, puzzle.kind)
            assertEquals(9, puzzle.width)
        }
    }

    @Test
    fun bookshelfHasFourteenThemesOfTwentyFiveAndThreeSpecialBooksOfSixty() {
        assertEquals(14, Books.THEMES.size)
        for (theme in Books.THEMES) {
            val puzzles = book(theme)
            assertEquals("caderno $theme", Books.PUZZLES_PER_BOOK, puzzles.size)
            puzzles.forEach { p ->
                assertTrue("$theme ${p.puzzle.id} lado ${p.puzzle.width}", p.puzzle.width in 7..11)
            }
        }
        for (special in Books.SPECIAL) {
            val puzzles = book(special)
            assertEquals("caderno $special", Books.PUZZLES_PER_SPECIAL, puzzles.size)
        }
        assertEquals(60, book("roda").count { it.puzzle.kind == PuzzleKind.WHEEL })
        assertEquals(60, book("diretas").count { it.puzzle.kind == PuzzleKind.DIRETAS })
        assertEquals(60, book("encaixar").count { it.puzzle.kind == PuzzleKind.FIT_IT })
    }

    @Test
    fun assetsStayUnderFourMegabytes() {
        val total = levelsDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        assertTrue("assets com $total bytes", total <= 4L * 1024 * 1024)
    }
}
