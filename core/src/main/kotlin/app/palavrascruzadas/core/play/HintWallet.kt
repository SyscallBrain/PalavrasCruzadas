package app.palavrascruzadas.core.play

/** Price of each hint, in hints. */
enum class HintCost(val amount: Int) {
    REVEAL_LETTER(1),
    ANAGRAM(1),
    VERIFY_GRID(1),
    REVEAL_WORD(3),
    VERIFY_WORD(0),
}

/** What each achievement or challenge earns. */
enum class HintReward(val amount: Int) {
    THREE_STARS(1),
    BOSS_WORLD(3),
    DAILY_DONE(2),
    STREAK_7(5),
    KEYWORD_GUESSED(1),
    BONUS_WORDS_WHEEL(1),
    ACHIEVEMENT(2),
}

/**
 * Hint balance: starts at 10, at most 99.
 * Immutable: [spend] and [earn] return a new wallet.
 */
@kotlinx.serialization.Serializable
data class HintWallet(val balance: Int = START) {

    fun canAfford(cost: HintCost): Boolean = balance >= cost.amount

    /** Spends the hint, or returns null if the balance is too low. */
    fun spend(cost: HintCost): HintWallet? =
        if (canAfford(cost)) copy(balance = balance - cost.amount) else null

    fun earn(reward: HintReward): HintWallet = copy(balance = minOf(MAX, balance + reward.amount))

    companion object {
        const val START = 10
        const val MAX = 99
    }
}
