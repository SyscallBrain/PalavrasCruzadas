package app.palavrascruzadas.core.campaign

import app.palavrascruzadas.core.play.HintReward
import app.palavrascruzadas.core.play.HintWallet
import kotlinx.serialization.Serializable

/** Saved result of a level. Stars and time are the best ones. */
@Serializable
data class LevelResult(
    val stars: Int,
    val bestSeconds: Int? = null,
    /** The 3-star reward was already given (first time only). */
    val threeStarRewarded: Boolean = false,
    /** The boss reward was already given (first time only). */
    val bossRewarded: Boolean = false,
)

/** Campaign progress: results by level number. Levels without an entry are not done. */
@Serializable
data class CampaignProgress(val levels: Map<Int, LevelResult> = emptyMap()) {

    /** First level not done yet, or the last one if everything is done. */
    val currentLevel: Int
        get() = (1..Campaign.TOTAL_LEVELS).firstOrNull { it !in levels } ?: Campaign.TOTAL_LEVELS
}

/**
 * Campaign rules. Pure functions: the screen and the persistence layer call them.
 */
object Campaign {
    const val TOTAL_LEVELS = 600

    /** Level 1 is open; each level opens when the previous one is done. A world opens with level 40 of the previous one. */
    fun isUnlocked(number: Int, progress: CampaignProgress): Boolean =
        number == 1 || (number - 1) in progress.levels

    /**
     * Records a win: keeps the best result and grants the first-time rewards
     * (3 stars: +1; boss cleared: +3). Returns the new progress and the new wallet.
     */
    fun complete(
        progress: CampaignProgress,
        wallet: HintWallet,
        number: Int,
        position: Int,
        stars: Int,
        seconds: Int,
    ): Pair<CampaignProgress, HintWallet> {
        require(stars in 1..3) { "Stars out of range: $stars" }
        val old = progress.levels[number]
        val boss = WorldTable.typeAt(position) == LevelType.BOSS
        var w = wallet
        var threeStarRewarded = old?.threeStarRewarded == true
        if (stars == 3 && !threeStarRewarded) {
            w = w.earn(HintReward.THREE_STARS)
            threeStarRewarded = true
        }
        var bossRewarded = old?.bossRewarded == true
        if (boss && !bossRewarded) {
            w = w.earn(HintReward.BOSS_WORLD)
            bossRewarded = true
        }
        val result = LevelResult(
            stars = maxOf(stars, old?.stars ?: 0),
            bestSeconds = listOfNotNull(seconds, old?.bestSeconds).min(),
            threeStarRewarded = threeStarRewarded,
            bossRewarded = bossRewarded,
        )
        return progress.copy(levels = progress.levels + (number to result)) to w
    }
}
