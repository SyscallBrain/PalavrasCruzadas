package app.palavrascruzadas.core.e2e

import app.palavrascruzadas.core.campaign.Campaign
import app.palavrascruzadas.core.campaign.CampaignProgress
import app.palavrascruzadas.core.gen.FreeformGenerator
import app.palavrascruzadas.core.gen.GenSpec
import app.palavrascruzadas.core.gen.loadRealBank
import app.palavrascruzadas.core.model.Difficulty
import app.palavrascruzadas.core.play.Checker
import app.palavrascruzadas.core.play.HintWallet
import app.palavrascruzadas.core.play.InputLogic
import app.palavrascruzadas.core.play.PlayState
import app.palavrascruzadas.core.play.SavedGame
import app.palavrascruzadas.core.play.Scoring
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Full flow without UI: generate → type → check →
 * win → stars → progress and reward → game saved mid-way and resumed.
 */
class GameFlowTest {
    private val bank = loadRealBank()

    private fun puzzle(seed: Int) = FreeformGenerator(bank).generate(
        GenSpec(maxW = 7, maxH = 7, minWords = 6, maxWords = 9, difficulties = setOf(Difficulty.EASY, Difficulty.MEDIUM), rngSeed = seed),
    ).copy(id = "c001")

    @Test
    fun generateTypeVerifyWinScoreAndSaveProgress() {
        val p = puzzle(7)
        var state = PlayState(p)
        // An error in a cell: check word points it out, without a penalty.
        val firstEntry = p.entries.first()
        val wrongCell = firstEntry.cells.first()
        state = InputLogic.select(state, wrongCell)
        state = Checker.markFound(InputLogic.type(state, if (p.solution.getValue(wrongCell) == 'A') 'B' else 'A'))
        val (_, wrong) = Checker.verifyWord(state)
        assertTrue("a verificação aponta a casa errada", wrong.contains(wrongCell))

        // Type everything right, cell by cell.
        state = PlayState(p)
        for ((pos, letter) in p.solution.toSortedMap(compareBy({ it.row }, { it.col }))) {
            state = InputLogic.select(state, pos)
            state = Checker.markFound(InputLogic.type(state, letter))
        }
        assertTrue("o puzzle fica completo", state.isComplete)
        assertEquals(p.entries.size, state.found.size)

        // Stars: 3 when fast and without hints.
        val fast = Scoring.stars(elapsedSeconds = p.targetSeconds - 1, targetSeconds = p.targetSeconds, state = state)
        assertEquals(3, fast.stars)
        // Going past the target time costs a star.
        val slow = Scoring.stars(elapsedSeconds = p.targetSeconds + 1, targetSeconds = p.targetSeconds, state = state)
        assertEquals(2, slow.stars)

        // Progress and the first-time 3-star reward.
        val (progress, wallet) = Campaign.complete(CampaignProgress(), HintWallet(10), number = 1, position = 1, stars = fast.stars, seconds = p.targetSeconds - 1)
        assertEquals(11, wallet.balance)
        assertEquals(2, progress.currentLevel)
        assertTrue(Campaign.isUnlocked(2, progress))
    }

    @Test
    fun saveAHalfFinishedGameAndResumeItExactly() {
        val p = puzzle(11)
        var state = PlayState(p)
        val cells = p.solution.keys.sortedWith(compareBy({ it.row }, { it.col })).take(p.solution.size / 2)
        for (pos in cells) {
            state = InputLogic.select(state, pos)
            state = Checker.markFound(InputLogic.type(state, p.solution.getValue(pos)))
        }
        assertFalse(state.isComplete)
        val saved = SavedGame.capture(state, levelNumber = 1, elapsedSeconds = 33, updatedAt = 1L)
        val resumed = saved.restore(p, state.options)
        assertEquals(state.typed, resumed.typed)
        assertEquals(state.found, resumed.found)
        // Acabar a partir do estado retomado dá vitória.
        var finish = resumed
        for (pos in p.solution.keys.filter { finish.typed[it] == null }.sortedWith(compareBy({ it.row }, { it.col }))) {
            finish = InputLogic.select(finish, pos)
            finish = Checker.markFound(InputLogic.type(finish, p.solution.getValue(pos)))
        }
        assertTrue(finish.isComplete)
    }
}
