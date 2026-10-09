package app.palavrascruzadas.data

import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * JSON serializer for a typed DataStore (one file per store).
 * If the file is corrupted, returns the default value instead of crashing the app.
 */
class JsonStoreSerializer<T>(
    private val serializer: KSerializer<T>,
    override val defaultValue: T,
) : Serializer<T> {

    override suspend fun readFrom(input: InputStream): T {
        val text = input.readBytes().decodeToString()
        if (text.isBlank()) return defaultValue
        return try {
            Json.decodeFromString(serializer, text)
        } catch (e: SerializationException) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: T, output: OutputStream) {
        output.write(Json.encodeToString(serializer, t).encodeToByteArray())
    }
}
