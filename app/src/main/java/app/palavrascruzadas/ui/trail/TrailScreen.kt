package app.palavrascruzadas.ui.trail

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.palavrascruzadas.core.campaign.Campaign
import app.palavrascruzadas.core.campaign.CampaignProgress
import app.palavrascruzadas.core.campaign.LevelType
import app.palavrascruzadas.core.campaign.World
import app.palavrascruzadas.core.campaign.WorldTable
import app.palavrascruzadas.ui.theme.Bricolage
import app.palavrascruzadas.ui.theme.CruzadasColors
import app.palavrascruzadas.ui.theme.Figtree
import kotlin.math.sin

/** Visible state of a level on the trail. */
enum class NodeState { DONE, CURRENT, OPEN, LOCKED }

private val ROW_HEIGHT = 96.dp
private val NODE_SIZE = 56.dp
private val NODE_TOP = 4.dp

/** Trail screen: 600 levels in one column, level 1 at the bottom. */
@Composable
fun TrailScreen(
    progress: CampaignProgress,
    colors: CruzadasColors,
    onOpenLevel: (Int) -> Unit,
    onBack: () -> Unit,
    /** "Reduce motion": turns off the pulse of the current level. */
    reduceMotion: Boolean = false,
) {
    val current = progress.currentLevel
    val listState = rememberLazyListState()
    LaunchedEffect(current) {
        listState.scrollToItem(indexOf(current))
    }
    Box(modifier = Modifier.fillMaxSize().background(colors.bg).statusBarsPadding().navigationBarsPadding()) {
        LazyColumn(
            state = listState,
            reverseLayout = true,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 72.dp, bottom = 128.dp),
        ) {
            // Reversed list: index 0 is at the bottom. Each world starts with its banner (below
            // the world's level 1) followed by its 40 levels. Sticky headers combined with
            // reverseLayout stuck to the bottom and showed the wrong world; the world in view is
            // now shown in the top bar.
            WorldTable.worlds.forEach { world ->
                item(key = "world-${world.number}") { WorldHeader(world, progress, colors) }
                items(WorldTable.LEVELS_PER_WORLD, key = { "level-${(world.number - 1) * WorldTable.LEVELS_PER_WORLD + it + 1}" }) { index ->
                    val number = (world.number - 1) * WorldTable.LEVELS_PER_WORLD + index + 1
                    val state = stateOf(number, progress)
                    NodeRow(
                        number = number,
                        position = index + 1,
                        type = WorldTable.typeAt(index + 1),
                        stars = progress.levels[number]?.stars ?: 0,
                        state = state,
                        colors = colors,
                        pulse = !reduceMotion,
                        onClick = { onOpenLevel(number) },
                    )
                }
            }
        }
        // World in view: the one of the topmost item on screen (the highest index, in the reversed list).
        val visibleWorld by remember {
            derivedStateOf {
                val top = listState.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: indexOf(current)
                (top / (WorldTable.LEVELS_PER_WORLD + 1)).coerceIn(0, WorldTable.worlds.lastIndex)
            }
        }
        val world = WorldTable.worlds[visibleWorld]
        val first = (world.number - 1) * WorldTable.LEVELS_PER_WORLD + 1
        val numbers = first until first + WorldTable.LEVELS_PER_WORLD
        Row(
            modifier = Modifier.fillMaxWidth().background(colors.bg).padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Início", tint = colors.text)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Mundo ${world.number} · ${world.name}", color = colors.text, fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1)
                Text(
                    "${numbers.count { it in progress.levels }} de ${WorldTable.LEVELS_PER_WORLD} · ★ ${numbers.sumOf { progress.levels[it]?.stars ?: 0 }}",
                    color = colors.mute,
                    fontFamily = Figtree,
                    fontSize = 12.sp,
                )
            }
        }
        Button(
            onClick = { onOpenLevel(current) },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
        ) {
            Text("Continuar · nível $current", fontFamily = Figtree, fontWeight = FontWeight.Bold)
        }
    }
}

/** Item index of a level in the reversed list: each world takes one banner and then 40 nodes. */
internal fun indexOf(number: Int): Int {
    val world = (number - 1) / WorldTable.LEVELS_PER_WORLD
    val position = (number - 1) % WorldTable.LEVELS_PER_WORLD
    return world * (WorldTable.LEVELS_PER_WORLD + 1) + 1 + position
}

internal fun stateOf(number: Int, progress: CampaignProgress): NodeState = when {
    number in progress.levels -> NodeState.DONE
    number == progress.currentLevel -> NodeState.CURRENT
    Campaign.isUnlocked(number, progress) -> NodeState.OPEN
    else -> NodeState.LOCKED
}

@Composable
private fun NodeRow(
    number: Int,
    position: Int,
    type: LevelType,
    stars: Int,
    state: NodeState,
    colors: CruzadasColors,
    pulse: Boolean,
    onClick: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(ROW_HEIGHT).clipToBounds()) {
        val nodeCentre = maxWidth * xOf(number)
        // Dashed line to the centres of the neighbours (one row above and one below), clipped to the
        // row bounds. Both halves meet exactly at the boundary.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()))
            val rowPx = ROW_HEIGHT.toPx()
            val centre = Offset(nodeCentre.toPx(), NODE_TOP.toPx() + NODE_SIZE.toPx() / 2)
            val total = WorldTable.worlds.size * WorldTable.LEVELS_PER_WORLD
            if (number < total) {
                drawLine(
                    color = colors.cell,
                    start = Offset((maxWidth * xOf(number + 1)).toPx(), centre.y - rowPx),
                    end = centre,
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = dash,
                )
            }
            if (number > 1) {
                drawLine(
                    color = colors.cell,
                    start = centre,
                    end = Offset((maxWidth * xOf(number - 1)).toPx(), centre.y + rowPx),
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = dash,
                )
            }
        }
        val clickable = state != NodeState.LOCKED
        Column(
            modifier = Modifier
                .offset(x = nodeCentre - NODE_SIZE / 2, y = NODE_TOP)
                .width(NODE_SIZE),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NodeBubble(number, type, state, colors, clickable, pulse, onClick)
            when {
                stars > 0 -> Text("★".repeat(stars) + "☆".repeat(3 - stars), color = colors.accent, fontSize = 10.sp)
                // Special level type: wheel, keyword, diretas and fit-it.
                typeLabel(type) != null -> Text(typeLabel(type)!!, color = colors.mute, fontFamily = Figtree, fontSize = 10.sp, maxLines = 1, softWrap = false)
            }
        }
    }
}

private fun typeLabel(type: LevelType): String? = when (type) {
    LevelType.WHEEL -> "Roda"
    LevelType.KEYWORD -> "Chave"
    LevelType.DIRETAS -> "Diretas"
    LevelType.FIT_IT -> "Encaixar"
    else -> null
}

/** Horizontal position (fraction of the width) of a level on the trail. */
private fun xOf(number: Int): Float = 0.5f + sin(number * 0.95f) * 0.26f

@Composable
private fun NodeBubble(
    number: Int,
    type: LevelType,
    state: NodeState,
    colors: CruzadasColors,
    clickable: Boolean,
    pulse: Boolean,
    onClick: () -> Unit,
) {
    val breathing = if (state == NodeState.CURRENT && pulse) {
        val transition = rememberInfiniteTransition(label = "current")
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(animation = tween(1400), repeatMode = RepeatMode.Reverse),
            label = "breathing",
        ).value
    } else {
        1f
    }
    val boss = type == LevelType.BOSS
    val shape = if (boss) RoundedCornerShape(12.dp) else CircleShape
    val fill = when (state) {
        NodeState.DONE -> colors.accent
        NodeState.CURRENT, NodeState.OPEN -> colors.surface
        NodeState.LOCKED -> colors.cell
    }
    val textColor = when (state) {
        NodeState.DONE -> colors.onAccent
        NodeState.LOCKED -> colors.mute
        else -> colors.text
    }
    Box(
        modifier = Modifier
            .size(NODE_SIZE)
            .scale(breathing)
            .clip(shape)
            .background(if (boss) colors.palette[3 % colors.palette.size].copy(alpha = if (state == NodeState.LOCKED) 0.3f else 1f) else fill)
            .then(if (state == NodeState.CURRENT) Modifier.border(2.dp, colors.accent, shape) else Modifier)
            .clickable(enabled = clickable, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (boss) "♛ $number" else number.toString(),
            color = if (boss && state != NodeState.LOCKED) colors.onAccent else textColor,
            fontFamily = Figtree,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun WorldHeader(world: World, progress: CampaignProgress, colors: CruzadasColors) {
    val first = (world.number - 1) * WorldTable.LEVELS_PER_WORLD + 1
    val numbers = first until first + WorldTable.LEVELS_PER_WORLD
    val done = numbers.count { it in progress.levels }
    val starTotal = numbers.sumOf { progress.levels[it]?.stars ?: 0 }
    val banner = colors.palette[(world.number - 1) % colors.palette.size]
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bg)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(modifier = Modifier.width(6.dp).height(36.dp).background(banner))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Mundo ${world.number} · ${world.name}",
                color = colors.text,
                fontFamily = Bricolage,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
            Text("$done de ${WorldTable.LEVELS_PER_WORLD} · ★ $starTotal", color = colors.mute, fontFamily = Figtree, fontSize = 12.sp)
        }
    }
}
