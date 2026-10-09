package app.palavrascruzadas.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import app.palavrascruzadas.R
import app.palavrascruzadas.core.settings.AppSettings

/**
 * Game sounds and vibration, taken from Sopa de Letras. Both respect the settings and,
 * for sounds, silent mode; vibration also follows the system setting.
 */
class Feedback(context: Context) {
    private val app = context.applicationContext
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val sounds = mapOf(
        Sound.KEY to pool.load(app, R.raw.key, 1),
        Sound.FOUND to pool.load(app, R.raw.found, 1),
        Sound.WIN to pool.load(app, R.raw.win, 1),
        Sound.ERROR to pool.load(app, R.raw.error, 1),
        Sound.HINT to pool.load(app, R.raw.hint, 1),
        Sound.BONUS to pool.load(app, R.raw.bonus, 1),
    )

    enum class Sound { KEY, FOUND, WIN, ERROR, HINT, BONUS }

    fun play(sound: Sound, settings: AppSettings) {
        if (!settings.sounds || !ringerNormal()) return
        val volume = if (sound == Sound.KEY) 0.25f else 0.5f
        sounds[sound]?.let { pool.play(it, volume, volume, 1, 0, 1f) }
    }

    /** Short vibration per key; double for a correct word; long for an error (as in Sopa de Letras). */
    fun buzz(kind: Buzz, settings: AppSettings) {
        if (!settings.vibration || !systemHapticsOn()) return
        val vibrator = app.getSystemService(Vibrator::class.java) ?: return
        if (!vibrator.hasVibrator()) return
        val effect = when (kind) {
            Buzz.KEY -> VibrationEffect.createOneShot(20, 120)
            Buzz.CONFIRM -> VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE)
            Buzz.REJECT -> VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        runCatching { vibrator.vibrate(effect) }
    }

    enum class Buzz { KEY, CONFIRM, REJECT }

    private fun ringerNormal(): Boolean {
        val manager = app.getSystemService(AudioManager::class.java) ?: return true
        return manager.ringerMode == AudioManager.RINGER_MODE_NORMAL
    }

    private fun systemHapticsOn(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return true
        return runCatching {
            Settings.System.getInt(app.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 1
        }.getOrDefault(true)
    }

    fun release() {
        pool.release()
    }
}
