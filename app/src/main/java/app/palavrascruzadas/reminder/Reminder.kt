package app.palavrascruzadas.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.palavrascruzadas.MainActivity
import app.palavrascruzadas.R
import app.palavrascruzadas.data.AppClock
import app.palavrascruzadas.data.ProgressRepository
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.flow.first

/**
 * Daily reminder: a daily job at the chosen hour. It only notifies if
 * today's daily puzzle is not done. No internet and no exact alarms (WorkManager).
 */
object ReminderScheduler {
    private const val WORK = "daily-reminder"
    private const val CHANNEL = "desafio-diario"

    /** Schedules (or reschedules) the reminder for the next [hour]. */
    fun schedule(context: Context, hour: Int) {
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(Duration.ofDays(1))
            .setInitialDelay(delayUntilHour(LocalDateTime.now(), hour))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    /** Time until the next [hour]:00 (today if it has not passed yet, otherwise tomorrow). */
    fun delayUntilHour(now: LocalDateTime, hour: Int): Duration {
        var next = now.toLocalDate().atTime(LocalTime.of(hour, 0))
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK)
    }

    /** "Desafio diário" notification channel (Android 8+). */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Opens the daily puzzle when the notification is tapped. */
    fun openDailyIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_ROUTE, MainActivity.ROUTE_DAILY)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    internal fun notify(context: Context) {
        if (!hasPermission(context)) return
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(context.getString(R.string.reminder_text))
            .setContentIntent(openDailyIntent(context))
            .setAutoCancel(true)
            .build()
        // The permission may be revoked between the check and the call: then nothing is sent.
        try {
            NotificationManagerCompat.from(context).notify(1001, notification)
        } catch (e: SecurityException) {
            return
        }
    }
}

/** Reminder job: only notifies if today's daily puzzle is not done. */
class DailyReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val data = ProgressRepository.from(applicationContext).data.first()
        val today = AppClock.today(data.simulatedDay)
        if (!data.daily.isDone(today)) ReminderScheduler.notify(applicationContext)
        return Result.success()
    }
}
