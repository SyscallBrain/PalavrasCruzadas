package app.palavrascruzadas.core.achievements

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementsTest {
    @Test
    fun nothingUnlockedAtTheStart() {
        val unlocked = Achievement.unlocked(AchievementStats())
        assertFalse(Achievement.FIRST_GRID in unlocked)
        assertFalse(Achievement.CAMPAIGN_10 in unlocked)
    }

    @Test
    fun firstPuzzleUnlocksTheFirstGridAchievement() {
        assertTrue(Achievement.FIRST_GRID in Achievement.unlocked(AchievementStats(puzzlesDone = 1)))
    }

    @Test
    fun campaignThresholds() {
        val at10 = Achievement.unlocked(AchievementStats(levelsDone = 10))
        assertTrue(Achievement.CAMPAIGN_10 in at10)
        assertFalse(Achievement.CAMPAIGN_100 in at10)
        assertTrue(Achievement.CAMPAIGN_600 !in Achievement.unlocked(AchievementStats(levelsDone = 599)))
    }

    @Test
    fun worldWithAHundredTwentyStarsIsPerfect() {
        assertTrue(Achievement.PERFECT_WORLD in Achievement.unlocked(AchievementStats(bestWorldStars = 120)))
        assertFalse(Achievement.PERFECT_WORLD in Achievement.unlocked(AchievementStats(bestWorldStars = 119)))
    }

    @Test
    fun dailyStreakThresholds() {
        val s = AchievementStats(dailyMaxStreak = 7)
        val u = Achievement.unlocked(s)
        assertTrue(Achievement.DAILY_7 in u)
        assertFalse(Achievement.DAILY_30 in u)
    }

    @Test
    fun miniUnderSixtySecondsNeedsARecord() {
        assertFalse(Achievement.MINI_60 in Achievement.unlocked(AchievementStats()))
        assertTrue(Achievement.MINI_60 in Achievement.unlocked(AchievementStats(miniBestSeconds = 59)))
        assertFalse(Achievement.MINI_60 in Achievement.unlocked(AchievementStats(miniBestSeconds = 60)))
    }

    @Test
    fun timeAttackRecords() {
        val u = Achievement.unlocked(AchievementStats(timeAttackBest = 25))
        assertTrue(Achievement.TIME_ATTACK_25 in u)
        assertTrue(Achievement.TIME_ATTACK_10 in u)
    }

    @Test
    fun everyModeOnceUnlocksTheModesAchievement() {
        val partial = AchievementStats(modesUsed = AchievementStats.MODES - "roda")
        assertFalse(Achievement.ONE_OF_EACH in Achievement.unlocked(partial))
        assertTrue(Achievement.ONE_OF_EACH in Achievement.unlocked(partial.copy(modesUsed = AchievementStats.MODES)))
    }

    @Test
    fun allFiveVisualThemesUnlockTheThemesAchievement() {
        val four = AchievementStats(visualThemesUsed = setOf("a", "b", "c", "d"))
        assertFalse(Achievement.ALL_THEMES in Achievement.unlocked(four))
        assertTrue(Achievement.ALL_THEMES in Achievement.unlocked(four.copy(visualThemesUsed = setOf("a", "b", "c", "d", "e"))))
    }

    @Test
    fun achievementIdsAreUnique() {
        assertEquals(Achievement.entries.size, Achievement.entries.map { it.id }.toSet().size)
    }
}
