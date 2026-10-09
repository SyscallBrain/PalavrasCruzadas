package app.palavrascruzadas.ui.settings

import app.palavrascruzadas.ui.components.ScreenHeader
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.time.LocalDate
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.palavrascruzadas.core.settings.AppSettings
import app.palavrascruzadas.ui.theme.AllThemes
import app.palavrascruzadas.ui.theme.Bricolage
import app.palavrascruzadas.ui.theme.CruzadasColors
import app.palavrascruzadas.ui.theme.Figtree

/** Settings screen. Every option has an effect on the game or the UI. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    colors: CruzadasColors,
    settings: AppSettings,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onResetProgress: () -> Unit,
    onCredits: () -> Unit,
    onBack: () -> Unit,
    onExport: (android.net.Uri) -> Unit = {},
    onPickBackup: (android.net.Uri) -> Unit = {},
    status: String? = null,
    onReminder: (Int?) -> Unit = {},
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var reminderStatus by remember { mutableStateOf<String?>(null) }
    val notificationsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            onReminder(AppSettings.DEFAULT_REMINDER_HOUR)
            reminderStatus = null
        } else {
            reminderStatus = "Sem permissão de notificações não há lembrete. Ativa-a nas definições do telemóvel."
        }
    }
    var confirmReset by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(onExport)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onPickBackup)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader("Definições", colors, onBack)

        Section("Aparência", colors) {
            Text("Tema", color = colors.text, fontFamily = Figtree)
            // Wrapped over several lines: the 5 themes on a single line did not fit on a phone.
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AllThemes.forEach { theme ->
                    val chosen = theme.id == settings.themeId
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (chosen) colors.accent else colors.surface)
                            .clickable { onChange { it.copy(themeId = theme.id) } }
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // Preview: the theme's background, text and accent.
                        Row(
                            modifier = Modifier
                                .width(64.dp)
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(theme.bg)
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(theme.accent))
                            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(theme.text))
                            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(theme.cell))
                        }
                        Text(
                            theme.name,
                            color = if (chosen) colors.onAccent else colors.text,
                            fontFamily = Figtree,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Tamanho da letra da grelha: ${settings.gridScale}%", color = colors.text, fontFamily = Figtree, modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = { onChange { it.withGridScale(-AppSettings.SCALE_STEP) } }) { Text("−", color = colors.accent) }
                    TextButton(onClick = { onChange { it.withGridScale(AppSettings.SCALE_STEP) } }) { Text("+", color = colors.accent) }
                }
            }
            SwitchRow("Modo daltónico", "Palavras completas ganham um símbolo além da cor", settings.colorblind, colors) {
                onChange { s -> s.copy(colorblind = it) }
            }
            Text("Reduzir animações", color = colors.text, fontFamily = Figtree)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf<Pair<String, Boolean?>>("Sistema" to null, "Sim" to true, "Não" to false).forEach { (label, value) ->
                    val chosen = settings.reduceMotion == value
                    Text(
                        label,
                        color = if (chosen) colors.onAccent else colors.text,
                        fontFamily = Figtree,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (chosen) colors.accent else colors.surface)
                            .clickable { onChange { it.copy(reduceMotion = value) } }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }

        Section("Jogo", colors) {
            SwitchRow("Confirmar palavras certas", "Palavra completa e certa fica colorida e bloqueada", settings.confirmWords, colors) {
                onChange { s -> s.copy(confirmWords = it) }
            }
            SwitchRow("Correção automática", "Marca erros logo ao escrever. Limita a 2 estrelas", settings.autoCheck, colors) {
                onChange { s -> s.copy(autoCheck = it) }
            }
            SwitchRow("Aceitar letras sem acento", "A na casa Ã conta como certo", settings.acceptUnaccented, colors) {
                onChange { s -> s.copy(acceptUnaccented = it) }
            }
            SwitchRow("Saltar casas preenchidas", "O cursor avança para a próxima casa vazia", settings.skipFilled, colors) {
                onChange { s -> s.copy(skipFilled = it) }
            }
        }

        Section("Sons e vibração", colors) {
            SwitchRow("Sons", "Tecla, palavra certa, ajuda, bónus e vitória", settings.sounds, colors) {
                onChange { s -> s.copy(sounds = it) }
            }
            SwitchRow("Vibração", "Vibração curta nas teclas e nas palavras certas", settings.vibration, colors) {
                onChange { s -> s.copy(vibration = it) }
            }
        }

        Section("Lembrete do diário", colors) {
            SwitchRow("Lembrete do diário", "Notifica à hora escolhida se o diário de hoje não estiver feito", settings.reminderHour != null, colors) { on ->
                if (!on) {
                    onReminder(null)
                } else if (app.palavrascruzadas.reminder.ReminderScheduler.hasPermission(context)) {
                    onReminder(AppSettings.DEFAULT_REMINDER_HOUR)
                } else {
                    notificationsLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            settings.reminderHour?.let { hour ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Hora: %02d:00".format(hour), color = colors.text, fontFamily = Figtree)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick = { onReminder((hour + 23) % 24) }) { Text("−", color = colors.accent) }
                        TextButton(onClick = { onReminder((hour + 1) % 24) }) { Text("+", color = colors.accent) }
                    }
                }
            }
            reminderStatus?.let { Text(it, color = colors.error, fontFamily = Figtree, fontSize = 13.sp) }
        }

        Section("Diário e dados", colors) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { exportLauncher.launch("palavras-cruzadas-${LocalDate.now()}.json") }) {
                    Text("Exportar progresso", color = colors.accent, fontFamily = Figtree)
                }
                TextButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                    Text("Importar progresso", color = colors.accent, fontFamily = Figtree)
                }
            }
            status?.let { Text(it, color = colors.mute, fontFamily = Figtree, fontSize = 13.sp) }
            if (!confirmReset) {
                TextButton(onClick = { confirmReset = true }) { Text("Repor progresso…", color = colors.error, fontFamily = Figtree) }
            } else {
                Text("Isto apaga a campanha, o diário, os cadernos e os recordes. Toca outra vez para confirmar.", color = colors.error, fontFamily = Figtree, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { confirmReset = false }) { Text("Cancelar", color = colors.accent, fontFamily = Figtree) }
                    TextButton(onClick = {
                        confirmReset = false
                        onResetProgress()
                    }) { Text("Repor tudo", color = colors.error, fontFamily = Figtree, fontWeight = FontWeight.Bold) }
                }
            }
            TextButton(onClick = onCredits) { Text("Créditos e licenças", color = colors.accent, fontFamily = Figtree) }
        }
    }
}

@Composable
private fun Section(title: String, colors: CruzadasColors, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = colors.mute, fontFamily = Figtree, fontSize = 13.sp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surface)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, colors: CruzadasColors, onChecked: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.text, fontFamily = Figtree)
            Text(subtitle, color = colors.mute, fontFamily = Figtree, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(checkedTrackColor = colors.accent, checkedThumbColor = colors.onAccent),
        )
    }
}

/** Credits and licences: fonts and sounds. */
@Composable
fun CreditsScreen(colors: CruzadasColors, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader("Créditos e licenças", colors, onBack)
        Text("Palavras Cruzadas — jogo offline, sem anúncios nem rede.", color = colors.text, fontFamily = Figtree)
        Text("Fontes: Bricolage Grotesque e Figtree (SIL Open Font License 1.1).", color = colors.mute, fontFamily = Figtree, fontSize = 13.sp)
        Text("Sons: sintetizados pela própria app (tools/make_sounds.py), sem material licenciado.", color = colors.mute, fontFamily = Figtree, fontSize = 13.sp)
        Text("Banco de pistas: escrito para este projeto (content/), validado por tools/validate_clues.py.", color = colors.mute, fontFamily = Figtree, fontSize = 13.sp)
    }
}
