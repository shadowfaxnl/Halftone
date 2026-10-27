package app.halftone.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Language
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.halftone.desktop.APP_VERSION
import app.halftone.desktop.i18n.S
import app.halftone.desktop.ui.common.ConfirmDialog
import app.halftone.desktop.ui.common.HalftoneMark
import app.halftone.desktop.ui.common.LocalApp
import app.halftone.desktop.ui.common.ScreenTitle
import app.halftone.desktop.ui.common.formatBytes
import app.halftone.desktop.ui.common.observe
import app.halftone.desktop.ui.theme.MistDim
import app.halftone.desktop.ui.theme.MistFaint

@Composable
fun SettingsScreen() {
    val app = LocalApp.current
    val settings = app.settings.state.observe()
    val bytes = app.downloads.bytesUsed.observe()
    val downloadCount = app.downloads.items.observe().values.count { it.isDone }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    val startLanguage = remember { settings.language }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenTitle(S.settings)
        Column(Modifier.padding(horizontal = 32.dp).widthIn(max = 720.dp).padding(bottom = 32.dp)) {
            Group(S.account) {
                settings.credentials?.let { c ->
                    SettingRow(Icons.Rounded.Person, c.username, c.serverUrl.removePrefix("https://").removePrefix("http://"))
                }
                SettingRow(Icons.AutoMirrored.Rounded.Logout, S.signOut, S.signOutKeepsDownloads, onClick = { confirmLogout = true })
            }

            Group(S.playback) {
                SettingRow(
                    Icons.Rounded.GraphicEq,
                    S.streamingQuality,
                    if (settings.maxBitRate == 0) S.qualityOriginalText else S.qualityLimitedText(settings.maxBitRate),
                )
                Chips(
                    options = listOf(0, 320, 192, 128),
                    selected = settings.maxBitRate,
                    label = { if (it == 0) S.qualityOriginal else "$it" },
                    onSelect = { v -> app.settings.update { it.copy(maxBitRate = v) } },
                )
                SettingRow(
                    Icons.Rounded.History, S.scrobbling, S.scrobblingText,
                    onClick = { app.settings.update { it.copy(scrobble = !it.scrobble) } },
                ) {
                    Switch(
                        checked = settings.scrobble,
                        onCheckedChange = { on -> app.settings.update { it.copy(scrobble = on) } },
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary, checkedThumbColor = MaterialTheme.colorScheme.onPrimary),
                    )
                }
            }

            Group(S.language) {
                SettingRow(
                    Icons.Rounded.Language,
                    S.language,
                    if (settings.language != startLanguage) S.languageRestart else null,
                )
                Chips(
                    options = listOf("system", "en", "nl"),
                    selected = settings.language,
                    label = { when (it) { "en" -> "English"; "nl" -> "Nederlands"; else -> S.languageSystem } },
                    onSelect = { v -> app.settings.update { it.copy(language = v) } },
                )
            }

            Group(S.downloads) {
                SettingRow(
                    Icons.Rounded.DeleteSweep,
                    S.removeAllDownloads,
                    "${S.songsCount(downloadCount)}  ·  ${formatBytes(bytes)}",
                    onClick = if (downloadCount > 0) {
                        { confirmClear = true }
                    } else {
                        null
                    },
                )
            }

            Group(S.shortcuts) {
                SettingRow(Icons.Rounded.Keyboard, S.shortcuts, S.shortcutsText)
            }

            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                HalftoneMark(size = 36.dp, tile = true)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Halftone", style = MaterialTheme.typography.titleMedium)
                    Text(S.versionLine(APP_VERSION), style = MaterialTheme.typography.bodySmall, color = MistFaint)
                }
            }
        }
    }

    if (confirmClear) {
        ConfirmDialog(S.removeAllDownloadsTitle, S.removeAllDownloadsText(formatBytes(bytes)), S.remove, onConfirm = {
            confirmClear = false
            app.downloads.removeAll()
        }, onDismiss = { confirmClear = false })
    }
    if (confirmLogout) {
        ConfirmDialog(S.signOutTitle, S.signOutText, S.signOut, onConfirm = {
            confirmLogout = false
            app.logOut()
        }, onDismiss = { confirmLogout = false })
    }
}

@Composable
private fun <T> Chips(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Row(Modifier.padding(start = 62.dp, end = 12.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { opt ->
            FilterChip(
                selected = selected == opt,
                onClick = { onSelect(opt) },
                label = { Text(label(opt)) },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.05f)).padding(vertical = 6.dp)) { content() }
    Spacer(Modifier.height(22.dp))
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, text: String?, onClick: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = onClick != null) { onClick?.invoke() }.padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!text.isNullOrEmpty()) Text(text, style = MaterialTheme.typography.bodySmall, color = MistDim)
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}
