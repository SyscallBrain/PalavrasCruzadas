package app.palavrascruzadas.core.play

import app.palavrascruzadas.core.model.Dir
import app.palavrascruzadas.core.model.Pos
import app.palavrascruzadas.core.model.Puzzle

/** Where the cursor is: a cell and the direction of the active entry. */
data class Cursor(val pos: Pos, val dir: Dir)

/**
 * State of a game in progress. Immutable: each action returns a new state.
 * The counters feed the stars and the hint economy.
 */
data class PlayState(
    val puzzle: Puzzle,
    val typed: Map<Pos, Char> = emptyMap(),
    /** Cells revealed by a hint: they count as correct and cannot be erased. */
    val revealed: Set<Pos> = emptySet(),
    /** Cells of complete, correct words: locked. */
    val locked: Set<Pos> = emptySet(),
    /** Indices of the entries already complete and correct. */
    val found: Set<Int> = emptySet(),
    val cursor: Cursor? = null,
    val letterHints: Int = 0,
    val anagramHints: Int = 0,
    val wordReveals: Int = 0,
    val gridChecks: Int = 0,
    val wordErrorChecks: Int = 0,
    /** The keyword was guessed (whole guess right). */
    val keywordGuessed: Boolean = false,
    /** Entries whose answer anagram is shown in the clue bar. */
    val anagrams: Set<Int> = emptySet(),
    val options: PlayOptions = PlayOptions(),
) {
    val solution: Map<Pos, Char> get() = puzzle.solution

    /** Hint points used for the stars. */
    val hintPoints: Int
        get() = letterHints + anagramHints + 2 * wordReveals + gridChecks + wordErrorChecks

    val isComplete: Boolean get() = puzzle.solution.keys.all { isCorrect(it) }

    fun isCorrect(pos: Pos): Boolean = pos in revealed || typed[pos] == solution[pos]

    companion object {
        /**
         * Initial state of a puzzle: the given cells (fit-it anchor) are revealed,
         * the words they complete are confirmed and the cursor goes to the first unsolved entry.
         */
        fun start(puzzle: Puzzle, options: PlayOptions = PlayOptions()): PlayState {
            val base = Checker.markFound(PlayState(puzzle = puzzle, options = options, revealed = puzzle.givens))
            val first = InputLogic.orderedEntries(base).firstOrNull() ?: return base
            return InputLogic.focusEntry(base, first)
        }
    }
}

/** Game settings that change behaviour. */
data class PlayOptions(
    /** "Confirm correct words": a complete, correct word turns coloured and locked. */
    val confirmWords: Boolean = true,
    /** "Auto-check": marks errors as you type. Caps the level at 2 stars. */
    val autoCheck: Boolean = false,
    /** "Skip filled cells": the cursor jumps to the next empty cell. */
    val skipFilled: Boolean = true,
    /** "Accept letters without accents": A in an Ã cell counts as correct. */
    val acceptUnaccented: Boolean = false,
)
