package app.palavrascruzadas.core.books

import org.junit.Assert.assertEquals
import org.junit.Test

class BooksProgressTest {
    @Test
    fun keepsTheBestStarsPerPuzzle() {
        val p = BooksProgress().complete("comida", 3, 1).complete("comida", 3, 3).complete("comida", 3, 2)
        assertEquals(3, p.stars.getValue("comida").getValue(3))
        assertEquals(1, p.doneCount("comida"))
        assertEquals(3, p.starTotal("comida"))
    }

    @Test
    fun countsEachBookSeparately() {
        val p = BooksProgress().complete("mar", 1, 2).complete("lingua", 1, 3).complete("lingua", 2, 1)
        assertEquals(1, p.doneCount("mar"))
        assertEquals(2, p.doneCount("lingua"))
        assertEquals(0, p.doneCount("corpo"))
    }

    @Test
    fun thereAreFourteenBooksOfTwentyFive() {
        assertEquals(14, Books.THEMES.size)
        assertEquals(25, Books.PUZZLES_PER_BOOK)
    }
}
