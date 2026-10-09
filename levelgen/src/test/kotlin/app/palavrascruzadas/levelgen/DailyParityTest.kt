package app.palavrascruzadas.levelgen

import app.palavrascruzadas.core.bank.ClueBank
import app.palavrascruzadas.core.campaign.LevelFormat
import app.palavrascruzadas.core.daily.DailyPuzzle
import java.io.File
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The phone generates the daily puzzle with the same code `levelgen` used for the
 * pre-generated file. For a few days, the generated puzzle must equal the one in the file.
 */
class DailyParityTest {
    @Test
    fun generatedDailyMatchesThePreGeneratedFile() {
        val bank = ClueBank.parse(File("../app/src/main/assets/clues/pt-PT.json").readText(Charsets.UTF_8))
        val file = LevelFormat.parsePuzzles(File("../app/src/main/assets/levels/daily.json").readText(Charsets.UTF_8))
        for (date in listOf(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 6, 6), LocalDate.of(2028, 12, 30))) {
            val index = DailyPuzzle.preGeneratedIndex(date)!!
            assertEquals("dia $date", file[index], DailyPuzzle.generate(bank, date))
        }
    }
}
