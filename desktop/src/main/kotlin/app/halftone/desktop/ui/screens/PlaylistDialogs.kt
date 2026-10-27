package app.halftone.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.halftone.desktop.data.Playlist
import app.halftone.desktop.data.Song
import app.halftone.desktop.i18n.S
import app.halftone.desktop.ui.common.CoverArt
import app.halftone.desktop.ui.common.Load
import app.halftone.desktop.ui.common.LocalApp
import app.halftone.desktop.ui.common.observe
import app.halftone.desktop.ui.common.rememberLoad
import app.halftone.desktop.ui.theme.Ink3
import app.halftone.desktop.ui.theme.MistDim

@Composable
fun PlaylistNameDialog(title: String, confirmLabel: String, initial: String = "", onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val app = LocalApp.current
    var name by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    val valid = name.isNotBlank()
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text(S.playlistNameHint) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (valid) onConfirm(name.trim()) }),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().focusRequester(focus).onFocusChanged { app.typing = it.isFocused },
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(name.trim()) }, enabled = valid) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(S.cancel) } },
        containerColor = Ink3,
        shape = MaterialTheme.shapes.large,
    )
}

/** Kies een afspeellijst voor de gekozen nummers, of maak een nieuwe. */
@Composable
fun AddToPlaylistDialog(songs: List<Song>, onDismiss: () -> Unit) {
    val app = LocalApp.current
    val version = app.repository.playlistVersion.observe()
    val load = rememberLoad("pick-playlist", refreshKey = version) { app.repository.playlists() }
    var creating by remember { mutableStateOf(false) }
    val countLabel = S.songsCount(songs.size)

    fun addTo(p: Playlist) {
        onDismiss()
        app.launchSafely {
            app.repository.addToPlaylist(p.id, songs.map { it.id })
            app.toast(S.addedToPlaylist(countLabel, p.name))
        }
    }

    if (creating) {
        PlaylistNameDialog(S.newPlaylist, S.create, onConfirm = { name ->
            creating = false
            onDismiss()
            app.launchSafely {
                app.repository.createPlaylist(name, songs.map { it.id })
                app.toast(S.playlistCreated(name))
            }
        }, onDismiss = { creating = false })
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(S.addToPlaylist)
                Text(countLabel, style = MaterialTheme.typography.bodyMedium, color = MistDim)
            }
        },
        text = {
            Column(Modifier.width(380.dp)) {
                PickRow(S.newPlaylist, null, onClick = { creating = true }) {
                    Box(
                        Modifier.size(44.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary) }
                }
                when (val s = load.state) {
                    is Load.Loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                    is Load.Failed -> Text(s.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp))
                    is Load.Ready -> LazyColumn(Modifier.heightIn(max = 380.dp)) {
                        items(s.data, key = { it.id }) { p ->
                            PickRow(p.name, S.songsCount(p.songCount), onClick = { addTo(p) }) {
                                CoverArt(p.coverArt, Modifier.size(44.dp), shape = MaterialTheme.shapes.small, placeholderIcon = Icons.AutoMirrored.Rounded.QueueMusic)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(S.cancel) } },
        containerColor = Ink3,
        shape = MaterialTheme.shapes.large,
    )
}

@Composable
private fun PickRow(title: String, subtitle: String?, onClick: () -> Unit, leading: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable(onClick = onClick).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MistDim)
        }
    }
}
