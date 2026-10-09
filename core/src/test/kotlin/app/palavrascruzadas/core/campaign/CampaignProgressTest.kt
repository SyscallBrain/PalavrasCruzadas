package app.palavrascruzadas.core.campaign

import app.palavrascruzadas.core.play.HintWallet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CampaignProgressTest {
    private val bossPosition = 40

    @Test
    fun onlyLevelOneIsOpenAtTheStart() {
        val empty = CampaignProgress()
        assertTrue(Campaign.isUnlocked(1, empty))
        assertFalse(Campaign.isUnlocked(2, empty))
        assertEquals(1, empty.currentLevel)
    }

    @Test
    fun nextLevelOpensWhenThePreviousIsDone() {
        val progress = CampaignProgress(mapOf(1 to LevelResult(stars = 1), 2 to LevelResult(stars = 2)))
        assertTrue(Campaign.isUnlocked(3, progress))
        assertFalse(Campaign.isUnlocked(4, progress))
        assertEquals(3, progress.currentLevel)
    }

    @Test
    fun worldTwoOpensWithLevelFortyOfWorldOne() {
        val upToFortyOne = CampaignProgress((1..40).associateWith { LevelResult(stars = 1) })
        assertTrue(Campaign.isUnlocked(41, upToFortyOne))
        assertFalse(Campaign.isUnlocked(42, upToFortyOne))
    }

    @Test
    fun currentLevelIsTheLastOneWhenEverythingIsDone() {
        val all = CampaignProgress((1..Campaign.TOTAL_LEVELS).associateWith { LevelResult(stars = 3) })
        assertEquals(Campaign.TOTAL_LEVELS, all.currentLevel)
    }

    @Test
    fun threeStarRewardIsGivenOnlyTheFirstTime() {
        val (first, wallet1) = Campaign.complete(CampaignProgress(), HintWallet(10), number = 5, position = 5, stars = 3, seconds = 120)
        assertEquals(11, wallet1.balance)
        val (_, wallet2) = Campaign.complete(first, wallet1, number = 5, position = 5, stars = 3, seconds = 100)
        assertEquals(11, wallet2.balance)
    }

    @Test
    fun twoStarsGiveNoReward() {
        val (_, wallet) = Campaign.complete(CampaignProgress(), HintWallet(10), number = 5, position = 5, stars = 2, seconds = 120)
        assertEquals(10, wallet.balance)
    }

    @Test
    fun bossRewardIsGivenOnceAndWhateverTheStars() {
        val (first, wallet1) = Campaign.complete(CampaignProgress(), HintWallet(10), number = 40, position = bossPosition, stars = 2, seconds = 300)
        assertEquals(13, wallet1.balance)
        val (_, wallet2) = Campaign.complete(first, wallet1, number = 40, position = bossPosition, stars = 3, seconds = 200)
        // 3 stars give +1 the first time; the boss gives nothing more.
        assertEquals(14, wallet2.balance)
    }

    @Test
    fun keepsBestStarsAndBestTime() {
        val (first, w1) = Campaign.complete(CampaignProgress(), HintWallet(10), number = 7, position = 7, stars = 2, seconds = 200)
        val (second, _) = Campaign.complete(first, w1, number = 7, position = 7, stars = 1, seconds = 90)
        val result = second.levels.getValue(7)
        assertEquals(2, result.stars)
        assertEquals(90, result.bestSeconds)
    }

    @Test
    fun balanceNeverGoesAboveNinetyNine() {
        val (_, wallet) = Campaign.complete(CampaignProgress(), HintWallet(98), number = 40, position = bossPosition, stars = 3, seconds = 10)
        assertEquals(HintWallet.MAX, wallet.balance)
    }
}
