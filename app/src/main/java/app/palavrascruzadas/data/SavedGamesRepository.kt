package app.palavrascruzadas.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import app.palavrascruzadas.core.play.SavedGame
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable

@Serializable
data class SavedGamesData(val games: List<SavedGame> = emptyList())

private val Context.savedGamesDataStore: DataStore<SavedGamesData> by dataStore(
    fileName = "saved_games.json",
    serializer = JsonStoreSerializer(SavedGamesData.serializer(), SavedGamesData()),
)

/** Games in progress, at most 20; the oldest ones are dropped. */
class SavedGamesRepository private constructor(private val store: DataStore<SavedGamesData>) {

    suspend fun get(puzzleId: String): SavedGame? =
        store.data.first().games.firstOrNull { it.puzzleId == puzzleId }

    suspend fun put(game: SavedGame) {
        store.updateData { data ->
            val others = data.games.filter { it.puzzleId != game.puzzleId }
            SavedGamesData((others + game).sortedByDescending { it.updatedAt }.take(MAX_GAMES))
        }
    }

    suspend fun replaceAll(games: List<SavedGame>) {
        store.updateData { SavedGamesData(games.sortedByDescending { it.updatedAt }.take(MAX_GAMES)) }
    }

    suspend fun all(): List<SavedGame> = store.data.first().games

    suspend fun clear() {
        store.updateData { SavedGamesData() }
    }

    suspend fun remove(puzzleId: String) {
        store.updateData { data -> SavedGamesData(data.games.filter { it.puzzleId != puzzleId }) }
    }

    companion object {
        const val MAX_GAMES = 20

        fun from(context: Context): SavedGamesRepository = SavedGamesRepository(context.applicationContext.savedGamesDataStore)
    }
}
