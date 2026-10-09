package app.palavrascruzadas.core.campaign

import app.palavrascruzadas.core.model.Difficulty
import app.palavrascruzadas.core.model.Dir
import app.palavrascruzadas.core.model.ClueCell
import app.palavrascruzadas.core.model.Entry
import app.palavrascruzadas.core.model.Keyword
import app.palavrascruzadas.core.model.Pos
import app.palavrascruzadas.core.model.Puzzle
import app.palavrascruzadas.core.model.PuzzleKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** A campaign level as stored in the file. */
data class LevelSpec(
    val number: Int,
    val position: Int,
    val type: LevelType,
    val puzzle: Puzzle,
    /** Wheel only: the base word and the bonus words. */
    val base: String? = null,
    val bonus: List<String> = emptyList(),
    /** Legacy field: there are no pending levels any more. */
    val todoF9: Boolean = false,
)

/** A world with its 40 levels. */
data class WorldLevels(val number: Int, val name: String, val theme: String, val levels: List<LevelSpec>)

/**
 * Reads the level files in `app/src/main/assets/levels/` (compact format).
 * The level type comes from its position; the file's `k` only says the puzzle class.
 */
object LevelFormat {

    fun parseWorld(json: String): WorldLevels {
        val root = Json.parseToJsonElement(json).jsonObject
        require(root.getValue("v").jsonPrimitive.int == VERSION) { "Unsupported level format" }
        val number = root.getValue("mundo").jsonPrimitive.int
        val levels = root.getValue("niveis").jsonArray.mapIndexed { index, element ->
            parseLevel(element.jsonObject, position = index + 1)
        }
        require(levels.size == WorldTable.LEVELS_PER_WORLD) { "World $number has ${levels.size} levels" }
        return WorldLevels(
            number = number,
            name = root.getValue("nome").jsonPrimitive.content,
            theme = root.getValue("tema").jsonPrimitive.content,
            levels = levels,
        )
    }

    /** Pre-generated daily puzzles (`levels/daily.json`), in date order. */
    fun parsePuzzles(json: String): List<Puzzle> = parseSpecs(json).map { it.puzzle }

    /** Levels of a puzzle file (daily or book), with the wheel fields. */
    fun parseSpecs(json: String): List<LevelSpec> {
        val root = Json.parseToJsonElement(json).jsonObject
        require(root.getValue("v").jsonPrimitive.int == VERSION) { "Unsupported puzzle file format" }
        return root.getValue("puzzles").jsonArray.map { parseLevel(it.jsonObject, position = 1) }
    }

    /** One level: `position` is the 1–40 position in the world; the global number comes from the file. */
    fun parseLevel(obj: JsonObject, position: Int): LevelSpec {
        val id = obj.getValue("id").jsonPrimitive.content
        // Campaign levels have id "cNNN"; daily puzzles have "dYYYYMMDD" and no level number.
        val number = id.removePrefix("c").toIntOrNull() ?: position
        val entries = obj.getValue("e").jsonArray.map { parseEntry(it.jsonArray) }
        val kind = when (obj.getValue("k").jsonPrimitive.content) {
            "C" -> PuzzleKind.CLASSIC
            "K" -> PuzzleKind.KEYWORD
            "B" -> PuzzleKind.BOSS
            "D" -> PuzzleKind.DIRETAS
            "W" -> PuzzleKind.WHEEL
            "F" -> PuzzleKind.FIT_IT
            else -> throw IllegalArgumentException("Unknown kind in $id")
        }
        val keyword = obj["kw"]?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.jsonObject?.let { kw ->
            Keyword(
                word = kw.getValue("w").jsonPrimitive.content,
                cells = kw.getValue("c").jsonArray.map { pair ->
                    val p = pair.jsonArray
                    Pos(p[0].jsonPrimitive.int, p[1].jsonPrimitive.int)
                },
            )
        }
        val puzzle = Puzzle(
            id = id,
            kind = kind,
            width = obj.getValue("w").jsonPrimitive.int,
            height = obj.getValue("h").jsonPrimitive.int,
            entries = entries,
            keyword = keyword,
            givens = obj["g"]?.jsonArray?.map { pair -> Pos(pair.jsonArray[0].jsonPrimitive.int, pair.jsonArray[1].jsonPrimitive.int) }?.toSet().orEmpty(),
            clueCells = obj["cl"]?.jsonArray?.map { c ->
                val f = c.jsonArray
                ClueCell(
                    pos = Pos(f[0].jsonPrimitive.int, f[1].jsonPrimitive.int),
                    arrows = f[2].jsonPrimitive.content.map { if (it == 'H') Dir.ACROSS else Dir.DOWN }.toSet(),
                )
            }.orEmpty(),
            theme = obj["th"]?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.jsonPrimitive?.content,
            difficulty = Difficulty.fromCode(obj.getValue("d").jsonPrimitive.int),
            targetSeconds = obj.getValue("td").jsonPrimitive.int,
        )
        return LevelSpec(
            number = number,
            position = position,
            type = WorldTable.typeAt(position),
            puzzle = puzzle,
            base = obj["base"]?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.jsonPrimitive?.content,
            bonus = obj["bonus"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty(),
        )
    }

    private fun parseEntry(fields: JsonArray): Entry {
        val short = fields[6].takeIf { it !is kotlinx.serialization.json.JsonNull }?.jsonPrimitive?.content
        return Entry(
            number = fields[0].jsonPrimitive.int,
            row = fields[1].jsonPrimitive.int,
            col = fields[2].jsonPrimitive.int,
            dir = if (fields[3].jsonPrimitive.content == "H") Dir.ACROSS else Dir.DOWN,
            answer = fields[4].jsonPrimitive.content,
            clue = fields[5].jsonPrimitive.content,
            shortClue = short,
        )
    }

    private const val VERSION = 1
}
