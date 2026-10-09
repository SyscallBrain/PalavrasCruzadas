package app.palavrascruzadas.data

import android.content.Context
import app.palavrascruzadas.core.campaign.LevelFormat
import app.palavrascruzadas.core.model.Puzzle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Pre-generated daily puzzles (`levels/daily.json`), read once and kept in memory. */
object DailyRepository {

    private val mutex = Mutex()
    private var cached: List<Puzzle>? = null

    suspend fun puzzles(context: Context): List<Puzzle> = mutex.withLock {
        cached ?: withContext(Dispatchers.IO) {
            context.applicationContext.assets.open("levels/daily.json").bufferedReader(Charsets.UTF_8).use { reader ->
                LevelFormat.parsePuzzles(reader.readText())
            }
        }.also { cached = it }
    }
}
