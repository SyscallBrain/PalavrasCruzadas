package app.palavrascruzadas.core.settings

import app.palavrascruzadas.core.play.PlayOptions
import kotlinx.serialization.Serializable

/**
 * App settings. Game settings become [PlayOptions]; appearance and
 * feedback settings are read by the UI and the sounds.
 */
@Serializable
data class AppSettings(
    /** Visual theme id (one of the 5 themes). */
    val themeId: String = DEFAULT_THEME,
    val sounds: Boolean = true,
    val vibration: Boolean = true,
    val confirmWords: Boolean = true,
    val autoCheck: Boolean = false,
    val acceptUnaccented: Boolean = false,
    val skipFilled: Boolean = true,
    /** Grid letter size, in %, from 90 to 130 in steps of 10. */
    val gridScale: Int = 100,
    /** Colour-blind mode: complete words get a symbol as well as a colour. */
    val colorblind: Boolean = false,
    /** Reduce motion: null follows the system. */
    val reduceMotion: Boolean? = null,
    /** Daily reminder hour (0–23), or null when off. */
    val reminderHour: Int? = null,
) {
    fun playOptions(): PlayOptions = PlayOptions(
        confirmWords = confirmWords,
        autoCheck = autoCheck,
        skipFilled = skipFilled,
        acceptUnaccented = acceptUnaccented,
    )

    fun withGridScale(step: Int): AppSettings = copy(gridScale = clampScale(gridScale + step))

    companion object {
        const val DEFAULT_THEME = "tokyo-night"
        const val MIN_SCALE = 90
        const val MAX_SCALE = 130
        const val SCALE_STEP = 10

        const val DEFAULT_REMINDER_HOUR = 20

        fun clampScale(percent: Int): Int = percent.coerceIn(MIN_SCALE, MAX_SCALE)
    }
}
