package app.palavrascruzadas.core.daily

import app.palavrascruzadas.core.gen.loadRealBank
import app.palavrascruzadas.core.model.PuzzleKind
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DailyPuzzleTest {
    private val bank = loadRealBank()

    @Test
    fun preGeneratedRangeIsTwoYears() {
        assertEquals(0, DailyPuzzle.preGeneratedIndex(LocalDate.of(2027, 1, 1)))
        // 730 days from 2027-01-01: the last one is 2028-12-30 (index 729).
        assertEquals(729, DailyPuzzle.preGeneratedIndex(LocalDate.of(2028, 12, 30)))
        assertNull(DailyPuzzle.preGeneratedIndex(LocalDate.of(2028, 12, 31)))
        assertNull(DailyPuzzle.preGeneratedIndex(LocalDate.of(2026, 12, 31)))
    }

    @Test
    fun theDayAfterTheLastPreGeneratedOneIsGeneratedOnThePhone() {
        // 2029-01-01 is not in the file: it must come from on-device generation.
        assertNull(DailyPuzzle.preGeneratedIndex(LocalDate.of(2029, 1, 1)))
        val puzzle = DailyPuzzle.generate(bank, LocalDate.of(2029, 1, 1))
        assertEquals("d20290101", puzzle.id)
        assertEquals(9, puzzle.width)
    }

    @Test
    fun generationIsDeterministicForTheSameDay() {
        val first = DailyPuzzle.generate(bank, LocalDate.of(2031, 3, 5))
        val second = DailyPuzzle.generate(bank, LocalDate.of(2031, 3, 5))
        assertEquals(first, second)
    }

    @Test
    fun sundaysHaveAKeywordAndOtherDaysDoNot() {
        val sunday = LocalDate.of(2029, 1, 7)
        assertEquals(true, DailyPuzzle.isKeywordDay(sunday))
        val sundayPuzzle = DailyPuzzle.generate(bank, sunday)
        assertEquals(PuzzleKind.KEYWORD, sundayPuzzle.kind)
        assertNotNull(sundayPuzzle.keyword)

        val monday = LocalDate.of(2029, 1, 1)
        assertEquals(false, DailyPuzzle.isKeywordDay(monday))
        assertEquals(PuzzleKind.CLASSIC, DailyPuzzle.generate(bank, monday).kind)
    }
}
