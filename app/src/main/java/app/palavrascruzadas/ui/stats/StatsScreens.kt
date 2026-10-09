package app.palavrascruzadas.ui.stats

import app.palavrascruzadas.ui.components.ScreenHeader
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.palavrascruzadas.core.achievements.Achievement
import app.palavrascruzadas.core.campaign.WorldTable
import app.palavrascruzadas.data.AppClock
import app.palavrascruzadas.data.ProgressData
import app.palavrascruzadas.data.StatsData
import app.palavrascruzadas.ui.theme.Bricolage
import app.palavrascruzadas.ui.theme.CruzadasColors
import app.palavrascruzadas.ui.theme.Figtree

/** Statistics: daily, totals, records and campaign. */
@Composable
fun StatsScreen(colors: CruzadasColors, progress: ProgressData, stats: StatsData, onBack: () -> Unit) {
    val levels = progress.progress.levels
    val streak = progress.daily.displayedStreak(AppClock.today(progress.simulatedDay))
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
        ScreenHeader("Estatísticas", colors, onBack)

        Block("Diário", colors) {
            Stat("Sequência atual", "$streak dias", colors)
            Stat("Recorde de sequência", "${progress.daily.maxStreak} dias", colors)
            Stat("Dias feitos", "${progress.daily.done.size}", colors)
        }
        Block("Campanha", colors) {
            Stat("Níveis feitos", "${levels.size} de ${WorldTable.worlds.size * WorldTable.LEVELS_PER_WORLD}", colors)
            Stat("Níveis com 3 estrelas", "${levels.values.count { it.stars == 3 }}", colors)
            Stat("Mundos concluídos", "${worldsDone(levels.keys)}", colors)
        }
        Block("Totais", colors) {
            Stat("Puzzles resolvidos", "${stats.puzzlesDone}", colors)
            Stat("Palavras resolvidas", "${stats.wordsSolved}", colors)
            Stat("Letras escritas", "${stats.lettersSolved}", colors)
            Stat("Tempo total", formatHours(stats.totalSeconds), colors)
        }
        Block("Recordes", colors) {
            Stat("Mini rápido (melhor)", progress.miniBestSeconds?.let { mmss(it) } ?: "—", colors)
            Stat("Contra-relógio (palavras)", progress.timeAttackBest?.toString() ?: "—", colors)
            val free = stats.freeBest.toSortedMap()
            if (free.isEmpty()) {
                Stat("Jogo livre", "—", colors)
            } else {
                free.forEach { (size, seconds) -> Stat("Jogo livre ${size}×$size", mmss(seconds), colors) }
            }
        }
    }
}

/** Achievements grid: unlocked ones with their name; locked ones greyed out with the requirement visible. */
@Composable
fun AchievementsScreen(colors: CruzadasColors, stats: StatsData, onBack: () -> Unit) {
    val unlocked = stats.unlocked
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ScreenHeader("Conquistas ${unlocked.size} de ${Achievement.entries.size}", colors, onBack)
        LazyVerticalGrid(columns = GridCells.Fixed(2), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(Achievement.entries) { a ->
                val done = a.id in unlocked
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (done) colors.surface else colors.cell)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        if (done) a.title else "Bloqueada",
                        color = if (done) colors.accent else colors.mute,
                        fontFamily = Bricolage,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                    Text(a.description, color = if (done) colors.text else colors.mute, fontFamily = Figtree, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun Block(title: String, colors: CruzadasColors, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = colors.mute, fontFamily = Figtree, fontSize = 13.sp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surface)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun Stat(label: String, value: String, colors: CruzadasColors) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = colors.text, fontFamily = Figtree, modifier = Modifier.weight(1f))
        Box {
            Text(value, color = colors.accent, fontFamily = Bricolage, fontWeight = FontWeight.Bold)
        }
    }
}

/** Worlds with all 40 levels done. */
private fun worldsDone(done: Set<Int>): Int = WorldTable.worlds.count { world ->
    val first = (world.number - 1) * WorldTable.LEVELS_PER_WORLD + 1
    (first until first + WorldTable.LEVELS_PER_WORLD).all { it in done }
}

private fun mmss(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

private fun formatHours(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h} h ${m} min" else "$m min"
}
