package app.palavrascruzadas.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.rememberTextMeasurer

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.palavrascruzadas.core.play.PlayState
import app.palavrascruzadas.ui.theme.Bricolage
import app.palavrascruzadas.ui.theme.CruzadasColors
import app.palavrascruzadas.ui.theme.Figtree
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Positions of the wheel letters, on a circle, starting at the top. */
private fun wheelPositions(count: Int, width: Float, height: Float): List<Offset> {
    val cx = width / 2f
    val cy = height / 2f
    val r = min(width, height) * 0.36f
    return List(count) { i ->
        val angle = -PI / 2 + 2 * PI * i / count
        Offset(cx + r * cos(angle).toFloat(), cy + r * sin(angle).toFloat())
    }
}

/**
 * Letter wheel: dragging across the letters forms a word; on release, the word is
 * submitted. Shuffle and hint below.
 */
@Composable
fun WheelPad(
    letters: List<Char>,
    bonusText: String,
    colors: CruzadasColors,
    onWord: (String) -> Unit,
    onShuffle: () -> Unit,
    onHint: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var path by remember { mutableStateOf(listOf<Int>()) }
    val measurer = rememberTextMeasurer()
    val preview = path.joinToString("") { letters[it].toString() }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            preview.ifEmpty { bonusText },
            color = if (preview.isEmpty()) colors.mute else colors.accent,
            fontFamily = Bricolage,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.height(28.dp),
        )
        // The wheel takes the remaining space (up to 280 dp) so it fits with the grid on any screen.
        Box(modifier = Modifier.fillMaxWidth().weight(1f).heightIn(min = 150.dp, max = 280.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(letters) {
                        val radius = min(size.width, size.height) * 0.12f
                        fun hitAt(o: Offset): Int? {
                            val centres = wheelPositions(letters.size, size.width.toFloat(), size.height.toFloat())
                            return centres.indices.firstOrNull { (centres[it] - o).getDistance() <= radius * 1.2f }
                        }
                        detectDragGestures(
                            onDragStart = { o -> path = listOfNotNull(hitAt(o)) },
                            onDrag = { change, _ ->
                                change.consume()
                                hitAt(change.position)?.let { i ->
                                    when {
                                        i !in path -> path = path + i
                                        // Going back to the previous letter undoes the last one (as in the usual wheels).
                                        path.size >= 2 && i == path[path.size - 2] -> path = path.dropLast(1)
                                    }
                                }
                            },
                            onDragEnd = {
                                if (path.size >= 2) onWord(path.joinToString("") { letters[it].toString() })
                                path = emptyList()
                            },
                            onDragCancel = { path = emptyList() },
                        )
                    },
            ) {
                val centres = wheelPositions(letters.size, size.width, size.height)
                val radius = min(size.width, size.height) * 0.12f
                // Wheel ring.
                drawCircle(color = colors.cell, radius = min(size.width, size.height) * 0.36f, center = Offset(size.width / 2f, size.height / 2f), style = Stroke(width = 2.dp.toPx()))
                // Path already traced.
                for (k in 1 until path.size) {
                    drawLine(
                        color = colors.accent,
                        start = centres[path[k - 1]],
                        end = centres[path[k]],
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
                centres.forEachIndexed { i, centre ->
                    val chosen = i in path
                    drawCircle(color = if (chosen) colors.accent else colors.surface, radius = radius, center = centre)
                    val layout = measurer.measure(
                        text = letters[i].toString(),
                        style = TextStyle(
                            fontFamily = Bricolage,
                            fontWeight = FontWeight.Bold,
                            fontSize = (radius * 1.1f).toSp(),
                            color = if (chosen) colors.onAccent else colors.text,
                        ),
                    )
                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(centre.x - layout.size.width / 2f, centre.y - layout.size.height / 2f),
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onShuffle) { Text("Baralhar", color = colors.accent, fontFamily = Figtree) }
            TextButton(onClick = onHint) { Text("Ajuda · 1", color = colors.accent, fontFamily = Figtree) }
        }
    }
}

/**
 * Fit-it box: the words still to place, grouped by length. Pick one and
 * tap the cell where you want it; the placed ones are struck through.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FitBank(play: PlayState, order: List<String>, selected: String?, colors: CruzadasColors, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val placed = play.found.map { play.puzzle.entries[it].answer }
    Column(modifier = modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            if (selected == null) "Escolhe uma palavra e toca na casa onde a queres pôr." else "Toca numa das casas tracejadas.",
            color = colors.mute,
            fontFamily = Figtree,
            fontSize = 12.sp,
        )
        order.groupBy { it.length }.toSortedMap().forEach { (length, words) ->
            Text("$length LETRAS", color = colors.mute, fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, letterSpacing = 0.8.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Words repeated in the grid: each copy is struck through as it gets placed.
                val remaining = placed.toMutableList()
                words.forEach { word ->
                    val isPlaced = remaining.remove(word)
                    val isSelected = !isPlaced && word == selected
                    Text(
                        word,
                        color = when {
                            isPlaced -> colors.mute
                            isSelected -> colors.onAccent
                            else -> colors.text
                        },
                        fontFamily = Bricolage,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp,
                        textDecoration = if (isPlaced) TextDecoration.LineThrough else null,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (isSelected) colors.accent else colors.surface)
                            .clickable(enabled = !isPlaced) { onSelect(word) }
                            .padding(horizontal = 11.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}
