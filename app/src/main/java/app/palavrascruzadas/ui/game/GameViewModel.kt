package app.palavrascruzadas.ui.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.palavrascruzadas.core.campaign.LevelSpec
import app.palavrascruzadas.core.model.Dir
import app.palavrascruzadas.core.model.PuzzleKind
import app.palavrascruzadas.core.play.HintReward
import app.palavrascruzadas.core.play.KeywordLogic
import app.palavrascruzadas.core.play.WheelLogic
import app.palavrascruzadas.core.play.TimeAttack
import app.palavrascruzadas.core.daily.DailyPuzzle
import app.palavrascruzadas.core.daily.MiniPuzzle
import app.palavrascruzadas.core.gen.FreeformGenerator
import app.palavrascruzadas.core.gen.GenSpec
import app.palavrascruzadas.core.gen.GenerationException
import app.palavrascruzadas.core.model.Difficulty
import app.palavrascruzadas.core.model.Pos
import app.palavrascruzadas.core.model.Puzzle
import app.palavrascruzadas.core.play.Checker
import app.palavrascruzadas.core.play.HintCost
import app.palavrascruzadas.core.play.HintWallet
import app.palavrascruzadas.core.play.InputLogic
import app.palavrascruzadas.core.play.PlayOptions
import app.palavrascruzadas.core.play.PlayState
import app.palavrascruzadas.core.play.SavedGame
import app.palavrascruzadas.core.play.Scoring
import app.palavrascruzadas.core.play.StarResult
import app.palavrascruzadas.data.BankRepository
import app.palavrascruzadas.data.BookRepository
import app.palavrascruzadas.data.CampaignRepository
import app.palavrascruzadas.data.DailyRepository
import app.palavrascruzadas.data.ProgressRepository
import androidx.glance.appwidget.updateAll
import app.palavrascruzadas.data.SavedGamesRepository
import app.palavrascruzadas.data.SettingsRepository
import app.palavrascruzadas.data.StatsRepository
import app.palavrascruzadas.core.achievements.Achievement
import app.palavrascruzadas.core.campaign.WorldTable
import app.palavrascruzadas.core.books.Books
import app.palavrascruzadas.feedback.Feedback
import app.palavrascruzadas.core.settings.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Visible state of the game screen. */
data class GameUi(
    val loading: Boolean = false,
    val error: String? = null,
    val play: PlayState? = null,
    val elapsed: Int = 0,
    val paused: Boolean = false,
    val finished: StarResult? = null,
    /** Wrong cells from the last "check word", shown in red for a few seconds. */
    val flashWrong: Set<Pos> = emptySet(),
    val wallet: HintWallet = HintWallet(),
    val message: String? = null,
    /** Global campaign level number, or null. */
    val levelNumber: Int? = null,
    /** Position 1–40 of the level in its world (for the boss rewards). */
    val levelPosition: Int? = null,
    /** Day of the daily challenge, or null. */
    val dailyDate: LocalDate? = null,
    /** Quick mini (5×5). */
    val mini: Boolean = false,
    /** Grid size in free play, or null. */
    val freeSize: Int? = null,
    /** Themed book and puzzle number (1–25). */
    val bookTheme: String? = null,
    val bookNumber: Int? = null,
    /** Time attack in progress, or null. */
    val timeAttack: TimeAttack? = null,
    val timeAttackOver: Boolean = false,
    /** Words of the current grid already counted for the time attack. */
    val taFound: Int = 0,
    /** Keyword guessing mode: the keys type into the draft. */
    val keywordMode: Boolean = false,
    val keywordDraft: String = "",
    /** While a new time-attack grid is being generated, the game ignores typing. */
    val busy: Boolean = false,
    /** Letter wheel: base word, letters in wheel order and bonus words. */
    val wheelBase: String? = null,
    val wheelOrder: List<Char> = emptyList(),
    val wheelBonus: List<String> = emptyList(),
    val bonusFound: Set<String> = emptySet(),
    /** Fit-it: words in the box, in shuffled order, and the one picked for placing. */
    val fitOrder: List<String> = emptyList(),
    val fitSelected: String? = null,
)

/**
 * Game state and actions. The logic lives in :core (play); this only wires the actions,
 * the timer, background generation and saving.
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val _ui = MutableStateFlow(GameUi())
    val ui: StateFlow<GameUi> = _ui.asStateFlow()

    private val progress = ProgressRepository.from(application)
    private val savedGames = SavedGamesRepository.from(application)
    private val settingsRepo = SettingsRepository.from(application)
    private val stats = StatsRepository.from(application)
    private val feedback = Feedback(application)

    /** Current settings, for the UI (theme, grid letter size, colour-blind mode). */
    val settings: StateFlow<AppSettings> = settingsRepo.data.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    /** Settings used by the game being started; read at the start of each puzzle. */
    private var current = AppSettings()

    private var timer: Job? = null
    private var flash: Job? = null
    private var save: Job? = null
    private var timeAttackRecorded = false

    /** Free play: grid generated on the phone, with an optional theme. */
    fun startFreeform(size: Int = 7, difficulty: Difficulty = Difficulty.MEDIUM, theme: String? = null) {
        viewModelScope.launch {
            current = settingsRepo.current()
            _ui.value = GameUi(loading = true)
            val bank = BankRepository.load(getApplication())
            val generator = FreeformGenerator(bank)
            val puzzle = withContext(Dispatchers.Default) { generateOrNull(generator, size, difficulty, theme) }
            if (puzzle == null) {
                _ui.value = GameUi(error = "Não foi possível gerar uma grelha com estas opções. Tenta outro tema ou tamanho.")
                return@launch
            }
            _ui.value = startingUi(puzzle, saved = null, wallet = progress.data.first().wallet, mode = "livre").copy(freeSize = size)
            startTimer()
        }
    }

    /** Quick mini: 5×5, generated with a new seed. */
    fun startMini() {
        viewModelScope.launch {
            current = settingsRepo.current()
            _ui.value = GameUi(loading = true)
            val bank = BankRepository.load(getApplication())
            val seed = Random.nextInt()
            val puzzle = withContext(Dispatchers.Default) { MiniPuzzle.generate(bank, seed) }
            _ui.value = startingUi(puzzle, saved = null, wallet = progress.data.first().wallet, mode = "mini").copy(mini = true)
            startTimer()
        }
    }

    /** Daily challenge: pre-generated for the first 730 days, generated on the phone afterwards. */
    fun startDaily(date: LocalDate) {
        viewModelScope.launch {
            current = settingsRepo.current()
            _ui.value = GameUi(loading = true)
            val app = getApplication<Application>()
            val puzzle = if (DailyPuzzle.preGeneratedIndex(date) != null) {
                DailyRepository.puzzles(app)[DailyPuzzle.preGeneratedIndex(date)!!]
            } else {
                withContext(Dispatchers.Default) { DailyPuzzle.generate(BankRepository.load(app), date) }
            }
            val saved = savedGames.get(puzzle.id)
            _ui.value = startingUi(puzzle, saved, progress.data.first().wallet, mode = "diario").copy(dailyDate = date)
            startTimer()
        }
    }

    /** Puzzle of a book (number 1–25). Resumes the saved game, if any. */
    fun startBook(theme: String, number: Int) {
        viewModelScope.launch {
            current = settingsRepo.current()
            _ui.value = GameUi(loading = true)
            val spec = BookRepository.puzzles(getApplication(), theme)[number - 1]
            val saved = savedGames.get(spec.puzzle.id)
            val mode = if (theme in Books.SPECIAL) theme else "caderno"
            _ui.value = startingUi(spec.puzzle, saved, progress.data.first().wallet, spec.base, spec.bonus, mode = mode).copy(bookTheme = theme, bookNumber = number)
            startTimer()
        }
    }

    /** Time attack: 3 minutes, 5×5 minis in sequence, no hints. */
    fun startTimeAttack() {
        timeAttackRecorded = false
        viewModelScope.launch {
            current = settingsRepo.current()
            stats.recordMode("contra-relogio", current.themeId)
            _ui.value = GameUi(loading = true, timeAttack = TimeAttack(), busy = true)
            loadNextMini()
            startTimer()
        }
    }

    private suspend fun loadNextMini() {
        val bank = BankRepository.load(getApplication())
        val puzzle = withContext(Dispatchers.Default) { MiniPuzzle.generate(bank, Random.nextInt()) }
        _ui.update {
            it.copy(
                loading = false,
                busy = false,
                play = PlayState.start(puzzle, current.playOptions()),
                taFound = 0,
            )
        }
    }

    /**
     * Campaign level: resumes this level's saved game, if any, or starts from scratch.
     * The timer continues from where it stopped.
     */
    fun startLevel(number: Int) {
        viewModelScope.launch {
            current = settingsRepo.current()
            _ui.value = GameUi(loading = true)
            val spec: LevelSpec = CampaignRepository.level(getApplication(), number)
            val saved = savedGames.get(spec.puzzle.id)
            _ui.value = startingUi(spec.puzzle, saved, progress.data.first().wallet, spec.base, spec.bonus, mode = "campanha").copy(
                levelNumber = number,
                levelPosition = spec.position,
            )
            startTimer()
        }
    }

    /** Initial state of a puzzle: resumes the saved game, or puts the cursor on the first entry. */
    private fun startingUi(
        puzzle: Puzzle,
        saved: SavedGame?,
        wallet: HintWallet,
        base: String? = null,
        bonus: List<String> = emptyList(),
        mode: String = "livre",
    ): GameUi {
        recordStart(puzzle, mode)
        // PlayState.start reveals the fit-it given cells; they used to start empty.
        val play = saved?.restore(puzzle, current.playOptions())?.let(Checker::markFound)
            ?: PlayState.start(puzzle, current.playOptions())
        return GameUi(
            play = play,
            elapsed = saved?.elapsedSeconds ?: 0,
            wallet = wallet,
            wheelBase = base,
            wheelOrder = base?.toList()?.shuffled().orEmpty(),
            wheelBonus = bonus,
            fitOrder = if (puzzle.kind == PuzzleKind.FIT_IT) puzzle.entries.map { it.answer }.shuffled() else emptyList(),
        )
    }

    /** Records the mode and visual theme of this game (for the "one of each" and "all colours" achievements). */
    private fun recordStart(puzzle: Puzzle, mode: String) {
        val modes = listOfNotNull(
            mode,
            when (puzzle.kind) {
                PuzzleKind.WHEEL -> "roda"
                PuzzleKind.DIRETAS -> "diretas"
                PuzzleKind.FIT_IT -> "encaixar"
                PuzzleKind.KEYWORD -> "palavra-chave"
                else -> null
            },
        )
        viewModelScope.launch { modes.forEach { stats.recordMode(it, current.themeId) } }
    }

    /** Saves the game now (when leaving the screen). No effect on games that are not saved. */
    fun saveNow() {
        viewModelScope.launch { persist() }
    }

    /**
     * The app went to the background: save and pause. Without this, the timer kept
     * running while the app was closed and the player lost the time star.
     */
    fun onAppStop() {
        saveNow()
        _ui.update { if (it.play != null && it.finished == null && it.timeAttack == null) it.copy(paused = true) else it }
    }

    /** Only the campaign, the daily puzzle and the books save games in progress. */
    private suspend fun persist() {
        val ui = _ui.value
        val play = ui.play ?: return
        if (ui.finished != null) return
        val keep = ui.levelNumber != null || ui.dailyDate != null || ui.bookTheme != null
        if (!keep) return
        savedGames.put(SavedGame.capture(play, ui.levelNumber, ui.elapsed, System.currentTimeMillis()))
    }

    private fun scheduleSave() {
        save?.cancel()
        save = viewModelScope.launch {
            delay(SAVE_DELAY_MILLIS)
            persist()
        }
    }

    private fun generateOrNull(generator: FreeformGenerator, size: Int, difficulty: Difficulty, theme: String?): Puzzle? {
        repeat(MAX_SEED_TRIES) {
            val spec = GenSpec(
                maxW = size,
                maxH = size,
                minWords = size - 2,
                maxWords = size + 1,
                difficulties = setOf(difficulty),
                themes = theme?.let { setOf(it) },
                rngSeed = Random.nextInt(),
            )
            try {
                return generator.generate(spec)
            } catch (e: GenerationException) {
                // This seed found no grid: try another one.
            }
        }
        return null
    }

    /**
     * Tap on a cell. In diretas, tapping a clue cell opens the arrow's entry.
     * In fit-it, with a word picked from the box, tries to place it on this cell.
     */
    fun onCellTap(pos: Pos) {
        val ui = _ui.value
        val play = ui.play ?: return
        val clue = play.puzzle.clueCells.firstOrNull { it.pos == pos }
        if (clue != null) {
            // Clue cell with two arrows: the first tap opens the first one, the next tap switches.
            val arrows = clue.arrows.sortedBy { it.ordinal }
            val active = InputLogic.activeEntry(play)?.let { play.puzzle.entries[it] }
            val startOf = { d: Dir -> if (d == Dir.ACROSS) Pos(pos.row, pos.col + 1) else Pos(pos.row + 1, pos.col) }
            val activeIsFirst = active != null && active.dir == arrows.first() &&
                Pos(active.row, active.col) == startOf(arrows.first())
            val dir = if (activeIsFirst && arrows.size > 1) arrows[1] else arrows.first()
            val start = if (dir == Dir.ACROSS) Pos(pos.row, pos.col + 1) else Pos(pos.row + 1, pos.col)
            val index = play.puzzle.entries.indexOfFirst { it.dir == dir && it.row == start.row && it.col == start.col }
            if (index >= 0) _ui.update { it.copy(play = InputLogic.focusEntry(play, index)) }
            return
        }
        if (ui.fitSelected != null) return fitPlace(pos)
        edit { InputLogic.select(it, pos) }
    }

    /** Fit-it box: picks the word to place; tapping it again clears the choice. */
    fun onFitSelect(word: String) {
        val play = _ui.value.play ?: return
        // It can only be picked while a copy of this word is still unplaced.
        if (play.puzzle.entries.withIndex().none { (i, e) -> e.answer == word && i !in play.found }) return
        _ui.update { it.copy(fitSelected = if (it.fitSelected == word) null else word, message = null) }
    }

    /** Places the chosen word on the entry through this cell if it is the right one; otherwise shows a message. */
    private fun fitPlace(pos: Pos) {
        val ui = _ui.value
        val play = ui.play ?: return
        val word = ui.fitSelected ?: return
        val hit = play.puzzle.entries.withIndex().firstOrNull { (i, e) ->
            i !in play.found && e.answer == word && pos in e.cells
        }
        if (hit == null) {
            _ui.update { it.copy(message = "Não encaixa aqui.") }
            return
        }
        val cells = hit.value.cells
        val next = play.copy(
            typed = play.typed + cells.mapIndexed { k, p -> p to word[k] },
            locked = play.locked + cells.toSet(),
            found = play.found + hit.index,
        )
        _ui.update { it.copy(fitSelected = null, message = null) }
        setPlay(next)
    }

    /** Letter wheel: the word formed with the wheel letters. Every 5 bonus words give +1 hint. */
    fun onWheelWord(word: String) {
        val ui = _ui.value
        val play = ui.play ?: return
        if (ui.paused || ui.finished != null || ui.busy || ui.timeAttackOver || ui.wheelBase == null) return
        val result = WheelLogic.submit(play, word, ui.wheelBonus.toSet(), ui.bonusFound)
        when (result.outcome) {
            // A wheel word can complete other entries through the crossings.
            WheelLogic.Outcome.GRID -> setPlay(Checker.markFound(result.state))
            WheelLogic.Outcome.BONUS -> {
                feedback.play(Feedback.Sound.BONUS, current)
                viewModelScope.launch { stats.recordBonusWord() }
                val earned = result.bonusFound.size % WHEEL_BONUS_EVERY == 0
                val wallet = if (earned) ui.wallet.earn(HintReward.BONUS_WORDS_WHEEL) else ui.wallet
                _ui.update {
                    it.copy(
                        bonusFound = result.bonusFound,
                        wallet = wallet,
                        message = if (earned) "5 palavras bónus: +1 ajuda" else "Palavra bónus!",
                    )
                }
                if (earned) viewModelScope.launch { progress.saveWallet(wallet) }
            }
            WheelLogic.Outcome.ALREADY -> _ui.update { it.copy(message = "Essa já a tens.") }
            WheelLogic.Outcome.NONE -> _ui.update { it.copy(message = "Não é palavra desta roda.") }
        }
    }

    fun onWheelShuffle() {
        _ui.update { it.copy(wheelOrder = it.wheelOrder.shuffled()) }
    }

    /** Wheel hint: reveals a letter of the first unsolved entry (1 hint). */
    fun onWheelHint() = spend(HintCost.REVEAL_LETTER) { play ->
        val index = play.puzzle.entries.indices.firstOrNull { it !in play.found } ?: return@spend play
        Checker.revealLetter(InputLogic.focusEntry(play, index))
    }

    /** Tap on the keyword strip: toggles the guessing mode. */
    fun onKeywordToggle() {
        val ui = _ui.value
        val play = ui.play ?: return
        if (play.puzzle.keyword == null || play.keywordGuessed || ui.timeAttack != null) return
        _ui.update { it.copy(keywordMode = !it.keywordMode, keywordDraft = "", message = null) }
    }

    fun onLetter(letter: Char) {
        feedback.play(Feedback.Sound.KEY, current)
        feedback.buzz(Feedback.Buzz.KEY, current)
        val ui = _ui.value
        if (ui.keywordMode) {
            val word = ui.play?.puzzle?.keyword?.word ?: return
            if (ui.keywordDraft.length < word.length) _ui.update { it.copy(keywordDraft = it.keywordDraft + letter) }
            return
        }
        edit { Checker.markFound(InputLogic.type(it, letter)) }
    }

    fun onBackspace() {
        val ui = _ui.value
        if (ui.keywordMode) {
            _ui.update { it.copy(keywordDraft = it.keywordDraft.dropLast(1)) }
            return
        }
        edit { InputLogic.backspace(it) }
    }

    /** ✓ in guessing mode confirms the keyword; otherwise it checks the word. */
    private fun guessKeyword() {
        val ui = _ui.value
        val play = ui.play ?: return
        val (next, right) = KeywordLogic.guess(play, ui.keywordDraft)
        if (!right) {
            _ui.update { it.copy(keywordDraft = "", message = "Ainda não é essa.") }
            return
        }
        val known = KeywordLogic.slots(play).count { it != null }
        val half = known * 2 >= play.puzzle.keyword!!.word.length
        viewModelScope.launch { stats.recordKeyword(half) }
        _ui.update { it.copy(keywordMode = false, keywordDraft = "", message = "Palavra-chave certa! +1 ajuda") }
        feedback.play(Feedback.Sound.BONUS, current)
        viewModelScope.launch {
            val wallet = progress.rewardKeyword(play.puzzle.id, _ui.value.wallet)
            _ui.update { it.copy(wallet = wallet) }
        }
        setPlay(next)
    }

    fun onNextEntry(step: Int) = edit { InputLogic.nextEntry(it, step) }

    /** Cell under the cursor, or null. */
    fun cursorPos(): Pos? = _ui.value.play?.cursor?.pos

    /** Physical keyboard arrows: move the cursor one cell, if that cell belongs to the grid. */
    fun onArrow(dRow: Int, dCol: Int) = edit { play ->
        val cursor = play.cursor ?: return@edit play
        val next = Pos(cursor.pos.row + dRow, cursor.pos.col + dCol)
        if (next in play.solution) InputLogic.select(play, next) else play
    }

    /** Tap on a clue (two-column layout): selects that entry. */
    fun onEntryPick(index: Int) = edit { InputLogic.focusEntry(it, index) }

    fun onRevealLetter() = spend(HintCost.REVEAL_LETTER) { Checker.revealLetter(it) }

    fun onRevealWord() = spend(HintCost.REVEAL_WORD) { Checker.revealWord(it) }

    fun onAnagram() = spend(HintCost.ANAGRAM) { Checker.showAnagram(it) }

    /** Check grid: costs 1 hint and shows the wrong cells. */
    fun onVerifyGrid() {
        val play = _ui.value.play ?: return
        if (_ui.value.paused || _ui.value.finished != null || _ui.value.timeAttack != null) return
        if (!_ui.value.wallet.canAfford(HintCost.VERIFY_GRID)) return notEnoughHints()
        val (next, wrong) = Checker.verifyGrid(play)
        val wallet = _ui.value.wallet.spend(HintCost.VERIFY_GRID) ?: _ui.value.wallet
        _ui.update { it.copy(wallet = wallet) }
        viewModelScope.launch { progress.saveWallet(wallet) }
        setPlay(next)
        flashWrong(wrong)
        if (wrong.isNotEmpty()) {
            feedback.play(Feedback.Sound.ERROR, current)
            feedback.buzz(Feedback.Buzz.REJECT, current)
        }
    }

    /** Check word (free): shows the wrong letters of the active entry. */
    fun onVerifyWord() {
        val ui = _ui.value
        if (ui.keywordMode) return guessKeyword()
        val play = ui.play ?: return
        if (ui.timeAttack != null) return
        val (next, wrong) = Checker.verifyWord(play)
        setPlay(next)
        flashWrong(wrong)
        if (wrong.isNotEmpty()) {
            feedback.play(Feedback.Sound.ERROR, current)
            feedback.buzz(Feedback.Buzz.REJECT, current)
        }
    }

    fun onPause() {
        _ui.update { if (it.play != null && it.finished == null) it.copy(paused = true) else it }
    }

    fun onResume() {
        _ui.update { it.copy(paused = false) }
    }

    fun onMessageShown() {
        _ui.update { it.copy(message = null) }
    }

    /** Text actions of the game: apply the change and handle the end of the puzzle. */
    private fun edit(transform: (PlayState) -> PlayState) {
        val ui = _ui.value
        val play = ui.play ?: return
        if (ui.paused || ui.finished != null || ui.busy || ui.timeAttackOver) return
        setPlay(transform(play))
    }

    private fun spend(cost: HintCost, transform: (PlayState) -> PlayState) {
        val ui = _ui.value
        val play = ui.play ?: return
        if (ui.paused || ui.finished != null) return
        if (ui.timeAttack != null) return notEnoughHints()
        if (!ui.wallet.canAfford(cost)) return notEnoughHints()
        // Only pay if the hint changes something (cell already right, word solved or anagram already shown).
        val next = transform(play)
        if (next == play) {
            _ui.update { it.copy(message = "Aqui não há nada a revelar.") }
            return
        }
        val wallet = ui.wallet.spend(cost) ?: return notEnoughHints()
        _ui.update { it.copy(wallet = wallet) }
        viewModelScope.launch { progress.saveWallet(wallet) }
        feedback.play(Feedback.Sound.HINT, current)
        setPlay(next)
    }

    private fun setPlay(play: PlayState) {
        val previousFound = _ui.value.play?.found?.size ?: 0
        // Without "confirm correct words", the sound must not give away that a word is right.
        if (play.found.size > previousFound && !play.isComplete && play.options.confirmWords) {
            feedback.play(Feedback.Sound.FOUND, current)
            feedback.buzz(Feedback.Buzz.CONFIRM, current)
        }
        if (play.isComplete && _ui.value.timeAttack == null) {
            feedback.play(Feedback.Sound.WIN, current)
            feedback.buzz(Feedback.Buzz.CONFIRM, current)
        }
        val before = _ui.value
        val timeAttack = before.timeAttack
        if (timeAttack != null) {
            setTimeAttackPlay(before, play)
            return
        }
        _ui.update { current ->
            val next = current.copy(play = play)
            if (play.isComplete) {
                timer?.cancel()
                next.copy(finished = Scoring.stars(next.elapsed, play.puzzle.targetSeconds, play))
            } else {
                next
            }
        }
        val ui = _ui.value
        if (ui.finished != null) {
            completeGame(ui)
        } else {
            scheduleSave()
        }
    }

    /** Time attack: counts the new words; a completed grid adds time and loads the next one. */
    private fun setTimeAttackPlay(before: GameUi, play: PlayState) {
        val gridDone = play.isComplete
        val found = play.found.size - before.taFound
        if (found > 0) viewModelScope.launch { stats.recordWords(found) }
        _ui.update {
            it.copy(
                play = play,
                taFound = play.found.size,
                timeAttack = it.timeAttack?.wordsFound(found)?.let { t -> if (gridDone) t.gridDone() else t },
                busy = gridDone,
            )
        }
        if (gridDone) viewModelScope.launch { loadNextMini() }
    }

    /**
     * End of the puzzle: campaign and daily record stars and rewards and delete the saved game;
     * the mini keeps the best time; free play records nothing.
     */
    private fun completeGame(ui: GameUi) {
        val play = ui.play ?: return
        val stars = ui.finished?.stars ?: return
        viewModelScope.launch {
            when {
                ui.levelNumber != null && ui.levelPosition != null -> {
                    val wallet = progress.recordWin(ui.levelNumber, ui.levelPosition, stars, ui.elapsed, ui.wallet)
                    savedGames.remove(play.puzzle.id)
                    _ui.update { it.copy(wallet = wallet) }
                }
                ui.bookTheme != null && ui.bookNumber != null -> {
                    progress.recordBook(ui.bookTheme, ui.bookNumber, stars)
                    savedGames.remove(play.puzzle.id)
                }
                ui.dailyDate != null -> {
                    val wallet = progress.recordDaily(ui.dailyDate, stars, ui.wallet)
                    app.palavrascruzadas.widget.DailyWidget().updateAll(getApplication())
                    savedGames.remove(play.puzzle.id)
                    _ui.update { it.copy(wallet = wallet) }
                }
                ui.mini -> progress.recordMini(ui.elapsed)
            }
            val boss = ui.levelPosition == WorldTable.LEVELS_PER_WORLD && ui.levelNumber != null
            if (boss && play.hintPoints == 0) stats.recordBossWithoutHints()
            if (ui.wheelBase != null && ui.wheelBonus.isNotEmpty() && ui.bonusFound.size == ui.wheelBonus.size) stats.recordWheelFull()
            stats.recordPuzzle(
                seconds = ui.elapsed,
                letters = play.puzzle.solution.size,
                words = play.puzzle.entries.size,
                freeSize = ui.freeSize,
            )
            val fresh = stats.unlockNew(progress.data.first())
            if (fresh.isNotEmpty()) {
                _ui.update { it.copy(message = "Conquista: " + fresh.joinToString(" · ") { a -> a.title }) }
            }
        }
    }

    private fun flashWrong(cells: List<Pos>) {
        flash?.cancel()
        _ui.update { it.copy(flashWrong = cells.toSet()) }
        if (cells.isEmpty()) return
        flash = viewModelScope.launch {
            delay(FLASH_MILLIS)
            _ui.update { it.copy(flashWrong = emptySet()) }
        }
    }

    private fun notEnoughHints() {
        _ui.update { it.copy(message = "Sem ajudas. Ganha-as com 3 estrelas e no desafio diário.") }
    }

    private fun startTimer() {
        timer?.cancel()
        timer = viewModelScope.launch {
            while (true) {
                delay(1_000)
                _ui.update { ui ->
                    if (ui.play == null || ui.paused || ui.finished != null || ui.timeAttackOver) return@update ui
                    val ta = ui.timeAttack ?: return@update ui.copy(elapsed = ui.elapsed + 1)
                    val next = ta.tick()
                    ui.copy(timeAttack = next, timeAttackOver = next.isOver, elapsed = ui.elapsed + 1)
                }
                val ui = _ui.value
                if (ui.timeAttackOver && !timeAttackRecorded) {
                    timeAttackRecorded = true
                    progress.recordTimeAttack(ui.timeAttack?.words ?: 0)
                }
            }
        }
    }

    override fun onCleared() {
        timer?.cancel()
        flash?.cancel()
        feedback.release()
        super.onCleared()
    }

    private companion object {
        const val MAX_SEED_TRIES = 10
        const val FLASH_MILLIS = 2_000L
        const val SAVE_DELAY_MILLIS = 500L
        const val WHEEL_BONUS_EVERY = 5
    }
}
