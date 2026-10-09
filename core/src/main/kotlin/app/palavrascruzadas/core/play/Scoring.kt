package app.palavrascruzadas.core.play

/** Stars of a level and the reason for each lost star. */
data class StarResult(val stars: Int, val lostReasons: List<String>)

/**
 * Stars: 3 by default; -1 past the target time; -1 with at least 1 hint point;
 * -1 more with 4 or more; with auto-check, at most 2; never less than 1.
 */
object Scoring {

    fun stars(elapsedSeconds: Int, targetSeconds: Int, state: PlayState): StarResult {
        val reasons = mutableListOf<String>()
        var stars = 3
        if (elapsedSeconds > targetSeconds) {
            stars -= 1
            reasons += "Mais de ${formatTime(targetSeconds)}"
        }
        val points = state.hintPoints
        if (points >= 1) {
            stars -= 1
            reasons += helpReason(points)
        }
        if (points >= 4) {
            stars -= 1
        }
        if (state.options.autoCheck && stars > 2) {
            stars = 2
            reasons += "Correção automática ligada"
        }
        stars = maxOf(stars, 1)
        return StarResult(stars, reasons)
    }

    /** Formats the time as m:ss, for the lost-star reason ("Mais de 3:30"). */
    fun formatTime(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

    private fun helpReason(points: Int): String =
        if (points == 1) "1 ajuda" else "$points ajudas"
}
