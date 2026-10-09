package app.palavrascruzadas.data

import android.content.Context
import app.palavrascruzadas.core.campaign.LevelFormat
import app.palavrascruzadas.core.campaign.LevelSpec
import app.palavrascruzadas.core.campaign.WorldLevels
import app.palavrascruzadas.core.campaign.WorldTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Campaign levels from the assets (`levels/world_XX.json`), read per world and kept in memory. */
object CampaignRepository {

    private val mutex = Mutex()
    private val worlds = HashMap<Int, WorldLevels>()

    suspend fun world(context: Context, number: Int): WorldLevels = mutex.withLock {
        worlds[number] ?: withContext(Dispatchers.IO) {
            val asset = "levels/world_%02d.json".format(number)
            context.applicationContext.assets.open(asset).bufferedReader(Charsets.UTF_8).use { reader ->
                LevelFormat.parseWorld(reader.readText())
            }
        }.also { worlds[number] = it }
    }

    /** Level by its global number (1–600). */
    suspend fun level(context: Context, number: Int): LevelSpec {
        require(number in 1..WorldTable.worlds.size * WorldTable.LEVELS_PER_WORLD) { "Level out of range: $number" }
        val worldNumber = (number - 1) / WorldTable.LEVELS_PER_WORLD + 1
        return world(context, worldNumber).levels[(number - 1) % WorldTable.LEVELS_PER_WORLD]
    }
}
