package app.palavrascruzadas.levelgen

import app.palavrascruzadas.core.campaign.World
import app.palavrascruzadas.core.model.Dir
import app.palavrascruzadas.core.model.Keyword
import app.palavrascruzadas.core.model.PuzzleKind
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Version of the level file format. Bump it when the format changes. */
private const val FORMAT = 1

/**
 * Level in the compact format: `{"id","k","w","h","td","d","th","e","kw","g","bonus","base","cl"}`.
 * `k`: C classic, K keyword, B boss, D diretas, W wheel, F fit-it. `cl` only for diretas,
 * `g` only for fit-it (given letters), `base` and `bonus` only for the wheel.
 */
internal fun levelJson(level: Level): JsonObject {
    val puzzle = level.puzzle
    val fields = linkedMapOf<String, JsonElement>(
        "id" to JsonPrimitive(puzzle.id),
        "k" to JsonPrimitive(kindCode(puzzle.kind)),
        "w" to JsonPrimitive(puzzle.width),
        "h" to JsonPrimitive(puzzle.height),
        "td" to JsonPrimitive(puzzle.targetSeconds),
        "d" to JsonPrimitive(puzzle.difficulty.code),
        "th" to (level.theme?.let { JsonPrimitive(it) } ?: JsonNull),
        "e" to JsonArray(puzzle.entries.map { e ->
            JsonArray(listOf(
                JsonPrimitive(e.number),
                JsonPrimitive(e.row),
                JsonPrimitive(e.col),
                JsonPrimitive(if (e.dir == Dir.ACROSS) "H" else "V"),
                JsonPrimitive(e.answer),
                JsonPrimitive(e.clue),
                e.shortClue?.let { JsonPrimitive(it) } ?: JsonNull,
            ))
        }),
        "kw" to (puzzle.keyword?.let { keywordJson(it) } ?: JsonNull),
        "g" to JsonArray(puzzle.givens.sortedWith(compareBy({ it.row }, { it.col })).map { p ->
            JsonArray(listOf(JsonPrimitive(p.row), JsonPrimitive(p.col)))
        }),
        "bonus" to JsonArray((level.bonus).map { JsonPrimitive(it) }),
        "base" to (level.base?.let { JsonPrimitive(it) } ?: JsonNull),
    )
    if (puzzle.clueCells.isNotEmpty()) {
        fields["cl"] = JsonArray(puzzle.clueCells.sortedWith(compareBy({ it.pos.row }, { it.pos.col })).map { c ->
            val arrows = c.arrows.sortedBy { it.ordinal }.joinToString("") { if (it == Dir.ACROSS) "H" else "V" }
            JsonArray(listOf(JsonPrimitive(c.pos.row), JsonPrimitive(c.pos.col), JsonPrimitive(arrows)))
        })
    }
    return JsonObject(fields)
}

private fun keywordJson(keyword: Keyword): JsonObject = JsonObject(linkedMapOf(
    "w" to JsonPrimitive(keyword.word),
    "c" to JsonArray(keyword.cells.map { JsonArray(listOf(JsonPrimitive(it.row), JsonPrimitive(it.col))) }),
))

private fun kindCode(kind: PuzzleKind): String = when (kind) {
    PuzzleKind.KEYWORD -> "K"
    PuzzleKind.BOSS -> "B"
    PuzzleKind.DIRETAS -> "D"
    PuzzleKind.WHEEL -> "W"
    PuzzleKind.FIT_IT -> "F"
    else -> "C"
}

/** File of a world: `world_XX.json`. */
internal fun worldFile(world: World, levels: List<Level>): String = JsonObject(linkedMapOf(
    "v" to JsonPrimitive(FORMAT),
    "mundo" to JsonPrimitive(world.number),
    "nome" to JsonPrimitive(world.name),
    "tema" to JsonPrimitive(world.theme),
    "niveis" to JsonArray(levels.map { levelJson(it) }),
)).toString() + "\n"

/** Daily file: `daily.json`. */
internal fun dailyFile(levels: List<Level>): String = JsonObject(linkedMapOf(
    "v" to JsonPrimitive(FORMAT),
    "inicio" to JsonPrimitive("2027-01-01"),
    "puzzles" to JsonArray(levels.map { levelJson(it) }),
)).toString() + "\n"

/** File of a book: `books/<theme>.json`. */
internal fun bookFile(theme: String, levels: List<Level>): String = JsonObject(linkedMapOf(
    "v" to JsonPrimitive(FORMAT),
    "tema" to JsonPrimitive(theme),
    "puzzles" to JsonArray(levels.map { levelJson(it) }),
)).toString() + "\n"
