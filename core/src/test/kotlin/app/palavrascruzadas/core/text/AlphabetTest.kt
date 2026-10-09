package app.palavrascruzadas.core.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlphabetTest {

    @Test
    fun `alphabet has 26 letters plus 12 accented forms`() {
        assertEquals(38, Alphabet.CHARS.length)
        assertEquals(38, Alphabet.CHARS.toSet().size)
    }

    @Test
    fun `k w and y are valid`() {
        assertTrue(Alphabet.isValidAnswer("KIWI"))
        assertTrue(Alphabet.isValidAnswer("YOGA"))
    }

    @Test
    fun `accented letters are valid`() {
        assertTrue(Alphabet.isValidAnswer("ÁÀÂÃÉÊÍÓÔÕÚÇ"))
    }

    @Test
    fun `hyphen space apostrophe and umlaut are rejected`() {
        assertFalse(Alphabet.isValidAnswer("GUARDA-ROUPA"))
        assertFalse(Alphabet.isValidAnswer("PÃO DE LÓ"))
        assertFalse(Alphabet.isValidAnswer("D'ÁGUA"))
        assertFalse(Alphabet.isValidAnswer("PINGÜIM"))
    }

    @Test
    fun `empty string is rejected`() {
        assertFalse(Alphabet.isValidAnswer(""))
    }

    @Test
    fun `lowercase is not in the alphabet, normalize first`() {
        assertFalse(Alphabet.contains('a'))
        assertTrue(Alphabet.isValidAnswer(Normalize.answer("ação")))
    }
}
