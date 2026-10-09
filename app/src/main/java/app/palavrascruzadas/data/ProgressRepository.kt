package app.palavrascruzadas.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import app.palavrascruzadas.core.campaign.Campaign
import app.palavrascruzadas.core.books.BooksProgress
import app.palavrascruzadas.core.campaign.CampaignProgress
import app.palavrascruzadas.core.daily.DailyProgress
import app.palavrascruzadas.core.play.HintReward
import app.palavrascruzadas.core.play.HintWallet
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.coroutines.flow.Flow

/** Campaign progress, hint balance and level types already explained. */
@Serializable
data class ProgressData(
    val progress: CampaignProgress = CampaignProgress(),
    val wallet: HintWallet = HintWallet(),
    /** Level types whose rule card was already shown. */
    val seenTypes: Set<String> = emptySet(),
    /** Daily challenge: days played, streak and record. */
    val daily: DailyProgress = DailyProgress(),
    /** Best quick-mini time, in seconds. */
    val miniBestSeconds: Int? = null,
    /** Simulated day (epoch day) to test the daily puzzle on the phone; null = real day. Test builds only. */
    val simulatedDay: Long? = null,
    /** Themed books: stars per theme and puzzle. */
    val books: BooksProgress = BooksProgress(),
    /** Best time-attack score (correct words). */
    val timeAttackBest: Int? = null,
    /** Puzzles whose keyword already gave its reward (once per puzzle). */
    val keywordRewarded: Set<String> = emptySet(),
)

private val Context.progressDataStore: DataStore<ProgressData> by dataStore(
    fileName = "progress.json",
    serializer = JsonStoreSerializer(ProgressData.serializer(), ProgressData()),
)

/** Progress and hints, with atomic writes through DataStore. */
class ProgressRepository private constructor(private val store: DataStore<ProgressData>) {

    val data: Flow<ProgressData> get() = store.data

    /** Replaces all progress (importing a backup). */
    suspend fun replace(data: ProgressData) {
        store.updateData { data }
    }

    /** Resets all progress (campaign, wallet, daily, books, records). Settings are kept. */
    suspend fun reset() {
        store.updateData { ProgressData() }
    }

    suspend fun saveWallet(wallet: HintWallet) {
        store.updateData { it.copy(wallet = wallet) }
    }

    /** Records a win and returns the wallet with the rewards granted. */
    suspend fun recordWin(number: Int, position: Int, stars: Int, seconds: Int, wallet: HintWallet): HintWallet {
        var result = wallet
        store.updateData { data ->
            val (progress, updated) = Campaign.complete(data.progress, wallet, number, position, stars, seconds)
            result = updated
            data.copy(progress = progress, wallet = updated)
        }
        return result
    }

    /** Records the daily challenge and returns the wallet with the rewards. */
    suspend fun recordDaily(day: java.time.LocalDate, stars: Int, wallet: HintWallet): HintWallet {
        var result = wallet
        store.updateData { data ->
            val (daily, updated) = data.daily.complete(day, stars, wallet)
            result = updated
            data.copy(daily = daily, wallet = updated)
        }
        return result
    }

    /** Guarda o tempo do mini se for o melhor. */
    suspend fun recordMini(seconds: Int) {
        store.updateData { data ->
            val best = data.miniBestSeconds
            data.copy(miniBestSeconds = if (best == null || seconds < best) seconds else best)
        }
    }

    suspend fun recordBook(theme: String, number: Int, stars: Int) {
        store.updateData { it.copy(books = it.books.complete(theme, number, stars)) }
    }

    /** Time attack: stores the score if it is a new record. */
    suspend fun recordTimeAttack(words: Int) {
        store.updateData { data ->
            val best = data.timeAttackBest
            data.copy(timeAttackBest = if (best == null || words > best) words else best)
        }
    }

    /** Keyword guessed: +1 hint, once per puzzle. Returns the wallet. */
    suspend fun rewardKeyword(puzzleId: String, wallet: HintWallet): HintWallet {
        var result = wallet
        store.updateData { data ->
            if (puzzleId in data.keywordRewarded) {
                data
            } else {
                result = wallet.earn(HintReward.KEYWORD_GUESSED)
                data.copy(wallet = result, keywordRewarded = data.keywordRewarded + puzzleId)
            }
        }
        return result
    }

    suspend fun setSimulatedDay(day: Long?) {
        store.updateData { it.copy(simulatedDay = day) }
    }

    suspend fun markSeen(type: String) {
        store.updateData { it.copy(seenTypes = it.seenTypes + type) }
    }

    companion object {
        fun from(context: Context): ProgressRepository = ProgressRepository(context.applicationContext.progressDataStore)
    }
}
