package app.palavrascruzadas.core.daily

import app.palavrascruzadas.core.play.HintReward
import app.palavrascruzadas.core.play.HintWallet
import java.time.LocalDate
import kotlinx.serialization.Serializable

/** Daily streak (same rules as `updateStreak` and `displayedStreak` in Sopa de Letras). */
object DailyStreak {
    /** Streak after playing today: kept if the last day was yesterday, back to 1 if a day was missed. */
    fun update(lastDay: Long, today: Long, streak: Int): Int = when {
        lastDay == today -> streak
        lastDay == today - 1 -> streak + 1
        else -> 1
    }

    /** Streak to display: zero if the last day played is neither today nor yesterday. */
    fun displayed(lastDay: Long, today: Long, streak: Int): Int =
        if (lastDay == today || lastDay == today - 1) streak else 0
}

/**
 * Daily state: days played with their stars, streak and records.
 * Days are stored both as ISO dates and as days since 1970.
 */
@Serializable
data class DailyProgress(
    val done: Map<String, Int> = emptyMap(),
    val streak: Int = 0,
    val lastDay: Long = NO_DAY,
    val maxStreak: Int = 0,
) {

    fun isDone(day: LocalDate): Boolean = day.toString() in done

    fun displayedStreak(today: LocalDate): Int = DailyStreak.displayed(lastDay, today.toEpochDay(), streak)

    /**
     * Records a finished day. Only the first result of the day counts for the streak and the
     * rewards (+2 per day, +5 every 7 days in a row). Replaying keeps the best stars.
     */
    fun complete(day: LocalDate, stars: Int, wallet: HintWallet): Pair<DailyProgress, HintWallet> {
        val key = day.toString()
        val previous = done[key]
        if (previous != null) {
            return copy(done = done + (key to maxOf(previous, stars))) to wallet
        }
        val today = day.toEpochDay()
        val newStreak = DailyStreak.update(lastDay, today, streak)
        var w = wallet.earn(HintReward.DAILY_DONE)
        if (newStreak % 7 == 0) w = w.earn(HintReward.STREAK_7)
        return DailyProgress(
            done = done + (key to stars),
            streak = newStreak,
            lastDay = today,
            maxStreak = maxOf(maxStreak, newStreak),
        ) to w
    }

    companion object {
        const val NO_DAY = -100L
    }
}
