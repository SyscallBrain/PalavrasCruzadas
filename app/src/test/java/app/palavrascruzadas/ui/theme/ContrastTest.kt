package app.palavrascruzadas.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AA contrast (WCAG 2.x, normal text ≥ 4.5:1) in every theme. Like the
 * `ColorblindTest` in Sopa de Letras, it only tests the colour values, without UI.
 */
class ContrastTest {

    private fun channel(c: Float): Double {
        val v = c.toDouble()
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(color: Color): Double =
        0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)

    private fun ratio(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    @Test
    fun everyThemeMeetsAaForTextOnItsBackgrounds() {
        val failures = mutableListOf<String>()
        for (t in AllThemes) {
            val pairs = listOf(
                "text/bg" to (t.text to t.bg),
                "text/surface" to (t.text to t.surface),
                "text/cell" to (t.text to t.cell),
                "mute/bg" to (t.mute to t.bg),
                "mute/surface" to (t.mute to t.surface),
                "accent/bg" to (t.accent to t.bg),
                "onAccent/accent" to (t.onAccent to t.accent),
                "error/bg" to (t.error to t.bg),
            )
            for ((name, pair) in pairs) {
                val r = ratio(pair.first, pair.second)
                if (r < 4.5) failures += "${t.id} $name = ${"%.2f".format(r)}"
            }
        }
        assertTrue("Pares abaixo de AA: $failures", failures.isEmpty())
    }
}
