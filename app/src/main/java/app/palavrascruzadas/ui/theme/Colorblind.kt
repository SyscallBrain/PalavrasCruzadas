package app.palavrascruzadas.ui.theme

private val COLORBLIND_SYMBOLS = listOf("•", "○", "/", "◆", "✕")

/** Symbol of a word colour, for colour-blind mode (taken from Sopa de Letras). */
fun colorblindSymbol(colorIndex: Int): String =
    COLORBLIND_SYMBOLS[colorIndex % COLORBLIND_SYMBOLS.size]
