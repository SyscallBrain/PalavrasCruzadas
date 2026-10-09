package app.palavrascruzadas.ui.game

import org.junit.Assert.assertEquals
import org.junit.Test

class AccessibilityTest {
    @Test
    fun plainLettersAreSpokenInLowerCase() {
        assertEquals("a", keyDescription('A'))
        assertEquals("z", keyDescription('Z'))
    }

    @Test
    fun accentedKeysHaveTheirPortugueseNames() {
        assertEquals("a com til", keyDescription('Ã'))
        assertEquals("c com cedilha", keyDescription('Ç'))
        assertEquals("e com acento agudo", keyDescription('É'))
    }

    @Test
    fun everyAccentedVowelHasAName() {
        listOf('Á', 'À', 'Â', 'Ã', 'É', 'Ê', 'Í', 'Ó', 'Ô', 'Õ', 'Ú', 'Ç').forEach { c ->
            assertEquals(false, keyDescription(c).length == 1)
        }
    }
}
