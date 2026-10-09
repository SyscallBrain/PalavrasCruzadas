package app.palavrascruzadas.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// The five themes (Tokyo Night, Storm, Moon, Day and Tinta Âmbar).
@Immutable
data class CruzadasColors(
    val id: String,
    val name: String,
    val bg: Color,
    val surface: Color,
    val cell: Color,
    val text: Color,
    val mute: Color,
    val accent: Color,
    val onAccent: Color,
    val error: Color,
    val palette: List<Color>,
    val isLight: Boolean = false,
) {
    // Neon selection: the accent is the glow colour.
    val neon: Color get() = accent
}

private val TokyoPalette = listOf(
    Color(0xFF7AA2F7), Color(0xFFBB9AF7), Color(0xFF9ECE6A), Color(0xFFFF9E64), Color(0xFFF7768E),
)

val TokyoNight = CruzadasColors(
    id = "tokyo-night",
    name = "Tokyo Night",
    bg = Color(0xFF1A1B26),
    surface = Color(0xFF24283B),
    cell = Color(0xFF292E42),
    text = Color(0xFFC0CAF5),
    mute = Color(0xFF8C94BE),
    accent = Color(0xFF7AA2F7),
    onAccent = Color(0xFF16161E),
    error = Color(0xFFF7768E),
    palette = TokyoPalette,
)

val TokyoNightStorm = CruzadasColors(
    id = "tokyo-night-storm",
    name = "Tokyo Night Storm",
    bg = Color(0xFF24283B),
    surface = Color(0xFF1F2335),
    cell = Color(0xFF2F3549),
    text = Color(0xFFC0CAF5),
    mute = Color(0xFF979DBA),
    accent = Color(0xFF7AA2F7),
    onAccent = Color(0xFF1A1B26),
    error = Color(0xFFF7768E),
    palette = TokyoPalette,
)

val TokyoNightMoon = CruzadasColors(
    id = "tokyo-night-moon",
    name = "Tokyo Night Moon",
    bg = Color(0xFF222436),
    surface = Color(0xFF1E2030),
    cell = Color(0xFF2F334D),
    text = Color(0xFFC8D3F5),
    mute = Color(0xFF949BC2),
    accent = Color(0xFF82AAFF),
    onAccent = Color(0xFF1B1D2B),
    error = Color(0xFFFF757F),
    palette = listOf(
        Color(0xFF82AAFF), Color(0xFFC099FF), Color(0xFFC3E88D), Color(0xFFFF966C), Color(0xFFFF757F),
    ),
)

val TokyoNightDay = CruzadasColors(
    id = "tokyo-night-day",
    name = "Tokyo Night Day",
    bg = Color(0xFFE1E2E7),
    surface = Color(0xFFD0D5E3),
    cell = Color(0xFFC4C8DA),
    text = Color(0xFF2E51A0),
    mute = Color(0xFF46527F),
    accent = Color(0xFF2463B8),
    onAccent = Color(0xFFFFFFFF),
    error = Color(0xFFBF214F),
    palette = listOf(
        Color(0xFF2E7DE9), Color(0xFF9854F1), Color(0xFF587539), Color(0xFFB15C00), Color(0xFFF52A65),
    ),
    isLight = true,
)

val TintaAmbar = CruzadasColors(
    id = "tinta-ambar",
    name = "Tinta Âmbar",
    bg = Color(0xFF0F1424),
    surface = Color(0xFF171E35),
    cell = Color(0xFF1B2440),
    text = Color(0xFFE8EAF4),
    mute = Color(0xFF8B93B5),
    accent = Color(0xFFF4B860),
    onAccent = Color(0xFF1A1304),
    error = Color(0xFFFF8FA3),
    palette = listOf(
        Color(0xFFF4B860), Color(0xFF7CC6FE), Color(0xFFC3A6FF), Color(0xFF8EE3C2), Color(0xFFFF8FA3),
    ),
)

val AllThemes = listOf(TokyoNight, TokyoNightStorm, TokyoNightMoon, TokyoNightDay, TintaAmbar)
