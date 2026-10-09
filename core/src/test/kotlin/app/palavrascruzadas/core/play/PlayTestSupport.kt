package app.palavrascruzadas.core.play

import app.palavrascruzadas.core.model.Difficulty
import app.palavrascruzadas.core.model.Dir
import app.palavrascruzadas.core.model.Entry
import app.palavrascruzadas.core.model.Pos
import app.palavrascruzadas.core.model.Puzzle
import app.palavrascruzadas.core.model.PuzzleKind

/**
 * Small 5×5 puzzle used by the gameplay tests:
 *   MAR  (across, 1, at 0,0)   MESA (down, 1, at 0,0)   SAL (across, 3, at 2,0)
 *   M A R . .
 *   E . . . .
 *   S A L . .
 *   A . . . .
 */
internal fun samplePuzzle(): Puzzle = Puzzle(
    id = "t-play",
    kind = PuzzleKind.CLASSIC,
    width = 5,
    height = 5,
    entries = listOf(
        Entry(1, 0, 0, Dir.ACROSS, "MAR", "Pista A"),
        Entry(1, 0, 0, Dir.DOWN, "MESA", "Pista B"),
        Entry(3, 2, 0, Dir.ACROSS, "SAL", "Pista C"),
    ),
    keyword = null,
    givens = emptySet(),
    theme = null,
    difficulty = Difficulty.EASY,
    targetSeconds = 60,
)

internal fun sampleState(options: PlayOptions = PlayOptions()): PlayState =
    PlayState(puzzle = samplePuzzle(), options = options)

internal fun PlayState.type(vararg letters: Char): PlayState =
    letters.fold(this) { s, c -> InputLogic.type(s, c) }

internal val origin = Pos(0, 0)
