package app.palavrascruzadas.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import app.palavrascruzadas.backup.BackupCodec
import app.palavrascruzadas.reminder.ReminderScheduler
import app.palavrascruzadas.backup.BackupFile
import app.palavrascruzadas.backup.BackupSummary
import app.palavrascruzadas.core.settings.AppSettings
import java.time.Instant
import app.palavrascruzadas.data.ProgressRepository
import app.palavrascruzadas.data.SavedGamesRepository
import app.palavrascruzadas.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Settings and progress reset, for the settings and home screens. */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository.from(application)
    private val progress = ProgressRepository.from(application)
    private val savedGames = SavedGamesRepository.from(application)
    private val stats = app.palavrascruzadas.data.StatsRepository.from(application)

    val settings: StateFlow<AppSettings> = settingsRepo.data.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun update(change: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsRepo.update(change) }
    }

    /** Turns the daily reminder on (with the hour) or off and reschedules the job. */
    fun setReminder(hour: Int?) {
        viewModelScope.launch {
            settingsRepo.update { it.copy(reminderHour = hour) }
            val context = getApplication<Application>()
            if (hour == null) ReminderScheduler.cancel(context) else ReminderScheduler.schedule(context, hour)
        }
    }

    /** Writes the backup to the chosen file. */
    fun exportTo(uri: Uri, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val file = BackupFile(
                exported = Instant.now().toString(),
                progress = progress.data.first(),
                stats = stats.data.first(),
                settings = settingsRepo.current(),
                savedGames = savedGames.all(),
            )
            val ok = runCatching {
                getApplication<Application>().contentResolver.openOutputStream(uri)!!.use { out ->
                    out.write(BackupCodec.encode(file).encodeToByteArray())
                }
            }.isSuccess
            onDone(ok)
        }
    }

    /** Reads and validates a chosen file. Returns the file and its summary, or the reason it was rejected. */
    suspend fun readBackup(uri: Uri): Result<Pair<BackupFile, BackupSummary>> = runCatching {
        val text = getApplication<Application>().contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
        val file = BackupCodec.decode(text)
        file to BackupCodec.summary(file)
    }

    /** Replaces progress, statistics, settings and games in progress with the backup. */
    fun applyBackup(file: BackupFile, onDone: () -> Unit) {
        viewModelScope.launch {
            progress.replace(file.progress)
            stats.replace(file.stats)
            settingsRepo.replace(file.settings)
            savedGames.replaceAll(file.savedGames)
            onDone()
        }
    }

    /** Deletes the progress and the games in progress. Only call it after the two-step confirmation. */
    fun resetProgress() {
        viewModelScope.launch {
            progress.reset()
            savedGames.clear()
        }
    }
}
