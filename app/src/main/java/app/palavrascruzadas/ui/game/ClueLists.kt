package app.palavrascruzadas.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.palavrascruzadas.core.model.Dir
import app.palavrascruzadas.core.play.InputLogic
import app.palavrascruzadas.core.play.PlayState
import app.palavrascruzadas.ui.theme.Bricolage
import app.palavrascruzadas.ui.theme.CruzadasColors
import app.palavrascruzadas.ui.theme.Figtree

/**
 * Clues in two columns, across on the left and down on the right. The active entry
 * is highlighted; solved ones are struck through. Tapping a clue selects its entry.
 */
@Composable
fun ClueLists(play: PlayState, colors: CruzadasColors, onPick: (Int) -> Unit, modifier: Modifier = Modifier) {
    val active = InputLogic.activeEntry(play)
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(Dir.ACROSS to "Horizontais", Dir.DOWN to "Verticais").forEach { (dir, title) ->
            val entries = play.puzzle.entries.withIndex()
                .filter { it.value.dir == dir }
                .sortedBy { it.value.number }
            // Each list takes half the width (without a weight, the first one took everything and the second disappeared).
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, color = colors.mute, fontFamily = Figtree, fontSize = 12.sp)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(entries) { (index, entry) ->
                        val isActive = index == active
                        val isDone = index in play.found
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isActive) colors.accent.copy(alpha = 0.22f) else colors.surface)
                                .clickable { onPick(index) }
                                .padding(8.dp),
                        ) {
                            Text(
                                "${entry.number} · ${entry.length} letras",
                                color = colors.accent,
                                fontFamily = Bricolage,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                            )
                            Text(
                                entry.shortClue ?: entry.clue,
                                color = if (isDone) colors.mute else colors.text,
                                fontFamily = Figtree,
                                fontSize = 13.sp,
                                textDecoration = if (isDone) TextDecoration.LineThrough else null,
                            )
                        }
                    }
                }
            }
        }
    }
}
