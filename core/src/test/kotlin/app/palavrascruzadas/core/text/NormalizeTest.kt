package app.palavrascruzadas.core.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NormalizeTest {

    @Test
    fun `NFD becomes NFC`() {
        // A + combining tilde (NFD) must become precomposed Ã (NFC).
        assertEquals("Ã", Normalize.answer("Ã"))
        assertEquals("AÇÃO", Normalize.answer("ação"))
    }

    @Test
    fun `lowercase becomes uppercase with cedilla and tilde`() {
        assertEquals("AÇÃO", Normalize.answer("ação"))
        assertEquals("AÇÃO", Normalize.answer("AÇÃO"))
    }

    @Test
    fun `all accented vowels and cedilla uppercase`() {
        assertEquals("ÁÀÂÃÉÊÍÓÔÕÚÇ", Normalize.answer("áàâãéêíóôõúç"))
    }

    @Test
    fun `trims surrounding spaces`() {
        assertEquals("MAR", Normalize.answer("  mar "))
    }

    @Test
    fun `base removes diacritics`() {
        assertEquals('A', Normalize.base('Ã'))
        assertEquals('C', Normalize.base('Ç'))
        assertEquals('O', Normalize.base('Ô'))
        assertEquals('B', Normalize.base('B'))
    }

    @Test
    fun `strict matching requires the exact accent`() {
        assertTrue(Normalize.matches("AÇÃO", "ação"))
        assertFalse(Normalize.matches("ACAO", "AÇÃO"))
        assertFalse(Normalize.matches("AÇAO", "AÇÃO"))
    }

    @Test
    fun `lenient matching accepts unaccented letter for accented cell only`() {
        assertTrue(Normalize.matches("A", "Ã", lenient = true))
        assertTrue(Normalize.matches("C", "Ç", lenient = true))
        // An accent typed in a cell without an accent is never accepted.
        assertFalse(Normalize.matches("Ã", "A", lenient = true))
        assertFalse(Normalize.matches("A", "Ã", lenient = false))
    }

    @Test
    fun `different lengths never match`() {
        assertFalse(Normalize.matches("MAR", "MARES", lenient = true))
    }
}
