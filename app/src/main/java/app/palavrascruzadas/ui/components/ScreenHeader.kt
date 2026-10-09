package app.palavrascruzadas.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.palavrascruzadas.ui.theme.Bricolage
import app.palavrascruzadas.ui.theme.CruzadasColors

/**
 * Header of the menu screens: back arrow and title on the same line, like the trail and
 * game screens. The arrow goes to the previous screen (same as the system back button).
 */
@Composable
fun ScreenHeader(title: String, colors: CruzadasColors, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        // Pulled to the left so the arrow lines up with the content margin.
        IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = colors.text)
        }
        Text(
            title,
            color = colors.text,
            fontFamily = Bricolage,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.offset(x = (-8).dp),
        )
    }
}
