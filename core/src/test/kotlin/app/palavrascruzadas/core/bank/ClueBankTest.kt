package app.palavrascruzadas.core.bank

import app.palavrascruzadas.core.model.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ClueBankTest {

    private val sample = """
        {"v":1,"entries":[
          ["MOLDE","MOLDE",["comida","casa"],[["Forma onde se verte a massa do bolo",1,"Forma"],["Padrão que serve de modelo",2,null]]],
          ["MAR","MAR",["mar"],[["Água salgada do oceano",1,null],["Extensão de água salgada",2,"Oceano"]]]
        ]}
    """.trimIndent()

    @Test
    fun `parses entries, themes and clues`() {
        val bank = ClueBank.parse(sample)
        assertEquals(2, bank.size)
        val molde = bank.entry("molde")!!
        assertEquals("MOLDE", molde.answer)
        assertEquals(listOf("comida", "casa"), molde.themes)
        assertEquals("Forma", molde.clues[0].short)
        assertEquals(Difficulty.EASY, molde.clues[0].difficulty)
    }

    @Test
    fun `null short clue becomes null`() {
        val molde = ClueBank.parse(sample).entry("MOLDE")!!
        assertNull(molde.clues[1].short)
        assertEquals(Difficulty.MEDIUM, molde.clues[1].difficulty)
    }

    @Test
    fun `unknown answer gives null`() {
        assertNull(ClueBank.parse(sample).entry("CASA"))
    }

    @Test
    fun `rejects unknown format version`() {
        assertThrows(IllegalArgumentException::class.java) {
            ClueBank.parse("""{"v":2,"entries":[]}""")
        }
    }

    @Test
    fun `rejects rows with the wrong shape`() {
        assertThrows(IllegalArgumentException::class.java) {
            ClueBank.parse("""{"v":1,"entries":[["MAR","MAR",["mar"]]]}""")
        }
    }

    @Test
    fun `word index finds words by length and letter position`() {
        val index = WordIndex(listOf("MOLDE", "MESA", "MAR", "SAL"))
        assertEquals(listOf("MESA"), index.ofLength(4))
        assertEquals(listOf("MOLDE"), index.withLetterAt(5, 2, 'L'))
        assertEquals(listOf("MESA"), index.withLetterAt(4, 1, 'E'))
        assertEquals(emptyList<String>(), index.withLetterAt(3, 0, 'Z'))
    }
}
