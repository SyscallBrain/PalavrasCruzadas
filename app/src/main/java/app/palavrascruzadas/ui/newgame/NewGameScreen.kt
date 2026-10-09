package app.palavrascruzadas.ui.newgame

import app.palavrascruzadas.ui.components.ScreenHeader
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import app.palavrascruzadas.core.model.Difficulty
import app.palavrascruzadas.ui.theme.Bricolage
import app.palavrascruzadas.ui.theme.CruzadasColors
import app.palavrascruzadas.ui.theme.Figtree

/** Free play setup: size, difficulty and theme. */
@Composable
fun NewGameScreen(
    colors: CruzadasColors,
    onBack: () -> Unit,
    onStart: (size: Int, difficulty: Difficulty, theme: String?) -> Unit,
) {
    var size by remember { mutableStateOf(7) }
    var difficulty by remember { mutableStateOf(Difficulty.MEDIUM) }
    var theme by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ScreenHeader("Jogo livre", colors, onBack)

        Section("Tamanho", colors) {
            ChipRow(listOf(5, 7, 9, 11, 13), size, colors, { "$it×$it" }) { size = it }
        }
        Section("Dificuldade", colors) {
            ChipRow(
                listOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD),
                difficulty,
                colors,
                { difficultyName(it) },
            ) { difficulty = it }
        }
        Section("Tema", colors) {
            val options: List<String?> = listOf(null) + THEMES.map { it.first }
            ChipRow(options, theme, colors, { id -> THEMES.firstOrNull { it.first == id }?.second ?: "Todos" }) { theme = it }
        }

        Button(
            onClick = { onStart(size, difficulty, theme) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
        ) {
            Text("Começar", fontFamily = Figtree, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Section(title: String, colors: CruzadasColors, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = colors.mute, fontFamily = Figtree, fontSize = 13.sp)
        content()
    }
}

/**
 * Row of options with one selected. Wraps to the available width (with a fixed 4 per row,
 * names such as "Música e arte" ran off the screen).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipRow(options: List<T>, selected: T, colors: CruzadasColors, label: (T) -> String, onPick: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val chosen = option == selected
            Text(
                label(option),
                color = if (chosen) colors.onAccent else colors.text,
                fontFamily = Figtree,
                fontWeight = if (chosen) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (chosen) colors.accent else colors.surface)
                    .clickable { onPick(option) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

private fun difficultyName(d: Difficulty): String = when (d) {
    Difficulty.EASY -> "Fácil"
    Difficulty.MEDIUM -> "Médio"
    Difficulty.HARD -> "Difícil"
}

/** Bank themes (`content/themes.json`) with their display names. */
val THEMES: List<Pair<String, String>> = listOf(
    "comida" to "Comida",
    "animais" to "Animais",
    "natureza" to "Natureza",
    "geografia" to "Geografia",
    "desporto" to "Desporto",
    "corpo" to "Corpo",
    "casa" to "Casa",
    "ciencia" to "Ciência",
    "musica-arte" to "Música e arte",
    "profissoes" to "Profissões",
    "historia-pt" to "História",
    "transportes" to "Transportes",
    "mar" to "Mar",
    "lingua" to "Língua",
    "geral" to "Geral",
)
