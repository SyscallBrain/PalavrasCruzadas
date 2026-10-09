package app.palavrascruzadas.core.play

import org.junit.Assert.assertEquals
import org.junit.Test

class ScoringTest {

    private fun state(hintPoints: Int = 0, autoCheck: Boolean = false): PlayState {
        // Each hint point comes from a grid check (costs 1 point).
        val s = sampleState(PlayOptions(autoCheck = autoCheck))
        return s.copy(gridChecks = hintPoints)
    }

    @Test
    fun `no hints and in time gives three stars`() {
        val r = Scoring.stars(elapsedSeconds = 50, targetSeconds = 60, state = state())
        assertEquals(3, r.stars)
        assertEquals(emptyList<String>(), r.lostReasons)
    }

    @Test
    fun `over time loses one star with a reason`() {
        val r = Scoring.stars(elapsedSeconds = 210, targetSeconds = 150, state = state())
        assertEquals(2, r.stars)
        assertEquals(listOf("Mais de 2:30"), r.lostReasons)
    }

    @Test
    fun `one hint point loses one star`() {
        val r = Scoring.stars(elapsedSeconds = 60, targetSeconds = 60, state = state(hintPoints = 1))
        assertEquals(2, r.stars)
        assertEquals(listOf("1 ajuda"), r.lostReasons)
    }

    @Test
    fun `four hint points lose two stars`() {
        val r = Scoring.stars(elapsedSeconds = 60, targetSeconds = 60, state = state(hintPoints = 4))
        assertEquals(1, r.stars)
        assertEquals(listOf("4 ajudas"), r.lostReasons)
    }

    @Test
    fun `over time plus four hints still gives at least one star`() {
        val r = Scoring.stars(elapsedSeconds = 500, targetSeconds = 60, state = state(hintPoints = 9))
        assertEquals(1, r.stars)
    }

    @Test
    fun `autocheck caps at two stars`() {
        val r = Scoring.stars(elapsedSeconds = 30, targetSeconds = 60, state = state(autoCheck = true))
        assertEquals(2, r.stars)
        assertEquals(listOf("Correção automática ligada"), r.lostReasons)
    }

    @Test
    fun `autocheck with two hints keeps the help reason and no extra loss`() {
        val r = Scoring.stars(elapsedSeconds = 30, targetSeconds = 60, state = state(hintPoints = 2, autoCheck = true))
        assertEquals(2, r.stars)
        assertEquals(listOf("2 ajudas"), r.lostReasons)
    }

    @Test
    fun `autocheck with four hints gives one star`() {
        val r = Scoring.stars(elapsedSeconds = 30, targetSeconds = 60, state = state(hintPoints = 4, autoCheck = true))
        assertEquals(1, r.stars)
    }

    @Test
    fun `time format is minutes and seconds`() {
        assertEquals("3:30", Scoring.formatTime(210))
        assertEquals("0:05", Scoring.formatTime(5))
    }
}
