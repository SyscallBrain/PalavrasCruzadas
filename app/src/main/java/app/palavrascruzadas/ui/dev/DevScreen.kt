package app.palavrascruzadas.ui.dev

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.palavrascruzadas.core.campaign.WorldTable
import app.palavrascruzadas.core.daily.DailyPuzzle
import app.palavrascruzadas.core.model.Dir
import app.palavrascruzadas.core.model.Puzzle
import app.palavrascruzadas.core.play.KeywordLogic
import app.palavrascruzadas.core.play.PlayState
import app.palavrascruzadas.data.BankRepository
import app.palavrascruzadas.data.CampaignRepository
import app.palavrascruzadas.data.DailyRepository
import app.palavrascruzadas.ui.components.ScreenHeader
import app.palavrascruzadas.ui.game.CrosswordGrid
import app.palavrascruzadas.ui.theme.Bricolage
import app.palavrascruzadas.ui.theme.CruzadasColors
import app.palavrascruzadas.ui.theme.Figtree
import java.time.LocalDate

private val TOTAL_LEVELS = WorldTable.worlds.size * WorldTable.LEVELS_PER_WORLD

/** What is shown: a puzzle's solution and, for the wheel, the base word and the bonus words. */
private data class Shown(val puzzle: Puzzle, val base: String? = null, val bonus: List<String> = emptyList())

/**
 * Developer menu (test builds only): solutions of the campaign levels and of the daily puzzle,
 * playing any level without unlocking it, and the simulated date.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DevScreen(
    colors: CruzadasColors,
    currentLevel: Int,
    today: LocalDate,
    onBack: () -> Unit,
    onPlayLevel: (Int) -> Unit,
    onSimulatedDate: () -> Unit,
) {
    val context = LocalContext.current
    var daily by rememberSaveable { mutableStateOf(false) }
    var level by rememberSaveable { mutableIntStateOf(currentLevel) }
    val shown by produceState<Shown?>(initialValue = null, daily, level, today) {
        value = null
        value = if (daily) {
            val index = DailyPuzzle.preGeneratedIndex(today)
            val puzzle = if (index != null) DailyRepository.puzzles(context)[index] else DailyPuzzle.generate(BankRepository.load(context), today)
            Shown(puzzle)
        } else {
            val spec = CampaignRepository.level(context, level)
            Shown(spec.puzzle, spec.base, spec.bonus)
        }
    }

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
        ScreenHeader("Programador", colors, onBack)

        FlowRow(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Campanha", !daily, colors) { daily = false }
            Chip("Diário de hoje", daily, colors) { daily = true }
            Chip("Data simulada", false, colors, onClick = onSimulatedDate)
        }

        if (daily) {
            Text("Diário de $today", color = colors.text, fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        } else {
            val world = WorldTable.worlds[(level - 1) / WorldTable.LEVELS_PER_WORLD]
            Text(
                "Nível $level · Mundo ${world.number} · ${world.name} · ${WorldTable.typeAt((level - 1) % WorldTable.LEVELS_PER_WORLD + 1).name.lowercase()}",
                color = colors.text,
                fontFamily = Bricolage,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
            FlowRow(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(-40, -10, -1, 1, 10, 40).forEach { step ->
                    Chip(if (step > 0) "+$step" else "$step", false, colors) {
                        level = (level + step).coerceIn(1, TOTAL_LEVELS)
                    }
                }
            }
            FlowRow(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Nível atual ($currentLevel)", false, colors) { level = currentLevel }
                Button(
                    onClick = { onPlayLevel(level) },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
                ) { Text("Jogar este nível", fontFamily = Figtree, fontWeight = FontWeight.Bold) }
            }
        }

        val current = shown
        if (current == null) {
            Text("A carregar…", color = colors.mute, fontFamily = Figtree)
        } else {
            Solution(current, colors)
        }
    }
}

@Composable
private fun Solution(shown: Shown, colors: CruzadasColors) {
    val puzzle = shown.puzzle
    // Everything revealed and no cursor: the grid shows the solution, without solved-word colours.
    val solved = PlayState(puzzle = puzzle, revealed = puzzle.solution.keys)
    CrosswordGrid(
        play = solved,
        flashWrong = emptySet(),
        colors = colors,
        onTap = {},
        modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
        keywordIndex = KeywordLogic.indexes(puzzle),
    )
    puzzle.keyword?.let { Line("Palavra-chave", it.word, colors) }
    shown.base?.let { Line("Roda", it, colors) }
    if (shown.bonus.isNotEmpty()) Line("Bónus", shown.bonus.joinToString(", "), colors)
    Text("${puzzle.id} · ${puzzle.width}×${puzzle.height} · alvo ${puzzle.targetSeconds} s", color = colors.mute, fontFamily = Figtree, fontSize = 12.sp)
    puzzle.entries
        .sortedWith(compareBy({ it.dir.ordinal }, { it.number }))
        .forEach { e ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "${e.number}${if (e.dir == Dir.ACROSS) "H" else "V"}",
                    color = colors.accent,
                    fontFamily = Bricolage,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
                Column {
                    Text(e.answer, color = colors.text, fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(e.clue, color = colors.mute, fontFamily = Figtree, fontSize = 12.sp)
                }
            }
        }
}

@Composable
private fun Line(label: String, value: String, colors: CruzadasColors) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = colors.mute, fontFamily = Figtree, fontSize = 13.sp)
        Text(value, color = colors.text, fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun Chip(label: String, chosen: Boolean, colors: CruzadasColors, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (chosen) colors.accent else colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Text(label, color = if (chosen) colors.onAccent else colors.text, fontFamily = Figtree, fontSize = 13.sp)
    }
}
