package app.palavrascruzadas.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import app.palavrascruzadas.core.settings.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.serializer

private val Context.settingsDataStore: DataStore<AppSettings> by dataStore(
    fileName = "settings.json",
    serializer = JsonStoreSerializer(AppSettings.serializer(), AppSettings()),
)

/**Settings. They live in a typed DataStore like the rest of the progress, instead of
 * Preferences: it is a single model and already tolerates corrupted files.
 * Preferences: it is a single model and already tolerates corrupted files.
 */
class SettingsRepository private constructor(private val store: DataStore<AppSettings>) {

    val data: Flow<AppSettings> get() = store.data

    suspend fun current(): AppSettings = store.data.first()

    suspend fun replace(settings: AppSettings) {
        store.updateData { settings }
    }

    suspend fun update(change: (AppSettings) -> AppSettings) {
        store.updateData(change)
    }

    companion object {
        fun from(context: Context): SettingsRepository = SettingsRepository(context.applicationContext.settingsDataStore)
    }
}
