package app.palavrascruzadas.ui.game

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.palavrascruzadas.ui.theme.CruzadasColors
import app.palavrascruzadas.ui.theme.Figtree

/** Accented variants of each vowel and of C, shown on long press. */
private val ACCENTS = mapOf(
    'A' to listOf('Á', 'À', 'Â', 'Ã'),
    'E' to listOf('É', 'Ê'),
    'I' to listOf('Í'),
    'O' to listOf('Ó', 'Ô', 'Õ'),
    'U' to listOf('Ú'),
    'C' to listOf('Ç'),
)

private val ROWS = listOf("QWERTYUIOP", "ASDFGHJKLÇ", "ZXCVBNM")

/** Top row with the accented letters, shown by the "´~" key. */
private const val ACCENT_ROW = "ÁÀÂÃÉÊÍÓÔÕÚ"

/**
 * In-app keyboard: three rows, ✓ (check word), "´~" and ⌫.
 * Long press on A, E, I, O, U and C opens the accented variants; tap the one you want.
 * The "´~" key swaps the top row for the accented letters, for those who prefer taps;
 * after typing one of them, the keyboard goes back to normal.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameKeyboard(
    colors: CruzadasColors,
    onLetter: (Char) -> Unit,
    onBackspace: () -> Unit,
    onVerifyWord: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var accentRow by remember { mutableStateOf(false) }
    val rows = if (accentRow) listOf(ACCENT_ROW) + ROWS.drop(1) else ROWS
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEachIndexed { index, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (index == rows.lastIndex) {
                    KeyWide("✓", colors, description = "Verificar palavra") { onVerifyWord() }
                    KeyWide("´~", colors, description = "Letras acentuadas", active = accentRow, filled = false) { accentRow = !accentRow }
                }
                row.forEach { letter ->
                    val accented = accentRow && index == 0
                    Key(letter, colors, accents = if (accented) emptyList() else ACCENTS[letter].orEmpty()) { typed ->
                        onLetter(typed)
                        if (accented) accentRow = false
                    }
                }
                if (index == rows.lastIndex) KeyWide("⌫", colors, description = "Apagar") { onBackspace() }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.Key(letter: Char, colors: CruzadasColors, accents: List<Char>, onLetter: (Char) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .weight(1f)
            .height(46.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .combinedClickable(
                onClick = { onLetter(letter) },
                onLongClick = { if (accents.isNotEmpty()) menu = true },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter.toString(),
            color = colors.text,
            fontFamily = Figtree,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            modifier = Modifier.semantics { contentDescription = keyDescription(letter) },
        )
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            accents.forEach { accent ->
                DropdownMenuItem(
                    text = { Text(accent.toString(), fontSize = 20.sp) },
                    onClick = {
                        menu = false
                        onLetter(accent)
                    },
                )
            }
        }
    }
}

@Composable
private fun RowScope.KeyWide(
    label: String,
    colors: CruzadasColors,
    description: String,
    active: Boolean = false,
    filled: Boolean = true,
    onClick: () -> Unit,
) {
    val background = when {
        filled || active -> colors.accent
        else -> colors.cell
    }
    Box(
        modifier = Modifier
            .weight(1.3f)
            .height(46.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (filled || active) colors.onAccent else colors.text,
            fontFamily = Figtree,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
    }
}
