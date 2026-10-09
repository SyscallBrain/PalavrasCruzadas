package app.palavrascruzadas.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.action.clickable
import androidx.glance.unit.ColorProvider
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.palavrascruzadas.MainActivity
import app.palavrascruzadas.data.AppClock
import app.palavrascruzadas.data.ProgressRepository
import app.palavrascruzadas.data.SettingsRepository
import app.palavrascruzadas.ui.theme.AllThemes
import app.palavrascruzadas.ui.theme.TokyoNight
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.flow.first

/**
 * Daily widget: "Desafio de hoje", status, streak and the theme colours.
 * Tapping it opens the daily puzzle. Updates when the daily is finished and at midnight (WidgetRefreshWorker).
 */
class DailyWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val progress = ProgressRepository.from(context).data.first()
        val settings = SettingsRepository.from(context).current()
        val today = AppClock.today(progress.simulatedDay)
        val theme = AllThemes.firstOrNull { it.id == settings.themeId } ?: TokyoNight
        val stars = progress.daily.done[today.toString()]
        val streak = progress.daily.displayedStreak(today)
        val open = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_ROUTE, MainActivity.ROUTE_DAILY)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(theme.bg))
                    .padding(12.dp)
                    .clickable(actionStartActivity(open)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Desafio de hoje",
                    style = TextStyle(color = ColorProvider(theme.accent), fontWeight = FontWeight.Bold, fontSize = 14.sp),
                )
                Text(
                    if (stars != null) "Feito " + "★".repeat(stars) + "☆".repeat(3 - stars) else "Por fazer",
                    style = TextStyle(color = ColorProvider(theme.text), fontSize = 13.sp),
                )
                Text(
                    "Sequência: $streak",
                    style = TextStyle(color = ColorProvider(theme.mute), fontSize = 12.sp),
                )
            }
        }
    }
}

class DailyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DailyWidget()
}

/** Midnight job: updates the widget for the new day. */
class WidgetRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        DailyWidget().updateAll(applicationContext)
        return Result.success()
    }
}

/** Schedules the daily widget refresh at midnight. */
object WidgetScheduler {
    private const val WORK = "daily-widget-refresh"

    fun schedule(context: Context) {
        val now = LocalDateTime.now()
        val midnight = now.toLocalDate().plusDays(1).atTime(LocalTime.MIDNIGHT)
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(Duration.ofDays(1))
            .setInitialDelay(Duration.between(now, midnight))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
