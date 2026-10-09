package app.palavrascruzadas.ui.game

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import app.palavrascruzadas.core.play.Checker
import app.palavrascruzadas.core.play.StarResult
import app.palavrascruzadas.ui.home.BulbIcon
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.palavrascruzadas.core.model.Dir
import app.palavrascruzadas.core.model.PuzzleKind
import app.palavrascruzadas.core.text.Alphabet
import android.content.res.Configuration
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import app.palavrascruzadas.ui.layout.GameLayout
import app.palavrascruzadas.ui.layout.chooseGameLayout
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.platform.LocalConfiguration
import app.palavrascruzadas.core.play.InputLogic
import app.palavrascruzadas.core.play.KeywordLogic
import app.palavrascruzadas.core.play.PlayState
import app.palavrascruzadas.core.play.Scoring
import app.palavrascruzadas.ui.theme.Bricolage
import app.palavrascruzadas.ui.theme.CruzadasColors
import app.palavrascruzadas.ui.theme.Figtree
import app.palavrascruzadas.ui.theme.LocalCruzadasColors
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/**
 * Game screen. On a phone, a single column: header, progress, board (takes
 * the remaining space), clue bar, hints and the keyboard always at the bottom. Two columns on
 * tablets and in landscape. The win and time-attack panels show on top of the game.
 */
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    title: String,
    subtitle: String,
    onExit: () -> Unit,
    /** Opens the next level or puzzle; null when there is none. */
    onNext: (() -> Unit)? = null,
) {
    val ui by viewModel.ui.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val colors = LocalCruzadasColors.current
    val context = LocalContext.current
    // When the app goes to the background, the game is saved and paused. Rotating the screen does not count.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if ((context as? Activity)?.isChangingConfigurations == true) viewModel.saveNow() else viewModel.onAppStop()
    }
    val leave = {
        viewModel.saveNow()
        onExit()
    }

    LaunchedEffect(ui.message) {
        ui.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.onMessageShown()
        }
    }

    val configuration = LocalConfiguration.current
    val wide = chooseGameLayout(configuration.screenWidthDp.toFloat(), configuration.screenHeightDp.toFloat()) == GameLayout.TWO_COLUMNS
    // Physical keyboard attached and visible: the on-screen keyboard is hidden. Many phones
    // report a keyboard type without having one attached, hence the "hardKeyboardHidden" check.
    val hardwareKeyboard = configuration.keyboard == Configuration.KEYBOARD_QWERTY &&
        configuration.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .focusRequester(focus)
                .focusable()
                .onPreviewKeyEvent { event -> onHardwareKey(event, viewModel) },
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val attack = ui.timeAttack
            val play = ui.play
            Header(
                title = if (attack != null) "Contra-relógio" else title,
                subtitle = when {
                    attack != null -> "${attack.words} palavras"
                    play != null -> listOf(subtitle, "${play.puzzle.width}×${play.puzzle.height}").filter { it.isNotBlank() }.joinToString(" · ")
                    else -> subtitle
                },
                time = attack?.timeLeft ?: ui.elapsed,
                urgent = attack != null && attack.timeLeft <= 15,
                colors = colors,
                canPause = play != null && ui.finished == null && attack == null,
                showGridCheck = play != null && attack == null && ui.wheelBase == null,
                balance = ui.wallet.balance,
                onPause = viewModel::onPause,
                onExit = leave,
                onVerifyGrid = viewModel::onVerifyGrid,
            )

            when {
                ui.loading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("A gerar a grelha…", color = colors.mute, fontFamily = Figtree)
                }
                ui.error != null -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(ui.error!!, color = colors.error, fontFamily = Figtree)
                }
                play != null -> {
                    val wheel = ui.wheelBase
                    val fitHint = ui.fitSelected?.let { word ->
                        play.puzzle.entries.withIndex()
                            .filter { (i, e) -> i !in play.found && e.length == word.length }
                            .flatMap { it.value.cells }
                            .toSet()
                    }.orEmpty()
                    val board: @Composable (Modifier) -> Unit = { mod ->
                        Box(modifier = mod.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CrosswordGrid(
                                play = play,
                                flashWrong = ui.flashWrong,
                                colors = colors,
                                onTap = viewModel::onCellTap,
                                modifier = Modifier.fillMaxSize(),
                                keywordIndex = KeywordLogic.indexes(play.puzzle),
                                scale = settings.gridScale / 100f,
                                colorblind = settings.colorblind,
                                hintCells = fitHint,
                            )
                            if (ui.paused) PauseOverlay(colors = colors, onResume = viewModel::onResume)
                        }
                    }
                    val progress: @Composable () -> Unit = {
                        val total = play.puzzle.solution.size
                        // Without word confirmation, the bar shows filled cells (so it does not give away correct words).
                        val done = if (play.options.confirmWords) {
                            play.solution.keys.count { it in play.locked || it in play.revealed }
                        } else {
                            play.solution.keys.count { it in play.typed || it in play.revealed }
                        }
                        LinearProgressIndicator(
                            progress = { if (total == 0) 0f else done.toFloat() / total },
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                            color = colors.accent,
                            trackColor = colors.surface,
                            drawStopIndicator = {},
                        )
                    }
                    val keyword: @Composable () -> Unit = {
                        if (play.puzzle.keyword != null) {
                            KeywordStrip(play = play, draft = ui.keywordDraft, keywordMode = ui.keywordMode, colors = colors, onToggle = viewModel::onKeywordToggle)
                        }
                    }
                    val hints: @Composable () -> Unit = {
                        if (attack == null && wheel == null) {
                            HintRow(ui.wallet.balance, colors, onLetter = viewModel::onRevealLetter, onWord = viewModel::onRevealWord, onAnagram = viewModel::onAnagram)
                        }
                    }
                    val keyboard: @Composable () -> Unit = {
                        if (wheel == null && !hardwareKeyboard) {
                            GameKeyboard(
                                colors = colors,
                                onLetter = viewModel::onLetter,
                                onBackspace = viewModel::onBackspace,
                                onVerifyWord = viewModel::onVerifyWord,
                            )
                        }
                    }
                    val wheelPad: @Composable (Modifier) -> Unit = { mod ->
                        WheelPad(
                            letters = ui.wheelOrder,
                            bonusText = "Bónus ${ui.bonusFound.size} / ${ui.wheelBonus.size}",
                            colors = colors,
                            onWord = viewModel::onWheelWord,
                            onShuffle = viewModel::onWheelShuffle,
                            onHint = viewModel::onWheelHint,
                            modifier = mod,
                        )
                    }
                    val clueBar: @Composable () -> Unit = {
                        ClueBar(play = play, colors = colors, onPrevious = { viewModel.onNextEntry(-1) }, onNext = { viewModel.onNextEntry(1) })
                    }
                    if (wide) {
                        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                progress()
                                board(Modifier.weight(1f))
                                keyword()
                            }
                            Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                when {
                                    wheel != null -> wheelPad(Modifier.weight(1f))
                                    play.puzzle.kind == PuzzleKind.FIT_IT -> FitBank(play, ui.fitOrder, ui.fitSelected, colors, viewModel::onFitSelect, Modifier.weight(1f))
                                    else -> {
                                        clueBar()
                                        ClueLists(play = play, colors = colors, onPick = viewModel::onEntryPick, modifier = Modifier.weight(1f))
                                    }
                                }
                                hints()
                                keyboard()
                            }
                        }
                    } else {
                        progress()
                        board(Modifier.weight(1f))
                        keyword()
                        when {
                            wheel != null -> wheelPad(Modifier.weight(0.9f))
                            play.puzzle.kind == PuzzleKind.FIT_IT -> FitBank(play, ui.fitOrder, ui.fitSelected, colors, viewModel::onFitSelect, Modifier.heightIn(max = 150.dp))
                            else -> clueBar()
                        }
                        hints()
                        keyboard()
                    }
                }
            }
        }

        ui.finished?.let { result ->
            WinOverlay(
                result = result,
                elapsed = ui.elapsed,
                play = ui.play,
                colors = colors,
                reduceMotion = settings.reduceMotion == true,
                onNext = onNext?.let { next -> { viewModel.saveNow(); next() } },
                onExit = leave,
            )
        }
        if (ui.timeAttackOver) {
            TimeAttackEnd(words = ui.timeAttack?.words ?: 0, colors = colors, onExit = leave)
        }
    }
}

@Composable
private fun Header(
    title: String,
    subtitle: String,
    time: Int,
    urgent: Boolean,
    colors: CruzadasColors,
    canPause: Boolean,
    showGridCheck: Boolean,
    balance: Int,
    onPause: () -> Unit,
    onExit: () -> Unit,
    onVerifyGrid: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = onExit) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Sair", tint = colors.text)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.text, fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 19.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) {
                Text(subtitle, color = colors.mute, fontFamily = Figtree, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Surface(shape = RoundedCornerShape(999.dp), color = colors.surface) {
            Text(
                text = Scoring.formatTime(time),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                color = if (urgent) colors.error else colors.accent,
                fontFamily = Bricolage,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
            )
        }
        if (canPause) {
            IconButton(onClick = onPause) { PauseGlyph(colors.text) }
        }
        if (showGridCheck) {
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Mais opções", tint = colors.text)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Verificar grelha · 1 ajuda", fontFamily = Figtree) },
                        enabled = balance >= 1,
                        onClick = {
                            menu = false
                            onVerifyGrid()
                        },
                    )
                }
            }
        }
    }
}

/** Drawn pause icon (two bars), without depending on the extended icons package. */
@Composable
private fun PauseGlyph(color: Color) {
    Canvas(modifier = Modifier.size(18.dp).semantics { contentDescription = "Pausa" }) {
        val bar = size.width * 0.3f
        drawRoundRect(color, topLeft = Offset(size.width * 0.12f, 0f), size = androidx.compose.ui.geometry.Size(bar, size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
        drawRoundRect(color, topLeft = Offset(size.width * 0.58f, 0f), size = androidx.compose.ui.geometry.Size(bar, size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
    }
}

@Composable
private fun ClueBar(play: PlayState, colors: CruzadasColors, onPrevious: () -> Unit, onNext: () -> Unit) {
    val index = InputLogic.activeEntry(play)
    val entry = index?.let { play.puzzle.entries[it] }
    var drag by remember { mutableStateOf(0f) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.accent.copy(alpha = 0.16f).compositeOver(colors.surface))
            // Swiping the bar sideways goes to the previous/next clue.
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { drag = 0f },
                    onHorizontalDrag = { _, amount -> drag += amount },
                    onDragEnd = {
                        if (drag < -SWIPE_PX) onNext() else if (drag > SWIPE_PX) onPrevious()
                        drag = 0f
                    },
                )
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(44.dp, 64.dp).clickable(onClick = onPrevious).semantics { contentDescription = "Pista anterior" },
            contentAlignment = Alignment.Center,
        ) { Text("‹", color = colors.accent, fontSize = 26.sp) }
        Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (entry == null) {
                Text("Toca numa casa", color = colors.mute, fontFamily = Figtree)
            } else {
                val dir = if (entry.dir == Dir.ACROSS) "H" else "V"
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${entry.number}$dir", color = colors.accent, fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("${entry.length} letras", color = colors.mute, fontFamily = Figtree, fontSize = 12.sp)
                    if (index in play.anagrams && index !in play.found) {
                        Text(
                            Checker.anagramOf(play, index).toList().joinToString(" "),
                            color = colors.text,
                            fontFamily = Bricolage,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.cell)
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                }
                AutoShrinkText(entry.clue, color = colors.text)
            }
        }
        Box(
            modifier = Modifier.size(44.dp, 64.dp).clickable(onClick = onNext).semantics { contentDescription = "Pista seguinte" },
            contentAlignment = Alignment.Center,
        ) { Text("›", color = colors.accent, fontSize = 26.sp) }
    }
}

/** Clue in up to 2 lines: shrinks the font before cutting the text. */
@Composable
private fun AutoShrinkText(text: String, color: Color) {
    var size by remember(text) { mutableStateOf(15f) }
    Text(
        text,
        color = color,
        fontFamily = Figtree,
        fontSize = size.sp,
        lineHeight = (size + 3).sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { result -> if (result.hasVisualOverflow && size > 12f) size -= 1f },
    )
}

private const val SWIPE_PX = 80f

@Composable
private fun HintRow(
    balance: Int,
    colors: CruzadasColors,
    onLetter: () -> Unit,
    onWord: () -> Unit,
    onAnagram: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HintChip("Letra · 1", balance >= 1, colors, onLetter, Modifier.weight(1f))
        HintChip("Anagrama · 1", balance >= 1, colors, onAnagram, Modifier.weight(1.25f))
        HintChip("Palavra · 3", balance >= 3, colors, onWord, Modifier.weight(1.1f))
        Row(
            modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "$balance ajudas" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            BulbIcon(color = colors.palette[3 % colors.palette.size], size = 16.dp)
            Text("$balance", color = colors.text, fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun HintChip(label: String, enabled: Boolean, colors: CruzadasColors, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surface)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (enabled) colors.text else colors.mute.copy(alpha = 0.7f),
            fontFamily = Figtree,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun PauseOverlay(colors: CruzadasColors, onResume: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surface)
            .clickable(onClick = onResume),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Pausa", color = colors.text, fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 26.sp)
            Text("Toca para continuar", color = colors.mute, fontFamily = Figtree)
        }
    }
}

/**
 * Level complete: on top of the game, the stars appear one by one, with the time, the
 * reasons for each lost star and the "Próximo" and "Voltar" buttons.
 */
@Composable
private fun WinOverlay(
    result: StarResult,
    elapsed: Int,
    play: PlayState?,
    colors: CruzadasColors,
    reduceMotion: Boolean,
    onNext: (() -> Unit)?,
    onExit: () -> Unit,
) {
    val shown = remember { Animatable(if (reduceMotion) 3f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduceMotion) {
            delay(250)
            shown.animateTo(3f, animationSpec = tween(durationMillis = 900, easing = LinearEasing))
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg.copy(alpha = 0.88f))
            .clickable(enabled = false, onClick = {})
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(colors.surface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Nível concluído", color = colors.text, fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(3) { i ->
                    val grow = (shown.value - i).coerceIn(0f, 1f)
                    val earned = i < result.stars
                    Text(
                        "★",
                        color = if (earned && grow > 0f) colors.accent else colors.cell,
                        fontSize = 44.sp,
                        modifier = Modifier.scale(if (earned) 0.6f + 0.4f * grow + 0.15f * sin(grow * PI.toFloat()) else 1f),
                    )
                }
            }
            play?.let {
                Text(
                    "${it.puzzle.entries.size} palavras em ${durationText(elapsed)}",
                    color = colors.mute,
                    fontFamily = Figtree,
                    fontSize = 15.sp,
                )
            }
            result.lostReasons.forEach { Text(it, color = colors.mute, fontFamily = Figtree, fontSize = 13.sp, textAlign = TextAlign.Center) }
            Spacer(Modifier.height(4.dp))
            if (onNext != null) {
                Button(
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
                ) { Text("Próximo", fontFamily = Figtree, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            }
            Button(
                onClick = onExit,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (onNext == null) colors.accent else colors.cell,
                    contentColor = if (onNext == null) colors.onAccent else colors.text,
                ),
            ) { Text("Voltar", fontFamily = Figtree, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
    }
}

/** "3m 32s", as in Sopa de Letras. */
private fun durationText(seconds: Int): String =
    if (seconds < 60) "${seconds}s" else "${seconds / 60}m ${"%02d".format(seconds % 60)}s"

/** Keyword strip: the letters already correct; tap it to guess the whole word. */
@Composable
private fun KeywordStrip(play: PlayState, draft: String, keywordMode: Boolean, colors: CruzadasColors, onToggle: () -> Unit) {
    val keyword = play.puzzle.keyword ?: return
    val slots = KeywordLogic.slots(play)
    val guessed = play.keywordGuessed
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (keywordMode) colors.accent.copy(alpha = 0.16f).compositeOver(colors.surface) else colors.surface)
            .clickable(enabled = !guessed, onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            when {
                guessed -> "Palavra-chave adivinhada"
                keywordMode -> "Escreve a palavra-chave e confirma com ✓"
                else -> "Palavra-chave · toca para adivinhar"
            },
            color = colors.mute,
            fontFamily = Figtree,
            fontSize = 12.sp,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            keyword.word.forEachIndexed { i, _ ->
                val letter: Char? = when {
                    guessed -> keyword.word[i]
                    slots[i] != null -> slots[i]
                    keywordMode && i < draft.length -> draft[i]
                    else -> null
                }
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (guessed) colors.accent else colors.cell),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        letter?.toString() ?: "",
                        color = if (guessed) colors.onAccent else colors.text,
                        fontFamily = Bricolage,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeAttackEnd(words: Int, colors: CruzadasColors, onExit: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg.copy(alpha = 0.88f))
            .clickable(enabled = false, onClick = {})
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(colors.surface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Tempo esgotado", color = colors.text, fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 26.sp)
            Text("$words palavras", color = colors.accent, fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 32.sp)
            Button(
                onClick = onExit,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
            ) { Text("Voltar", fontFamily = Figtree, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
    }
}

/** Physical keyboard: letters, backspace, tab, space to switch direction, and arrows. */
private fun onHardwareKey(event: KeyEvent, viewModel: GameViewModel): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    return when (event.key) {
        Key.Backspace -> { viewModel.onBackspace(); true }
        Key.Tab -> { viewModel.onNextEntry(if (event.isShiftPressed) -1 else 1); true }
        Key.Spacebar -> { viewModel.cursorPos()?.let { viewModel.onCellTap(it) }; true }
        Key.DirectionLeft -> { viewModel.onArrow(0, -1); true }
        Key.DirectionRight -> { viewModel.onArrow(0, 1); true }
        Key.DirectionUp -> { viewModel.onArrow(-1, 0); true }
        Key.DirectionDown -> { viewModel.onArrow(1, 0); true }
        else -> {
            val c = event.utf16CodePoint.toChar().uppercaseChar()
            if (c in Alphabet.CHARS) { viewModel.onLetter(c); true } else false
        }
    }
}
