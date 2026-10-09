package app.palavrascruzadas.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import app.palavrascruzadas.core.achievements.Achievement
import app.palavrascruzadas.core.achievements.AchievementStats
import app.palavrascruzadas.core.books.Books
import app.palavrascruzadas.core.campaign.Campaign
import app.palavrascruzadas.core.campaign.WorldTable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable

/** Statistics: totals, records, achievement counters and the unlocked achievements. */
@Serializable
data class StatsData(
    val puzzlesDone: Int = 0,
    val wordsSolved: Int = 0,
    val lettersSolved: Int = 0,
    val totalSeconds: Long = 0,
    /** Best free-play time, per grid size. */
    val freeBest: Map<Int, Int> = emptyMap(),
    val freeGridSizesDone: Set<Int> = emptySet(),
    val modesUsed: Set<String> = emptySet(),
    val visualThemesUsed: Set<String> = emptySet(),
    val bossWithoutHints: Int = 0,
    val keywordsGuessed: Int = 0,
    val keywordsHalfKnown: Int = 0,
    val wheelsFullBonus: Int = 0,
    val bonusWords: Int = 0,
    val unlocked: Set<String> = emptySet(),
)

private val Context.statsDataStore: DataStore<StatsData> by dataStore(
    fileName = "stats.json",
    serializer = JsonStoreSerializer(StatsData.serializer(), StatsData()),
)

/** Stores the totals and returns the achievements that were just unlocked. */
class StatsRepository private constructor(private val store: DataStore<StatsData>) {

    val data: Flow<StatsData> get() = store.data

    suspend fun replace(data: StatsData) {
        store.updateData { data }
    }

    suspend fun recordMode(mode: String, visualTheme: String) {
        store.updateData { it.copy(modesUsed = it.modesUsed + mode, visualThemesUsed = it.visualThemesUsed + visualTheme) }
    }

    /** A finished puzzle. [freeSize] only for free play. */
    suspend fun recordPuzzle(seconds: Int, letters: Int, words: Int, freeSize: Int?) {
        store.updateData { s ->
            s.copy(
                puzzlesDone = s.puzzlesDone + 1,
                wordsSolved = s.wordsSolved + words,
                lettersSolved = s.lettersSolved + letters,
                totalSeconds = s.totalSeconds + seconds,
                freeGridSizesDone = if (freeSize != null) s.freeGridSizesDone + freeSize else s.freeGridSizesDone,
                freeBest = if (freeSize == null) s.freeBest else s.freeBest + (freeSize to minOf(seconds, s.freeBest[freeSize] ?: Int.MAX_VALUE)),
            )
        }
    }

    /** Correct words in a time attack (which does not count as a finished puzzle). */
    suspend fun recordWords(count: Int) {
        store.updateData { it.copy(wordsSolved = it.wordsSolved + count) }
    }

    suspend fun recordBossWithoutHints() {
        store.updateData { it.copy(bossWithoutHints = it.bossWithoutHints + 1) }
    }

    suspend fun recordKeyword(halfKnown: Boolean) {
        store.updateData { s -> s.copy(keywordsGuessed = s.keywordsGuessed + 1, keywordsHalfKnown = s.keywordsHalfKnown + if (halfKnown) 1 else 0) }
    }

    suspend fun recordBonusWord() {
        store.updateData { it.copy(bonusWords = it.bonusWords + 1) }
    }

    suspend fun recordWheelFull() {
        store.updateData { it.copy(wheelsFullBonus = it.wheelsFullBonus + 1) }
    }

    /**
     * Computes the achievements from the current numbers (progress + statistics), stores the new ones and
     * returns only those that were just unlocked.
     */
    suspend fun unlockNew(progress: ProgressData): List<Achievement> {
        var fresh = emptyList<Achievement>()
        store.updateData { s ->
            val now = Achievement.unlocked(achievementStats(progress, s))
            fresh = now.filter { it.id !in s.unlocked }
            s.copy(unlocked = s.unlocked + fresh.map { it.id })
        }
        return fresh
    }

    companion object {
        fun from(context: Context): StatsRepository = StatsRepository(context.applicationContext.statsDataStore)

        /** Numbers the achievements read, built from the progress and the statistics. */
        fun achievementStats(progress: ProgressData, stats: StatsData): AchievementStats {
            val levels = progress.progress.levels
            val worldStars = WorldTable.worlds.map { world ->
                val first = (world.number - 1) * WorldTable.LEVELS_PER_WORLD + 1
                (first until first + WorldTable.LEVELS_PER_WORLD).sumOf { levels[it]?.stars ?: 0 }
            }
            return AchievementStats(
                puzzlesDone = stats.puzzlesDone,
                wordsSolved = stats.wordsSolved,
                levelsDone = levels.size,
                bestWorldStars = worldStars.maxOrNull() ?: 0,
                worldsDone = worldStars.count { it >= WorldTable.LEVELS_PER_WORLD },
                threeStarLevels = levels.values.count { it.stars == 3 },
                dailyMaxStreak = progress.daily.maxStreak,
                bossWithoutHints = stats.bossWithoutHints,
                miniBestSeconds = progress.miniBestSeconds,
                timeAttackBest = progress.timeAttackBest,
                keywordsGuessed = stats.keywordsGuessed,
                keywordsHalfKnown = stats.keywordsHalfKnown,
                wheelsFullBonus = stats.wheelsFullBonus,
                booksDone = Books.THEMES.count { progress.books.doneCount(it) >= Books.PUZZLES_PER_BOOK } +
                    Books.SPECIAL.count { progress.books.doneCount(it) >= Books.PUZZLES_PER_SPECIAL },
                modesUsed = stats.modesUsed,
                visualThemesUsed = stats.visualThemesUsed,
                freeGridSizesDone = stats.freeGridSizesDone,
            )
        }
    }
}
