package app.palavrascruzadas.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import app.palavrascruzadas.core.campaign.Campaign
import app.palavrascruzadas.core.campaign.WorldTable
import app.palavrascruzadas.core.daily.DailyPuzzle
import app.palavrascruzadas.data.ProgressData
import app.palavrascruzadas.ui.theme.Bricolage
import app.palavrascruzadas.ui.theme.CruzadasColors
import app.palavrascruzadas.ui.theme.Figtree
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Home screen: continue the campaign, daily challenge with streak,
 * game modes and the bottom row.
 */
@Composable
fun HomeScreen(
    colors: CruzadasColors,
    data: ProgressData,
    today: LocalDate,
    onContinue: () -> Unit,
    onTrail: () -> Unit,
    onDaily: () -> Unit,
    onMini: () -> Unit,
    onFreeGame: () -> Unit,
    onBooks: () -> Unit,
    onTimeAttack: () -> Unit,
    onSettings: () -> Unit,
    onStats: () -> Unit,
    onAchievements: () -> Unit,
    /** Developer menu: test builds only; null hides the entry point. */
    onSecret: (() -> Unit)? = null,
) {
    val current = data.progress.currentLevel
    val currentStars = data.progress.levels[current]?.stars ?: 0
    val world = (current - 1) / WorldTable.LEVELS_PER_WORLD + 1
    val dailyDone = data.daily.isDone(today)
    val streak = data.daily.displayedStreak(today)
    val dateText = today.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.forLanguageTag("pt-PT")))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Secret entry to the developer menu: 7 taps in a row on the title (no visible feedback).
            var taps by remember { mutableStateOf(0) }
            var lastTap by remember { mutableStateOf(0L) }
            Text(
                "Palavras Cruzadas",
                color = colors.text,
                fontFamily = Bricolage,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (onSecret == null) Modifier else Modifier.pointerInput(Unit) {
                            detectTapGestures {
                                val now = System.currentTimeMillis()
                                taps = if (now - lastTap < SECRET_TAP_GAP_MS) taps + 1 else 1
                                lastTap = now
                                if (taps >= SECRET_TAPS) {
                                    taps = 0
                                    onSecret()
                                }
                            }
                        },
                    ),
            )
            Row(
                modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "${data.wallet.balance} ajudas" },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                BulbIcon(color = colors.palette[3 % colors.palette.size], size = 18.dp)
                Text("${data.wallet.balance}", color = colors.text, fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        }

        // Tapping the card opens the campaign trail; the button plays the current level right away.
        Card(colors, onClick = onTrail) {
            Text("Campanha · ver o trilho", color = colors.mute, fontFamily = Figtree, fontSize = 13.sp)
            Text(
                "Nível $current · Mundo $world",
                color = colors.text,
                fontFamily = Bricolage,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            )
            Text(
                if (currentStars > 0) "★".repeat(currentStars) + "☆".repeat(3 - currentStars) else "Ainda não jogado",
                color = colors.accent,
                fontFamily = Figtree,
                fontSize = 14.sp,
            )
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
            ) {
                Text("Continuar · nível $current", fontFamily = Figtree, fontWeight = FontWeight.Bold)
            }
        }

        Card(colors, onClick = onDaily) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FlameIcon(color = if (streak > 0) colors.palette[3] else colors.mute, size = 28.dp)
                Text(
                    "Desafio diário",
                    color = colors.text,
                    fontFamily = Bricolage,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.weight(1f),
                )
                Text("$streak", color = colors.text, fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Text(dateText.replaceFirstChar { it.uppercase() }, color = colors.mute, fontFamily = Figtree, fontSize = 13.sp)
            Text(
                when {
                    dailyDone -> "Feito hoje · " + "★".repeat(data.daily.done.getValue(today.toString())) + "☆".repeat(3 - data.daily.done.getValue(today.toString()))
                    else -> "Por fazer"
                },
                color = colors.accent,
                fontFamily = Figtree,
                fontSize = 14.sp,
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tile("Mini rápido", "5×5 em poucos minutos", colors, enabled = true, onClick = onMini, modifier = Modifier.weight(1f))
            Tile("Jogo livre", "Escolhe tamanho e tema", colors, enabled = true, onClick = onFreeGame, modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tile("Cadernos", "Temas, roda, diretas e encaixar", colors, enabled = true, onClick = onBooks, modifier = Modifier.weight(1f))
            Tile(
                "Contra-relógio",
                data.timeAttackBest?.let { "Recorde: $it palavras" } ?: "3 minutos, sem ajudas",
                colors,
                enabled = true,
                onClick = onTimeAttack,
                modifier = Modifier.weight(1f),
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Estatísticas", "Conquistas", "Definições").forEach { label ->
                val action = when (label) {
                    "Estatísticas" -> onStats
                    "Conquistas" -> onAchievements
                    else -> onSettings
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface)
                        .clickable(onClick = action)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, color = colors.text, fontFamily = Figtree, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun Card(colors: CruzadasColors, onClick: () -> Unit, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        content()
    }
}

@Composable
private fun Tile(
    title: String,
    subtitle: String,
    colors: CruzadasColors,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) colors.surface else colors.surface.copy(alpha = 0.5f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            title,
            color = if (enabled) colors.text else colors.mute,
            fontFamily = Bricolage,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
        Text(subtitle, color = colors.mute, fontFamily = Figtree, fontSize = 12.sp)
    }
}

/** "Simulated date" dialog: test builds only, to test the daily puzzle on the phone. */
@Composable
fun SimulatedDateDialog(
    today: LocalDate,
    simulated: Boolean,
    onChange: (LocalDate?) -> Unit,
    onDismiss: () -> Unit,
) {
    var day by remember { mutableStateOf(today) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Data simulada") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(day.toString(), fontFamily = Figtree, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { day = day.minusDays(1) }) { Text("−1 dia") }
                    TextButton(onClick = { day = day.plusDays(1) }) { Text("+1 dia") }
                }
                Text(
                    if (simulated) "A usar a data simulada." else "A usar a data real.",
                    fontFamily = Figtree,
                    fontSize = 12.sp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onChange(day) }) { Text("Usar") }
        },
        dismissButton = {
            TextButton(onClick = { onChange(null) }) { Text("Data real") }
        },
    )
}

private const val SECRET_TAPS = 7
private const val SECRET_TAP_GAP_MS = 600L
