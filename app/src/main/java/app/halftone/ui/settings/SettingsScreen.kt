package app.halftone.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.halftone.SessionState
import app.halftone.ui.Navigator
import app.halftone.ui.common.BackButton
import app.halftone.ui.common.ConfirmDialog
import app.halftone.ui.common.LocalAppContainer
import app.halftone.ui.common.HalftoneMark
import app.halftone.ui.common.formatBytes
import app.halftone.ui.theme.MistDim
import app.halftone.ui.theme.MistFaint
import kotlinx.coroutines.launch

private val qualities = listOf(0 to "Origineel", 320 to "320", 192 to "192", 128 to "128")

@Composable
fun SettingsScreen(navigator: Navigator, padding: PaddingValues) {
    val container = LocalAppContainer.current
    val session by container.session.collectAsStateWithLifecycle()
    val prefs by container.prefs.collectAsStateWithLifecycle()
    val bytes by container.downloads.bytesUsed.collectAsStateWithLifecycle()
    val downloadCount = container.downloads.items.collectAsStateWithLifecycle().value.values.count { it.isDone }
    val scope = rememberCoroutineScope()
    var confirmClear by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    val user = (session as? SessionState.LoggedIn)?.credentials

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = padding.calculateBottomPadding() + 24.dp),
        ) {
            Text(
                "Instellingen",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(start = 52.dp, top = 18.dp, bottom = 20.dp),
            )

            Group("Account") {
                if (user != null) {
                    SettingRow(
                        icon = Icons.Rounded.Person,
                        title = user.username,
                        text = user.serverUrl.removePrefix("https://").removePrefix("http://"),
                    )
                }
                SettingRow(
                    icon = Icons.AutoMirrored.Rounded.Logout,
                    title = "Uitloggen",
                    text = "Je downloads blijven bewaard",
                    onClick = { confirmLogout = true },
                )
            }

            Group("Afspelen") {
                SettingRow(
                    icon = Icons.Rounded.GraphicEq,
                    title = "Streamkwaliteit",
                    text = if (prefs.maxBitRate == 0) {
                        "Het originele bestand. Beste geluid en naadloze overgangen."
                    } else {
                        "Maximaal ${prefs.maxBitRate} kbps. Zuiniger met mobiele data."
                    },
                )
                Row(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 62.dp, end = 12.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    qualities.forEach { (kbps, label) ->
                        FilterChip(
                            selected = prefs.maxBitRate == kbps,
                            onClick = { scope.launch { container.settings.setMaxBitRate(kbps) } },
                            label = { Text(label) },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }
                SettingRow(
                    icon = Icons.Rounded.History,
                    title = "Scrobbelen",
                    text = "Geef door wat je luistert, voor 'Recent gespeeld' en 'Vaak gedraaid'. " +
                        "Navidrome stuurt het desgewenst door naar Last.fm of ListenBrainz.",
                    onClick = { scope.launch { container.settings.setScrobble(!prefs.scrobble) } },
                ) {
                    Switch(
                        checked = prefs.scrobble,
                        onCheckedChange = { scope.launch { container.settings.setScrobble(it) } },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }

            Group("Downloads") {
                SettingRow(
                    icon = Icons.Rounded.DeleteSweep,
                    title = "Alle downloads verwijderen",
                    text = "$downloadCount nummers  ·  ${formatBytes(bytes)}",
                    onClick = if (downloadCount > 0) {
                        { confirmClear = true }
                    } else {
                        null
                    },
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HalftoneMark(size = 36.dp, tile = true)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Halftone", style = MaterialTheme.typography.titleMedium)
                    Text("Versie 0.3 · voor Navidrome en Subsonic", style = MaterialTheme.typography.bodySmall, color = MistFaint)
                }
            }
        }

        BackButton(
            onBack = navigator::back,
            modifier = Modifier
                .statusBarsPadding()
                .padding(12.dp),
        )
    }

    if (confirmClear) {
        ConfirmDialog(
            title = "Alle downloads verwijderen?",
            text = "Dit maakt ${formatBytes(bytes)} vrij. Je muziek blijft gewoon op je server staan.",
            confirmLabel = "Verwijderen",
            onConfirm = {
                confirmClear = false
                container.downloads.removeAll()
            },
            onDismiss = { confirmClear = false },
        )
    }
    if (confirmLogout) {
        ConfirmDialog(
            title = "Uitloggen?",
            text = "Je kunt daarna inloggen op dezelfde of een andere server.",
            confirmLabel = "Uitloggen",
            onConfirm = {
                confirmLogout = false
                scope.launch { container.logOut() }
            },
            onDismiss = { confirmLogout = false },
        )
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(vertical = 6.dp),
    ) { content() }
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    text: String?,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (text != null) {
                Text(text, style = MaterialTheme.typography.bodySmall, color = MistDim)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}
