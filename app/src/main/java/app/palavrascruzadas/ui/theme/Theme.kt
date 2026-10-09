package app.palavrascruzadas.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalCruzadasColors = staticCompositionLocalOf { TokyoNight }

@Composable
fun CruzadasTheme(
    colors: CruzadasColors = TokyoNight,
    content: @Composable () -> Unit,
) {
    val scheme = if (colors.isLight) {
        lightColorScheme(
            background = colors.bg,
            surface = colors.surface,
            onBackground = colors.text,
            onSurface = colors.text,
            primary = colors.accent,
            onPrimary = colors.onAccent,
            secondary = colors.mute,
            error = colors.error,
        )
    } else {
        darkColorScheme(
            background = colors.bg,
            surface = colors.surface,
            onBackground = colors.text,
            onSurface = colors.text,
            primary = colors.accent,
            onPrimary = colors.onAccent,
            secondary = colors.mute,
            error = colors.error,
        )
    }
    CompositionLocalProvider(LocalCruzadasColors provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            content = content,
        )
    }
}
