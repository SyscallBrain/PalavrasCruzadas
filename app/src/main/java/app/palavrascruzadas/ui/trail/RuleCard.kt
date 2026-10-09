package app.palavrascruzadas.ui.trail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.input.pointer.pointerInput
import app.palavrascruzadas.core.campaign.LevelType
import app.palavrascruzadas.ui.theme.Bricolage
import app.palavrascruzadas.ui.theme.CruzadasColors
import app.palavrascruzadas.ui.theme.Figtree

/**
 * Rule card of a level type, shown the first time it appears.
 * Two pages; the last button calls [onDone].
 */
@Composable
fun RuleCard(
    title: String,
    pages: List<String>,
    colors: CruzadasColors,
    onDone: () -> Unit,
) {
    var page by remember { mutableIntStateOf(0) }
    val last = page == pages.lastIndex
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg.copy(alpha = 0.96f))
            // Taps do not reach the screen underneath.
            .pointerInput(Unit) { detectTapGestures { } }
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, color = colors.accent, fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text(
            pages[page],
            color = colors.text,
            fontFamily = Figtree,
            fontSize = 16.sp,
            modifier = Modifier.padding(vertical = 24.dp),
        )
        Text("${page + 1} / ${pages.size}", color = colors.mute, fontFamily = Figtree, fontSize = 12.sp)
        Button(
            onClick = { if (last) onDone() else page++ },
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
        ) {
            Text(if (last) "Começar" else "Seguinte", fontFamily = Figtree, fontWeight = FontWeight.Bold)
        }
    }
}

/** Texts of the rule cards of each level type, shown before the first time it appears. */
object RuleCards {
    const val BOSS = "boss"

    data class Card(val key: String, val title: String, val pages: List<String>)

    val boss = listOf(
        "Chefe do mundo: a grelha é um lado maior do que o nível anterior e tem mais 3 palavras. É o último nível de cada mundo.",
        "Ao concluir o chefe ganhas 3 ajudas, uma vez por mundo. Com 3 estrelas ganhas também a recompensa normal.",
    )

    /** Card of the level type, or null for the classic one (which needs no explanation). */
    fun forType(type: LevelType): Card? = when (type) {
        LevelType.BOSS -> Card(BOSS, "Chefe", boss)
        LevelType.WHEEL -> Card(
            "wheel",
            "Roda de letras",
            listOf(
                "Arrasta o dedo pelas letras da roda para formar palavras. Voltar à letra anterior desfaz a última.",
                "As palavras da grelha preenchem-se sozinhas. As outras palavras válidas são bónus: a cada 5 ganhas 1 ajuda.",
            ),
        )
        LevelType.KEYWORD -> Card(
            "keyword",
            "Palavra-chave",
            listOf(
                "Algumas casas têm um número no canto. As letras dessas casas, por ordem, formam uma palavra escondida.",
                "Toca na faixa por baixo da grelha para a tentar adivinhar antes do fim. Acertar dá 1 ajuda; errar não tira nada.",
            ),
        )
        LevelType.DIRETAS -> Card(
            "diretas",
            "Diretas",
            listOf(
                "Aqui as pistas estão dentro da grelha, nas casas escuras. A seta mostra para onde vai a resposta.",
                "Toca numa casa-pista para escrever a resposta. Se tiver duas setas, toca outra vez para trocar.",
            ),
        )
        LevelType.FIT_IT -> Card(
            "encaixar",
            "Encaixar palavras",
            listOf(
                "Não há pistas: tens a lista das palavras, agrupadas por tamanho, e uma já está posta para começar.",
                "Escolhe uma palavra e toca numa das casas tracejadas onde ela cabe. Também podes escrever letra a letra.",
            ),
        )
        LevelType.CLASSIC -> null
    }
}
