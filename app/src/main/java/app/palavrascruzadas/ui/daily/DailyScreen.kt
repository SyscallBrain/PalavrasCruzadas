package app.palavrascruzadas.ui.daily

import app.palavrascruzadas.ui.components.ScreenHeader
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.palavrascruzadas.core.daily.DailyProgress
import app.palavrascruzadas.ui.home.FlameIcon
import app.palavrascruzadas.ui.theme.Bricolage
import app.palavrascruzadas.ui.theme.CruzadasColors
import app.palavrascruzadas.ui.theme.Figtree
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Daily calendar: days played with their stars, streak and record. */
@Composable
fun DailyScreen(
    colors: CruzadasColors,
    daily: DailyProgress,
    today: LocalDate,
    onBack: () -> Unit,
    onPlay: () -> Unit,
) {
    val month = today.withDayOfMonth(1)
    val firstWeekday = (month.dayOfWeek.value - 1) // Monday = 0
    val days = month.lengthOfMonth()
    val streak = daily.displayedStreak(today)
    val title = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("pt-PT")))
        .replaceFirstChar { it.uppercase() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader("Desafio diário", colors, onBack)

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FlameIcon(color = if (streak > 0) colors.palette[3] else colors.mute, size = 26.dp)
            Text("Sequência: $streak", color = colors.text, fontFamily = Figtree, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("· recorde ${daily.maxStreak}", color = colors.mute, fontFamily = Figtree, fontSize = 14.sp)
        }

        Text(title, color = colors.text, fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("S", "T", "Q", "Q", "S", "S", "D").forEach { letter ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(letter, color = colors.mute, fontFamily = Figtree, fontSize = 12.sp)
                }
            }
        }
        val cells = firstWeekday + days
        val rows = (cells + 6) / 7
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val index = row * 7 + col
                    val day = index - firstWeekday + 1
                    Box(modifier = Modifier.weight(1f).height(44.dp), contentAlignment = Alignment.Center) {
                        if (day in 1..days) {
                            val date = month.withDayOfMonth(day)
                            val stars = daily.done[date.toString()]
                            DayCell(day, stars, isToday = date == today, colors = colors)
                        }
                    }
                }
            }
        }

        Text(
            "Recorde de sequência: ${daily.maxStreak} dias",
            color = colors.mute,
            fontFamily = Figtree,
            fontSize = 13.sp,
        )
        Button(
            onClick = onPlay,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
        ) {
            Text(
                if (daily.isDone(today)) "Jogar outra vez" else "Jogar hoje",
                fontFamily = Figtree,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun DayCell(day: Int, stars: Int?, isToday: Boolean, colors: CruzadasColors) {
    val fill = if (stars != null) colors.accent else colors.surface
    val text = if (stars != null) colors.onAccent else colors.text
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(fill)
            .then(if (isToday) Modifier.border(2.dp, colors.accent, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(day.toString(), color = text, fontFamily = Figtree, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
    }
}
