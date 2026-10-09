package app.palavrascruzadas.backup

import app.palavrascruzadas.core.play.SavedGame
import app.palavrascruzadas.core.settings.AppSettings
import app.palavrascruzadas.data.ProgressData
import app.palavrascruzadas.data.StatsData
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Backup: every store in a single versioned JSON. */
@Serializable
data class BackupFile(
    val app: String = BackupCodec.APP,
    val schema: Int = BackupCodec.SCHEMA,
    val exported: String,
    val progress: ProgressData,
    val stats: StatsData,
    val settings: AppSettings,
    val savedGames: List<SavedGame>,
)

/** Clear reason for rejecting a file (shown to the user). */
class BackupException(message: String) : Exception(message)

/** Summary shown before importing: levels, stars and streak. */
data class BackupSummary(val levels: Int, val stars: Int, val streak: Int, val exported: String)

/** Encodes and decodes the backup. Pure functions, tested on the JVM. */
object BackupCodec {
    const val APP = "palavrascruzadas"
    const val SCHEMA = 1

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        // Without this, the header (app and schema, which have default values) was left out of the file.
        encodeDefaults = true
    }

    fun encode(file: BackupFile): String = json.encodeToString(BackupFile.serializer(), file)

    fun decode(text: String): BackupFile {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: Exception) {
            throw BackupException("Este ficheiro não é uma cópia de Palavras Cruzadas.")
        }
        if (file.app != APP) throw BackupException("Este ficheiro é de outra aplicação.")
        if (file.schema > SCHEMA) throw BackupException("Este ficheiro é de uma versão mais nova da app. Atualiza a app primeiro.")
        return file
    }

    fun summary(file: BackupFile): BackupSummary = BackupSummary(
        levels = file.progress.progress.levels.size,
        stars = file.progress.progress.levels.values.sumOf { it.stars },
        streak = file.progress.daily.maxStreak,
        exported = file.exported,
    )
}
