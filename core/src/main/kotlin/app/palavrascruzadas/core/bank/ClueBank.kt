package app.palavrascruzadas.core.bank

import app.palavrascruzadas.core.model.Clue
import app.palavrascruzadas.core.model.Difficulty
import app.palavrascruzadas.core.text.Normalize
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** One answer from the bank with its clues. */
data class BankEntry(
    val answer: String,
    val lemma: String,
    val themes: List<String>,
    val clues: List<Clue>,
)

/**
 * Clue bank read from the JSON compiled by tools/compile_bank.py.
 * Format: {"v":1,"entries":[[answer, lemma, [themes], [[text, difficulty, short|null], ...]], ...]}
 */
class ClueBank private constructor(val entries: List<BankEntry>) {

    private val byAnswer: Map<String, BankEntry> = entries.associateBy { it.answer }

    val size: Int get() = entries.size

    fun entry(answer: String): BankEntry? = byAnswer[Normalize.answer(answer)]

    companion object {
        const val FORMAT_VERSION = 1

        fun parse(json: String): ClueBank {
            val root = Json.parseToJsonElement(json).jsonObject
            val version = root.getValue("v").jsonPrimitive.int
            require(version == FORMAT_VERSION) { "Unsupported bank format version $version" }
            val entries = root.getValue("entries").jsonArray.map { parseRow(it.jsonArray) }
            return ClueBank(entries)
        }

        private fun parseRow(row: JsonArray): BankEntry {
            require(row.size == 4) { "Bank row must have 4 fields, got ${row.size}" }
            return BankEntry(
                answer = row[0].jsonPrimitive.content,
                lemma = row[1].jsonPrimitive.content,
                themes = row[2].jsonArray.map { it.jsonPrimitive.content },
                clues = row[3].jsonArray.map { parseClue(it) },
            )
        }

        private fun parseClue(element: JsonElement): Clue {
            val fields = element.jsonArray
            require(fields.size == 3) { "Clue must have 3 fields, got ${fields.size}" }
            val short = fields[2].takeUnless { it == JsonNull }?.jsonPrimitive?.content
            return Clue(
                text = fields[0].jsonPrimitive.content,
                short = short,
                difficulty = Difficulty.fromCode(fields[1].jsonPrimitive.int),
            )
        }
    }
}
