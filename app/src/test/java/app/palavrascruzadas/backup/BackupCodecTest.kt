package app.palavrascruzadas.backup

import app.palavrascruzadas.core.campaign.CampaignProgress
import app.palavrascruzadas.core.campaign.LevelResult
import app.palavrascruzadas.core.play.HintWallet
import app.palavrascruzadas.core.settings.AppSettings
import app.palavrascruzadas.data.ProgressData
import app.palavrascruzadas.data.StatsData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupCodecTest {
    private fun sample(): BackupFile = BackupFile(
        exported = "2026-10-09T12:00:00Z",
        progress = ProgressData(
            progress = CampaignProgress(mapOf(1 to LevelResult(stars = 3, bestSeconds = 95), 2 to LevelResult(stars = 2))),
            wallet = HintWallet(17),
            miniBestSeconds = 42,
        ),
        stats = StatsData(puzzlesDone = 5, wordsSolved = 60, unlocked = setOf("primeira-grelha")),
        settings = AppSettings(themeId = "tinta-ambar", gridScale = 120),
        savedGames = emptyList(),
    )

    @Test
    fun roundTripKeepsEverything() {
        val file = sample()
        assertEquals(file, BackupCodec.decode(BackupCodec.encode(file)))
    }

    @Test
    fun headerCarriesAppNameAndSchema() {
        val text = BackupCodec.encode(sample())
        assertTrue(text.contains("\"app\": \"palavrascruzadas\""))
        assertTrue(text.contains("\"schema\": 1"))
    }

    @Test
    fun summaryCountsLevelsStarsAndStreak() {
        val s = BackupCodec.summary(sample())
        assertEquals(2, s.levels)
        assertEquals(5, s.stars)
        assertEquals(0, s.streak)
    }

    @Test
    fun refusesAFileFromAnotherApp() {
        val text = BackupCodec.encode(sample()).replace("\"app\": \"palavrascruzadas\"", "\"app\": \"sopadeletras\"")
        try {
            BackupCodec.decode(text)
            fail("should refuse")
        } catch (e: BackupException) {
            assertTrue(e.message!!.contains("outra aplicação"))
        }
    }

    @Test
    fun refusesAFileFromANewerSchema() {
        val text = BackupCodec.encode(sample()).replace("\"schema\": 1", "\"schema\": 2")
        try {
            BackupCodec.decode(text)
            fail("should refuse")
        } catch (e: BackupException) {
            assertTrue(e.message!!.contains("mais nova"))
        }
    }

    @Test
    fun refusesGarbage() {
        try {
            BackupCodec.decode("isto não é json")
            fail("should refuse")
        } catch (e: BackupException) {
            assertTrue(e.message!!.contains("não é uma cópia"))
        }
    }
}
