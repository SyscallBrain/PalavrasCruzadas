package app.palavrascruzadas.ui.layout

/** Game screen layout: phone column or two columns (grid on the left, clues on the right). */
enum class GameLayout { COMPACT, TWO_COLUMNS }

/**
 * Picks the layout. Two columns on medium or large screens (≥ 600 dp) and in
 * landscape; a phone in portrait gets a single column. Pure function, tested for several widths.
 */
fun chooseGameLayout(widthDp: Float, heightDp: Float): GameLayout =
    if (widthDp >= TWO_COLUMN_MIN_WIDTH_DP || widthDp > heightDp) GameLayout.TWO_COLUMNS else GameLayout.COMPACT

/** Width from which the screen is medium (Material Medium window class, 600 dp). */
const val TWO_COLUMN_MIN_WIDTH_DP = 600f
