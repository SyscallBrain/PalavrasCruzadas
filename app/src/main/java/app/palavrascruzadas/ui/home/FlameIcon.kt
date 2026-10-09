package app.palavrascruzadas.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp

/** Daily streak flame, drawn (not an emoji). */
@Composable
fun FlameIcon(color: Color, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.02f)
            cubicTo(w * 0.62f, h * 0.28f, w * 0.95f, h * 0.45f, w * 0.9f, h * 0.7f)
            cubicTo(w * 0.86f, h * 0.92f, w * 0.68f, h * 0.99f, w * 0.5f, h * 0.99f)
            cubicTo(w * 0.3f, h * 0.99f, w * 0.12f, h * 0.9f, w * 0.1f, h * 0.68f)
            cubicTo(w * 0.09f, h * 0.5f, w * 0.25f, h * 0.38f, w * 0.32f, h * 0.22f)
            cubicTo(w * 0.36f, h * 0.44f, w * 0.44f, h * 0.5f, w * 0.5f, h * 0.02f)
            close()
        }
        drawPath(path, color)
        // Miolo mais claro: a chama tem dois tons.
        drawCircle(color.copy(alpha = 0.55f), radius = w * 0.14f, center = Offset(w * 0.5f, h * 0.72f))
    }
}

/** Hint bulb, drawn like the flame: does not depend on the system emoji font. */
@Composable
fun BulbIcon(color: Color, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawCircle(color, radius = w * 0.36f, center = Offset(w * 0.5f, h * 0.4f))
        drawRect(color, topLeft = Offset(w * 0.33f, h * 0.62f), size = androidx.compose.ui.geometry.Size(w * 0.34f, h * 0.16f))
        drawRect(color.copy(alpha = 0.7f), topLeft = Offset(w * 0.36f, h * 0.82f), size = androidx.compose.ui.geometry.Size(w * 0.28f, h * 0.1f))
    }
}
