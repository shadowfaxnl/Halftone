package app.halftone.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.halftone.data.Playlist
import app.halftone.data.Song
import app.halftone.ui.common.CoverArt
import app.halftone.ui.common.ErrorView
import app.halftone.ui.common.Load
import app.halftone.ui.common.LoadViewModel
import app.halftone.ui.common.LocalAppContainer
import app.halftone.ui.common.appViewModel
import app.halftone.ui.theme.Ink2
import app.halftone.ui.theme.MistDim
import app.halftone.ui.theme.MistFaint
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import app.halftone.R
import app.halftone.Str

/** Onderblad waarin je kiest aan welke afspeellijst je nummers toevoegt. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(songs: List<Song>, onDismiss: () -> Unit) {
    val container = LocalAppContainer.current
    val vm = appViewModel(key = "pick-playlist") { c ->
        LoadViewModel(refreshOn = c.repository.playlistVersion) { c.repository.playlists() }
    }
    val state by vm.state.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val countLabel = pluralStringResource(R.plurals.songs_count, songs.size, songs.size)

    fun addTo(playlist: Playlist) {
        onDismiss()
        container.launchSafely {
            container.repository.addToPlaylist(playlist.id, songs.map { it.id })
            container.toast(Str.get(R.string.added_to_playlist, countLabel, playlist.name))
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Ink2) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text(
                stringResource(R.string.add_to_playlist_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Text(
                countLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MistFaint,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
            )

            SheetRow(
                title = stringResource(R.string.new_playlist),
                subtitle = null,
                onClick = { creating = true },
            ) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary)
                }
            }

            when (val s = state) {
                is Load.Loading -> Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }

                is Load.Failed -> Box(Modifier.heightIn(max = 320.dp)) { ErrorView(s.message, vm::reload) }

                is Load.Ready -> LazyColumn(
                    Modifier.heightIn(max = 460.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    items(s.data, key = { it.id }) { playlist ->
                        SheetRow(
                            title = playlist.name,
                            subtitle = pluralStringResource(R.plurals.songs_count, playlist.songCount, playlist.songCount),
                            onClick = { addTo(playlist) },
                        ) {
                            CoverArt(
                                playlist.coverArt,
                                Modifier.size(52.dp),
                                shape = MaterialTheme.shapes.small,
                                placeholderIcon = Icons.AutoMirrored.Rounded.QueueMusic,
                            )
                        }
                    }
                }
            }
        }
    }

    if (creating) {
        PlaylistNameDialog(
            title = stringResource(R.string.new_playlist),
            confirmLabel = stringResource(R.string.create),
            onConfirm = { name ->
                creating = false
                onDismiss()
                container.launchSafely {
                    container.repository.createPlaylist(name, songs.map { it.id })
                    container.toast(Str.get(R.string.playlist_created, name))
                }
            },
            onDismiss = { creating = false },
        )
    }
}

@Composable
private fun SheetRow(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MistDim)
            }
        }
    }
}
