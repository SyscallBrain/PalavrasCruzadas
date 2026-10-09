package app.palavrascruzadas.data

import android.content.Context
import app.palavrascruzadas.core.campaign.LevelFormat
import app.palavrascruzadas.core.campaign.LevelSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Puzzles of each book (`levels/books/<theme>.json`), read per theme and kept in memory. */
object BookRepository {

    private val mutex = Mutex()
    private val books = HashMap<String, List<LevelSpec>>()

    /** Puzzles of the book with the wheel fields (base word and bonus words). */
    suspend fun puzzles(context: Context, theme: String): List<LevelSpec> = mutex.withLock {
        books[theme] ?: withContext(Dispatchers.IO) {
            context.applicationContext.assets.open("levels/books/$theme.json").bufferedReader(Charsets.UTF_8).use { reader ->
                LevelFormat.parseSpecs(reader.readText())
            }
        }.also { books[theme] = it }
    }
}
