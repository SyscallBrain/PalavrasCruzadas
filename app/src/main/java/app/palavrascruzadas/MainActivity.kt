package app.palavrascruzadas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import app.palavrascruzadas.core.books.Books
import app.palavrascruzadas.ui.newgame.THEMES
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import app.palavrascruzadas.core.campaign.WorldTable
import app.palavrascruzadas.data.AppClock
import app.palavrascruzadas.ui.books.BookScreen
import app.palavrascruzadas.ui.books.BooksScreen
import app.palavrascruzadas.ui.daily.DailyScreen
import app.palavrascruzadas.ui.stats.AchievementsScreen
import app.palavrascruzadas.ui.stats.StatsScreen
import app.palavrascruzadas.ui.game.GameScreen
import app.palavrascruzadas.ui.game.GameViewModel
import app.palavrascruzadas.ui.home.HomeScreen
import app.palavrascruzadas.ui.dev.DevScreen
import app.palavrascruzadas.ui.home.SimulatedDateDialog
import app.palavrascruzadas.ui.newgame.NewGameScreen
import android.content.Context
import android.provider.Settings as AndroidSettings
import androidx.compose.ui.platform.LocalContext
import app.palavrascruzadas.core.settings.AppSettings
import app.palavrascruzadas.ui.settings.CreditsScreen
import app.palavrascruzadas.ui.settings.SettingsScreen
import app.palavrascruzadas.ui.settings.SettingsViewModel
import app.palavrascruzadas.ui.theme.AllThemes
import app.palavrascruzadas.ui.theme.CruzadasTheme
import app.palavrascruzadas.ui.theme.LocalCruzadasColors
import app.palavrascruzadas.ui.theme.TokyoNight
import app.palavrascruzadas.ui.trail.ProgressViewModel
import app.palavrascruzadas.ui.trail.RuleCard
import app.palavrascruzadas.ui.trail.RuleCards
import app.palavrascruzadas.ui.trail.TrailScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val game: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        routeOf(intent)?.let { Navigation.request.value = it }
        setContent {
            val settingsVm: SettingsViewModel = viewModel()
            val settings by settingsVm.settings.collectAsState()
            val theme = AllThemes.firstOrNull { it.id == settings.themeId } ?: TokyoNight
            // System bar icons follow the app theme, not the phone's light/dark mode:
            // with a dark theme and the phone in light mode, the icons used to be invisible.
            LaunchedEffect(theme.isLight) {
                val transparent = android.graphics.Color.TRANSPARENT
                val style = if (theme.isLight) SystemBarStyle.light(transparent, transparent) else SystemBarStyle.dark(transparent)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            CruzadasTheme(colors = theme) {
                App(game, settingsVm, settings)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        routeOf(intent)?.let { Navigation.request.value = it }
    }

    companion object {
        const val EXTRA_ROUTE = "route"
        const val ROUTE_DAILY = "daily"

        private fun routeOf(intent: android.content.Intent?): String? = intent?.getStringExtra(EXTRA_ROUTE)
    }
}

/** Navigation requests coming from outside the app (notification and widget). */
object Navigation {
    val request = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
}

/** App screens. Simple state-based navigation. */
private sealed interface Screen {
    data object Home : Screen
    data object Trail : Screen
    data object Daily : Screen
    data object NewGame : Screen
    data object Settings : Screen
    data object Stats : Screen
    data object Achievements : Screen
    data object Credits : Screen
    data object Dev : Screen
    data object Books : Screen
    data class Book(val theme: String) : Screen
    data class Game(val target: Target) : Screen
}

/** What is being played. */
private sealed interface Target {
    data class Campaign(val number: Int) : Target
    data object Daily : Target
    data object Free : Target
    data object Mini : Target
    data class Book(val theme: String, val number: Int) : Target
    data object TimeAttack : Target
}

@Composable
private fun App(game: GameViewModel, settingsVm: SettingsViewModel, settings: AppSettings) {
    val progressVm: ProgressViewModel = viewModel()
    val data by progressVm.data.collectAsState()
    val statsData by progressVm.stats.collectAsState()
    val scope = rememberCoroutineScope()
    val today = AppClock.today(data.simulatedDay)
    // Saved across rotation. A game is only restored if the ViewModel still
    // has it; after the system kills the app, it goes back to the previous screen.
    var screen by rememberSaveable(stateSaver = screenSaver { game.ui.value.play != null }) { mutableStateOf<Screen>(Screen.Home) }
    /** Level waiting for the rule card of its type (shown once per type). */
    var pendingRule by remember { mutableStateOf<Pair<Int, RuleCards.Card>?>(null) }
    var showDateDialog by remember { mutableStateOf(false) }
    var backupStatus by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    // Reminder and widget: reschedule when the hour changes and at midnight; open the daily puzzle when asked from outside.
    androidx.compose.runtime.LaunchedEffect(settings.reminderHour) {
        settings.reminderHour?.let { app.palavrascruzadas.reminder.ReminderScheduler.schedule(context, it) }
    }
    androidx.compose.runtime.LaunchedEffect(Unit) { app.palavrascruzadas.widget.WidgetScheduler.schedule(context) }
    val routeRequest by Navigation.request.collectAsState()
    androidx.compose.runtime.LaunchedEffect(routeRequest) {
        if (routeRequest == MainActivity.ROUTE_DAILY) {
            screen = Screen.Daily
            Navigation.request.value = null
        }
    }
    var pendingImport by remember { mutableStateOf<Pair<app.palavrascruzadas.backup.BackupFile, app.palavrascruzadas.backup.BackupSummary>?>(null) }

    // System back button: goes back to the previous menu screen; only the home screen closes the app.
    BackHandler(enabled = pendingRule != null) { pendingRule = null }
    BackHandler(enabled = showDateDialog) { showDateDialog = false }
    BackHandler(enabled = screen != Screen.Home && pendingRule == null && !showDateDialog) {
        val current = screen
        if (current is Screen.Game) game.saveNow()
        screen = parentOf(current)
    }

    fun openLevel(number: Int) {
        val position = (number - 1) % WorldTable.LEVELS_PER_WORLD + 1
        val card = RuleCards.forType(WorldTable.typeAt(position))
        if (card != null && card.key !in data.seenTypes) {
            pendingRule = number to card
            return
        }
        game.startLevel(number)
        screen = Screen.Game(Target.Campaign(number))
    }

    val colors = LocalCruzadasColors.current
    val reduceMotion = settings.reduceMotion ?: systemReducesMotion(LocalContext.current)
    when (val current = screen) {
        Screen.Home -> HomeScreen(
            colors = colors,
            data = data,
            today = today,
            onContinue = { openLevel(data.progress.currentLevel) },
            onTrail = { screen = Screen.Trail },
            onDaily = { screen = Screen.Daily },
            onMini = {
                game.startMini()
                screen = Screen.Game(Target.Mini)
            },
            onFreeGame = { screen = Screen.NewGame },
            onBooks = { screen = Screen.Books },
            onTimeAttack = {
                game.startTimeAttack()
                screen = Screen.Game(Target.TimeAttack)
            },
            onSettings = { screen = Screen.Settings },
            onStats = { screen = Screen.Stats },
            onAchievements = { screen = Screen.Achievements },
            onSecret = if (BuildConfig.DEBUG) { { screen = Screen.Dev } } else null,
        )
        Screen.Trail -> TrailScreen(
            progress = data.progress,
            colors = colors,
            onOpenLevel = ::openLevel,
            onBack = { screen = Screen.Home },
            reduceMotion = reduceMotion,
        )
        Screen.Daily -> DailyScreen(
            colors = colors,
            daily = data.daily,
            today = today,
            onBack = { screen = Screen.Home },
            onPlay = {
                game.startDaily(today)
                screen = Screen.Game(Target.Daily)
            },
        )
        Screen.NewGame -> NewGameScreen(
            colors = colors,
            onBack = { screen = Screen.Home },
            onStart = { size, difficulty, theme ->
                game.startFreeform(size, difficulty, theme)
                screen = Screen.Game(Target.Free)
            },
        )
        Screen.Books -> BooksScreen(
            colors = colors,
            progress = data.books,
            onBack = { screen = Screen.Home },
            onOpenBook = { theme -> screen = Screen.Book(theme) },
        )
        is Screen.Book -> BookScreen(
            colors = colors,
            theme = current.theme,
            progress = data.books,
            onBack = { screen = Screen.Books },
            onOpenPuzzle = { number ->
                game.startBook(current.theme, number)
                screen = Screen.Game(Target.Book(current.theme, number))
            },
        )
        Screen.Settings -> SettingsScreen(
            colors = colors,
            settings = settings,
            onChange = settingsVm::update,
            onResetProgress = settingsVm::resetProgress,
            onCredits = { screen = Screen.Credits },
            onBack = { screen = Screen.Home },
            onExport = { uri -> settingsVm.exportTo(uri) { ok -> backupStatus = if (ok) "Progresso exportado." else "Não foi possível exportar." } },
            onPickBackup = { uri ->
                scope.launch {
                    settingsVm.readBackup(uri)
                        .onSuccess { (file, summary) -> pendingImport = file to summary }
                        .onFailure { backupStatus = it.message ?: "Ficheiro inválido." }
                }
            },
            status = backupStatus,
            onReminder = settingsVm::setReminder,
        )
        Screen.Dev -> DevScreen(
            colors = colors,
            currentLevel = data.progress.currentLevel,
            today = today,
            onBack = { screen = Screen.Home },
            onPlayLevel = { number ->
                // No rule card and no unlock check: this is for testing.
                game.startLevel(number)
                screen = Screen.Game(Target.Campaign(number))
            },
            onSimulatedDate = { showDateDialog = true },
        )
        Screen.Credits -> CreditsScreen(colors = colors, onBack = { screen = Screen.Settings })
        Screen.Stats -> StatsScreen(colors = colors, progress = data, stats = statsData, onBack = { screen = Screen.Home })
        Screen.Achievements -> AchievementsScreen(colors = colors, stats = statsData, onBack = { screen = Screen.Home })
        is Screen.Game -> GameScreen(
            viewModel = game,
            title = titleOf(current.target),
            subtitle = subtitleOf(current.target, today),
            onNext = when (val target = current.target) {
                is Target.Campaign -> if (target.number < WorldTable.worlds.size * WorldTable.LEVELS_PER_WORLD) {
                    { openLevel(target.number + 1) }
                } else null
                is Target.Book -> if (target.number < Books.puzzlesIn(target.theme)) {
                    {
                        game.startBook(target.theme, target.number + 1)
                        screen = Screen.Game(Target.Book(target.theme, target.number + 1))
                    }
                } else null
                Target.Mini -> { { game.startMini() } }
                else -> null
            },
            onExit = {
                screen = when (val target = current.target) {
                    is Target.Campaign -> Screen.Trail
                    Target.Daily -> Screen.Daily
                    is Target.Book -> Screen.Book(target.theme)
                    else -> Screen.Home
                }
            },
        )
    }

    pendingRule?.let { (number, card) ->
        RuleCard(
            title = card.title,
            pages = card.pages,
            colors = colors,
            onDone = {
                pendingRule = null
                scope.launch { progressVm.markSeen(card.key) }
                game.startLevel(number)
                screen = Screen.Game(Target.Campaign(number))
            },
        )
    }

    pendingImport?.let { (file, summary) ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { androidx.compose.material3.Text("Importar progresso?") },
            text = {
                androidx.compose.material3.Text(
                    "Cópia de ${summary.exported.take(10)}: ${summary.levels} níveis, ${summary.stars} estrelas, sequência máxima ${summary.streak} dias.\n\nIsto substitui o progresso atual.",
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    pendingImport = null
                    settingsVm.applyBackup(file) { backupStatus = "Progresso importado." }
                }) { androidx.compose.material3.Text("Importar") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pendingImport = null }) { androidx.compose.material3.Text("Cancelar") }
            },
        )
    }

    if (showDateDialog) {
        SimulatedDateDialog(
            today = today,
            simulated = data.simulatedDay != null,
            onChange = { day ->
                showDateDialog = false
                scope.launch { progressVm.setSimulatedDay(day?.toEpochDay()) }
            },
            onDismiss = { showDateDialog = false },
        )
    }
}

private fun titleOf(target: Target): String = when (target) {
    is Target.Campaign -> "Nível ${target.number}"
    Target.Daily -> "Desafio diário"
    Target.Free -> "Jogo livre"
    Target.Mini -> "Mini rápido"
    is Target.Book -> "Puzzle ${target.number}"
    Target.TimeAttack -> "Contra-relógio"
}

/** Line under the title ("Mundo 2 · À Mesa · 7×7"; the grid size is added by the screen). */
private fun subtitleOf(target: Target, today: java.time.LocalDate): String = when (target) {
    is Target.Campaign -> {
        val world = WorldTable.worlds[(target.number - 1) / WorldTable.LEVELS_PER_WORLD]
        "Mundo ${world.number} · ${world.name}"
    }
    Target.Daily -> today.format(java.time.format.DateTimeFormatter.ofPattern("d 'de' MMMM", java.util.Locale.forLanguageTag("pt-PT")))
    is Target.Book -> "Caderno " + (THEMES.firstOrNull { it.first == target.theme }?.second ?: bookName(target.theme))
    else -> ""
}

private fun bookName(theme: String): String = when (theme) {
    "roda" -> "Roda de letras"
    "diretas" -> "Diretas"
    "encaixar" -> "Encaixar palavras"
    else -> theme
}

/** Current screen as text, to survive rotation and process recreation. */
private fun screenSaver(gameAlive: () -> Boolean): Saver<Screen, String> = Saver(
    save = { encode(it) },
    restore = { text ->
        val screen = decode(text)
        if (screen is Screen.Game && !gameAlive()) parentOf(screen) else screen
    },
)

private fun encode(screen: Screen): String = when (screen) {
    Screen.Home -> "home"
    Screen.Trail -> "trail"
    Screen.Daily -> "daily"
    Screen.NewGame -> "newgame"
    Screen.Settings -> "settings"
    Screen.Stats -> "stats"
    Screen.Achievements -> "achievements"
    Screen.Credits -> "credits"
    Screen.Dev -> "dev"
    Screen.Books -> "books"
    is Screen.Book -> "book|${screen.theme}"
    is Screen.Game -> "game|" + when (val t = screen.target) {
        is Target.Campaign -> "campaign|${t.number}"
        Target.Daily -> "daily"
        Target.Free -> "free"
        Target.Mini -> "mini"
        is Target.Book -> "book|${t.theme}|${t.number}"
        Target.TimeAttack -> "timeattack"
    }
}

private fun decode(text: String): Screen {
    val parts = text.split("|")
    return when (parts[0]) {
        "trail" -> Screen.Trail
        "daily" -> Screen.Daily
        "newgame" -> Screen.NewGame
        "settings" -> Screen.Settings
        "stats" -> Screen.Stats
        "achievements" -> Screen.Achievements
        "credits" -> Screen.Credits
        "dev" -> Screen.Dev
        "books" -> Screen.Books
        "book" -> Screen.Book(parts[1])
        "game" -> when (parts.getOrNull(1)) {
            "campaign" -> Screen.Game(Target.Campaign(parts[2].toInt()))
            "daily" -> Screen.Game(Target.Daily)
            "free" -> Screen.Game(Target.Free)
            "mini" -> Screen.Game(Target.Mini)
            "book" -> Screen.Game(Target.Book(parts[2], parts[3].toInt()))
            "timeattack" -> Screen.Game(Target.TimeAttack)
            else -> Screen.Home
        }
        else -> Screen.Home
    }
}

/** System "remove animations": the animator duration scale is zero. */
private fun systemReducesMotion(context: Context): Boolean =
    runCatching { AndroidSettings.Global.getFloat(context.contentResolver, AndroidSettings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
        .getOrDefault(false)

/** Screen the system back button leads to from [screen] (a menu, never an exit). */
private fun parentOf(screen: Screen): Screen = when (screen) {
    Screen.Home -> Screen.Home
    Screen.Trail, Screen.Daily, Screen.NewGame, Screen.Books, Screen.Settings, Screen.Stats, Screen.Achievements, Screen.Dev -> Screen.Home
    is Screen.Book -> Screen.Books
    Screen.Credits -> Screen.Settings
    is Screen.Game -> when (val target = screen.target) {
        is Target.Campaign -> Screen.Trail
        Target.Daily -> Screen.Daily
        is Target.Book -> Screen.Book(target.theme)
        else -> Screen.Home
    }
}
