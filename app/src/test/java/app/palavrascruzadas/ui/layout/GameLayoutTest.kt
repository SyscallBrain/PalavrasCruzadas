package app.palavrascruzadas.ui.layout

import org.junit.Assert.assertEquals
import org.junit.Test

class GameLayoutTest {
    @Test
    fun phoneInPortraitIsCompact() {
        assertEquals(GameLayout.COMPACT, chooseGameLayout(widthDp = 411f, heightDp = 891f))
        assertEquals(GameLayout.COMPACT, chooseGameLayout(widthDp = 360f, heightDp = 800f))
        assertEquals(GameLayout.COMPACT, chooseGameLayout(widthDp = 599f, heightDp = 1000f))
    }

    @Test
    fun phoneInLandscapeUsesTwoColumns() {
        assertEquals(GameLayout.TWO_COLUMNS, chooseGameLayout(widthDp = 891f, heightDp = 411f))
    }

    @Test
    fun mediumWidthUsesTwoColumnsInPortrait() {
        assertEquals(GameLayout.TWO_COLUMNS, chooseGameLayout(widthDp = 600f, heightDp = 960f))
        assertEquals(GameLayout.TWO_COLUMNS, chooseGameLayout(widthDp = 768f, heightDp = 1024f))
    }

    @Test
    fun squareWindowBelowMediumStaysCompact() {
        assertEquals(GameLayout.COMPACT, chooseGameLayout(widthDp = 500f, heightDp = 500f))
    }
}
