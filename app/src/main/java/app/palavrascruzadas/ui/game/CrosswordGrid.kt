package app.palavrascruzadas.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import app.palavrascruzadas.core.model.Dir
import app.palavrascruzadas.core.model.Pos
import app.palavrascruzadas.core.play.Checker
import app.palavrascruzadas.core.play.InputLogic
import app.palavrascruzadas.core.play.PlayState
import app.palavrascruzadas.ui.theme.Bricolage
import app.palavrascruzadas.ui.theme.CruzadasColors
import app.palavrascruzadas.ui.theme.Figtree
import app.palavrascruzadas.ui.theme.colorblindSymbol

/** Inner padding of the board (`surface` background with rounded corners). */
private val BOARD_PAD = 8.dp

/**
 * Grid drawn on a single Canvas: cells, colour of each solved word,
 * neon selection of the active entry, letters, numbers and errors in red.
 *
 * Takes whatever space it is given: the cell is the largest size that fits both the width **and** the height,
 * and the board is centred. This way tall grids do not push the keyboard off screen.
 */
@Composable
fun CrosswordGrid(
    play: PlayState,
    flashWrong: Set<Pos>,
    colors: CruzadasColors,
    onTap: (Pos) -> Unit,
    modifier: Modifier = Modifier,
    /** Keyword cells with the index (1, 2, …) shown in the corner. */
    keywordIndex: Map<Pos, Int> = emptyMap(),
    /** Grid letter scale (90–130 %). */
    scale: Float = 1f,
    /** Colour-blind mode: each solved word gets a symbol in its cells. */
    colorblind: Boolean = false,
    /** Fit-it: cells where the chosen word fits by length, with a dashed outline. */
    hintCells: Set<Pos> = emptySet(),
) {
    val puzzle = play.puzzle
    val tap by rememberUpdatedState(onTap)
    val measurer = rememberTextMeasurer()
    val activeEntry = InputLogic.activeEntry(play)
    val activeCells = activeEntry?.let { puzzle.entries[it].cells.toSet() }.orEmpty()
    val numbers = puzzle.entries.groupBy { Pos(it.row, it.col) }.mapValues { (_, list) -> list.minOf { it.number } }
    // Without "confirm correct words", nothing is coloured before the whole grid is right.
    val showWordColors = play.options.confirmWords || play.isComplete
    val wordColor = HashMap<Pos, Color>()
    val wordSymbol = HashMap<Pos, String>()
    if (showWordColors) {
        play.found.sorted().forEach { index ->
            val color = colors.palette[index % colors.palette.size]
            puzzle.entries[index].cells.forEach {
                wordColor.putIfAbsent(it, color)
                if (colorblind) wordSymbol.putIfAbsent(it, colorblindSymbol(index % colors.palette.size))
            }
        }
    }
    val autoWrong = if (play.options.autoCheck) play.typed.keys.filter { Checker.isWrong(play, it) }.toSet() else emptySet()
    val wrong = flashWrong + autoWrong

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val usableW = maxWidth - BOARD_PAD * 2
        val usableH = if (constraints.hasBoundedHeight) maxHeight - BOARD_PAD * 2 else Dp.Infinity
        val cellDp = min(usableW / puzzle.width, usableH / puzzle.height)
        val gridW = cellDp * puzzle.width
        val gridH = cellDp * puzzle.height
        Box(
            modifier = Modifier
                .size(gridW + BOARD_PAD * 2, gridH + BOARD_PAD * 2)
                .clip(RoundedCornerShape(24.dp))
                .background(colors.surface)
                .padding(BOARD_PAD),
        ) {
            Canvas(
                modifier = Modifier
                    .size(gridW, gridH)
                    .pointerInput(puzzle) {
                        detectTapGestures { offset ->
                            val cell = size.width / puzzle.width.toFloat()
                            if (offset.x < 0 || offset.y < 0) return@detectTapGestures
                            val pos = Pos((offset.y / cell).toInt(), (offset.x / cell).toInt())
                            if (pos in puzzle.solution || puzzle.clueCells.any { it.pos == pos }) tap(pos)
                        }
                    },
            ) {
                val cell = size.width / puzzle.width
                val pad = cell * 0.05f
                val radius = CornerRadius(cell * 0.16f)
                // Font sizes in px converted with toSp(): they do not grow with the system
                // font scale, which would push the letters out of the cells.
                fun px(fraction: Float) = (cell * fraction).toSp()

                // Diretas clue cells: the short clue and the arrow of each entry starting next to it.
                for (clue in puzzle.clueCells) {
                    val topLeft = Offset(clue.pos.col * cell + pad, clue.pos.row * cell + pad)
                    val box = Size(cell - 2 * pad, cell - 2 * pad)
                    drawRoundRect(color = colors.cell.copy(alpha = 0.55f), topLeft = topLeft, size = box, cornerRadius = radius)
                    val arrows = clue.arrows.sortedBy { it.ordinal }
                    val slot = box.height / arrows.size
                    arrows.forEachIndexed { k, dir ->
                        val start = if (dir == Dir.ACROSS) Pos(clue.pos.row, clue.pos.col + 1) else Pos(clue.pos.row + 1, clue.pos.col)
                        val index = puzzle.entries.indexOfFirst { it.dir == dir && it.row == start.row && it.col == start.col }
                        if (index < 0) return@forEachIndexed
                        val entry = puzzle.entries[index]
                        val isActive = index == activeEntry
                        val text = measurer.measure(
                            text = entry.shortClue ?: entry.clue,
                            style = TextStyle(
                                fontFamily = Figtree,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                fontSize = px(0.15f),
                                lineHeight = px(0.17f),
                                color = if (isActive) colors.accent else colors.text,
                            ),
                            constraints = Constraints(maxWidth = (box.width * 0.9f).toInt(), maxHeight = (slot * 0.78f).toInt()),
                        )
                        drawText(text, topLeft = Offset(topLeft.x + box.width * 0.05f, topLeft.y + slot * k + slot * 0.06f))
                        val arrow = measurer.measure(
                            text = if (dir == Dir.ACROSS) "→" else "↓",
                            style = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Bold, fontSize = px(0.2f), color = colors.accent),
                        )
                        drawText(arrow, topLeft = Offset(topLeft.x + box.width - arrow.size.width - box.width * 0.04f, topLeft.y + slot * (k + 1) - arrow.size.height))
                    }
                }

                val cursor = play.cursor?.pos
                for ((pos, _) in play.solution) {
                    val topLeft = Offset(pos.col * cell + pad, pos.row * cell + pad)
                    val box = Size(cell - 2 * pad, cell - 2 * pad)
                    val coloured = pos in wordColor
                    // Neon: the active entry keeps its normal fill with an outline; the active cell
                    // gets a tinted fill, a stronger outline and a glow made of rings.
                    val fill = when {
                        coloured -> wordColor.getValue(pos)
                        pos == cursor -> colors.accent.copy(alpha = 0.18f).compositeOver(colors.cell)
                        else -> colors.cell
                    }
                    drawRoundRect(color = fill, topLeft = topLeft, size = box, cornerRadius = radius)
                    if (pos in activeCells && pos != cursor) {
                        drawRoundRect(
                            color = colors.accent.copy(alpha = 0.75f),
                            topLeft = topLeft,
                            size = box,
                            cornerRadius = radius,
                            style = Stroke(width = 1.5.dp.toPx()),
                        )
                    }
                    if (pos in hintCells) {
                        drawRoundRect(
                            color = colors.accent,
                            topLeft = topLeft,
                            size = box,
                            cornerRadius = radius,
                            style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
                        )
                    }
                    if (pos == cursor) {
                        listOf(7.dp to 0.10f, 5.dp to 0.20f, 3.5.dp to 0.35f).forEach { (w, a) ->
                            drawRoundRect(
                                color = colors.accent.copy(alpha = a),
                                topLeft = topLeft,
                                size = box,
                                cornerRadius = radius,
                                style = Stroke(width = w.toPx()),
                            )
                        }
                        drawRoundRect(
                            color = colors.accent,
                            topLeft = topLeft,
                            size = box,
                            cornerRadius = radius,
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    }
                    val letter = if (pos in play.revealed) play.solution.getValue(pos) else play.typed[pos]
                    if (letter != null) {
                        val isWrong = pos in wrong && !coloured
                        val textColor = when {
                            isWrong -> colors.error
                            coloured -> colors.onAccent
                            else -> colors.text
                        }
                        val layout = measurer.measure(
                            text = letter.toString(),
                            style = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = px(0.48f * scale), color = textColor),
                        )
                        val origin = Offset(pos.col * cell + (cell - layout.size.width) / 2f, pos.row * cell + (cell - layout.size.height) / 2f)
                        drawText(textLayoutResult = layout, topLeft = origin)
                        if (isWrong) {
                            // Error: red text with an underline, so it is not confused with the pink word colour.
                            val y = pos.row * cell + cell * 0.82f
                            drawLine(colors.error, Offset(pos.col * cell + cell * 0.3f, y), Offset(pos.col * cell + cell * 0.7f, y), strokeWidth = 2.dp.toPx())
                        }
                    }
                    keywordIndex[pos]?.let { index ->
                        val dot = cell * 0.11f
                        val centre = Offset(pos.col * cell + cell * 0.80f, pos.row * cell + cell * 0.80f)
                        drawCircle(color = if (coloured) colors.onAccent else colors.accent, radius = dot, center = centre)
                        val layout = measurer.measure(
                            text = index.toString(),
                            style = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.Bold, fontSize = px(0.14f), color = if (coloured) wordColor.getValue(pos) else colors.onAccent),
                        )
                        drawText(textLayoutResult = layout, topLeft = Offset(centre.x - layout.size.width / 2f, centre.y - layout.size.height / 2f))
                    }
                    wordSymbol[pos]?.let { symbol ->
                        val layout = measurer.measure(
                            text = symbol,
                            style = TextStyle(fontFamily = Figtree, fontSize = px(0.2f), color = colors.onAccent.copy(alpha = 0.8f)),
                        )
                        drawText(textLayoutResult = layout, topLeft = Offset(pos.col * cell + cell * 0.10f, pos.row * cell + cell * 0.62f))
                    }
                    numbers[pos]?.let { n ->
                        val layout = measurer.measure(
                            text = n.toString(),
                            style = TextStyle(
                                fontFamily = Figtree,
                                fontWeight = FontWeight.Medium,
                                fontSize = px(0.2f),
                                color = if (coloured) colors.onAccent.copy(alpha = 0.7f) else colors.mute,
                            ),
                        )
                        drawText(textLayoutResult = layout, topLeft = Offset(pos.col * cell + cell * 0.12f, pos.row * cell + cell * 0.07f))
                    }
                }
            }
            // Accessibility layer (TalkBack): one speakable node per solution cell.
            Box(modifier = Modifier.size(gridW, gridH).semantics { contentDescription = gridDescription(play) }) {
                play.solution.keys.forEach { pos ->
                    Box(
                        modifier = Modifier
                            .offset(x = cellDp * pos.col, y = cellDp * pos.row)
                            .size(cellDp)
                            .semantics { contentDescription = cellDescription(play, pos) },
                    )
                }
            }
        }
    }
}
