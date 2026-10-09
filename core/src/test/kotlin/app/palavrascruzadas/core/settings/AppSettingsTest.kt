package app.palavrascruzadas.core.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {
    @Test
    fun defaultsMatchThePlan() {
        val s = AppSettings()
        assertTrue(s.confirmWords)
        assertFalse(s.autoCheck)
        assertFalse(s.acceptUnaccented)
        assertTrue(s.skipFilled)
        assertEquals(100, s.gridScale)
        assertEquals(null, s.reduceMotion)
    }

    @Test
    fun playOptionsFollowTheGameSettings() {
        val s = AppSettings(confirmWords = false, autoCheck = true, acceptUnaccented = true, skipFilled = false)
        val o = s.playOptions()
        assertFalse(o.confirmWords)
        assertTrue(o.autoCheck)
        assertTrue(o.acceptUnaccented)
        assertFalse(o.skipFilled)
    }

    @Test
    fun gridScaleStaysBetweenNinetyAndOneHundredThirty() {
        assertEquals(130, AppSettings(gridScale = 130).withGridScale(AppSettings.SCALE_STEP).gridScale)
        assertEquals(90, AppSettings(gridScale = 90).withGridScale(-AppSettings.SCALE_STEP).gridScale)
        assertEquals(110, AppSettings().withGridScale(AppSettings.SCALE_STEP).gridScale)
    }

    @Test
    fun reminderIsOffByDefaultAndDefaultsToEight() {
        assertEquals(null, AppSettings().reminderHour)
        assertEquals(20, AppSettings.DEFAULT_REMINDER_HOUR)
    }
}
