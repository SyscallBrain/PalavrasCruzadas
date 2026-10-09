package app.palavrascruzadas.core.play

/**
 * Time attack: 3 minutes, a sequence of 5×5 minis, no hints.
 * Each correct word adds 5 s; each completed grid adds 15 s. The score is the number of correct words.
 */
data class TimeAttack(
    val timeLeft: Int = START_SECONDS,
    val words: Int = 0,
    val grids: Int = 0,
) {
    val isOver: Boolean get() = timeLeft <= 0

    fun tick(): TimeAttack = copy(timeLeft = maxOf(0, timeLeft - 1))

    /** Adds the new words that became correct since the last call. */
    fun wordsFound(count: Int): TimeAttack =
        if (count <= 0) this else copy(words = words + count, timeLeft = timeLeft + count * WORD_BONUS)

    fun gridDone(): TimeAttack = copy(grids = grids + 1, timeLeft = timeLeft + GRID_BONUS)

    companion object {
        const val START_SECONDS = 180
        const val WORD_BONUS = 5
        const val GRID_BONUS = 15
    }
}
