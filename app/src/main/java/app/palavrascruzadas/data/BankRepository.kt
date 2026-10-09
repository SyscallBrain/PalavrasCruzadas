package app.palavrascruzadas.data

import android.content.Context
import app.palavrascruzadas.core.bank.ClueBank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Clue bank from the assets, read once and kept in memory. */
object BankRepository {

    @Volatile
    private var cached: ClueBank? = null

    suspend fun load(context: Context): ClueBank {
        cached?.let { return it }
        return withContext(Dispatchers.IO) {
            context.applicationContext.assets.open(ASSET).bufferedReader(Charsets.UTF_8).use { reader ->
                ClueBank.parse(reader.readText())
            }.also { cached = it }
        }
    }

    private const val ASSET = "clues/pt-PT.json"
}
