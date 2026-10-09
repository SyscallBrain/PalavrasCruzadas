package app.palavrascruzadas.ui.books

import app.palavrascruzadas.ui.components.ScreenHeader
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.palavrascruzadas.core.books.BooksProgress
import app.palavrascruzadas.core.books.Books
import app.palavrascruzadas.data.ProgressData
import app.palavrascruzadas.ui.newgame.THEMES
import app.palavrascruzadas.ui.theme.Bricolage
import app.palavrascruzadas.ui.theme.CruzadasColors
import app.palavrascruzadas.ui.theme.Figtree

/** List of the books, each with a coloured cover and its progress. */
@Composable
fun BooksScreen(colors: CruzadasColors, progress: BooksProgress, onBack: () -> Unit, onOpenBook: (String) -> Unit) {
    val names = THEMES.toMap() + SPECIAL_NAMES
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader("Cadernos", colors, onBack)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(Books.THEMES + Books.SPECIAL) { theme ->
                val cover = colors.palette[(Books.THEMES + Books.SPECIAL).indexOf(theme) % colors.palette.size]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surface)
                        .clickable { onOpenBook(theme) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.width(8.dp).height(64.dp).background(cover))
                    Column(modifier = Modifier.padding(14.dp).weight(1f)) {
                        Text(names[theme] ?: theme, color = colors.text, fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(
                            "${progress.doneCount(theme)} de ${Books.puzzlesIn(theme)} · ★ ${progress.starTotal(theme)}",
                            color = colors.mute,
                            fontFamily = Figtree,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }
    }
}

/** The puzzles of a book: size from 7×7 to 11×11, stars per puzzle. */
@Composable
fun BookScreen(
    colors: CruzadasColors,
    theme: String,
    progress: BooksProgress,
    onBack: () -> Unit,
    onOpenPuzzle: (Int) -> Unit,
) {
    val title = THEMES.firstOrNull { it.first == theme }?.second ?: SPECIAL_NAMES[theme] ?: theme
    val stars = progress.stars[theme].orEmpty()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(title, colors, onBack)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items((1..Books.puzzlesIn(theme)).toList()) { number ->
                val size = 7 + Math.round(4.0 * (number - 1) / (Books.PUZZLES_PER_BOOK - 1)).toInt()
                val label = if (theme in Books.SPECIAL) "Puzzle $number" else "${size}×$size"
                val earned = stars[number] ?: 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface)
                        .clickable { onOpenPuzzle(number) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Puzzle $number", color = colors.text, fontFamily = Figtree, modifier = Modifier.weight(1f))
                    Text(label, color = colors.mute, fontFamily = Figtree, fontSize = 13.sp)
                    Text(
                        if (earned > 0) "★".repeat(earned) + "☆".repeat(3 - earned) else "☆☆☆",
                        color = if (earned > 0) colors.accent else colors.mute,
                        fontFamily = Figtree,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
    }
}

/** Names of the type books. */
private val SPECIAL_NAMES = mapOf(
    "roda" to "Roda de letras",
    "diretas" to "Diretas",
    "encaixar" to "Encaixar palavras",
)
