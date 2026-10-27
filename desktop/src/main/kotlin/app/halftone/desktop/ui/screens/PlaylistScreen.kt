package app.halftone.desktop.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.halftone.desktop.data.PlaylistWithSongs
import app.halftone.desktop.i18n.S
import app.halftone.desktop.ui.common.CollectionHeader
import app.halftone.desktop.ui.common.ConfirmDialog
import app.halftone.desktop.ui.common.CoverArt
import app.halftone.desktop.ui.common.CoverGlow
import app.halftone.desktop.ui.common.EmptyView
import app.halftone.desktop.ui.common.GroupDownloadButton
import app.halftone.desktop.ui.common.LoadContent
import app.halftone.desktop.ui.common.LocalApp
import app.halftone.desktop.ui.common.MenuAction
import app.halftone.desktop.ui.common.OverflowMenu
import app.halftone.desktop.ui.common.SongRow
import app.halftone.desktop.ui.common.formatTotal
import app.halftone.desktop.ui.common.observe
import app.halftone.desktop.ui.common.rememberLoad
import app.halftone.desktop.ui.common.songMenu
import app.halftone.desktop.ui.theme.rememberCoverColors

@Composable
fun PlaylistScreen(id: String) {
    val app = LocalApp.current
    val version = app.repository.playlistVersion.observe()
    val load = rememberLoad("playlist-$id", refreshKey = version) { app.repository.playlist(id) }
    LoadContent(load) { PlaylistContent(it) }
}

@Composable
private fun PlaylistContent(playlist: PlaylistWithSongs) {
    val app = LocalApp.current
    val ps = app.player.state.observe()
    val colors = rememberCoverColors(app.okHttp, app.client.coverArtUrl(playlist.coverArt, 64))
    val songs = playlist.entry
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        CoverGlow(colors)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                CollectionHeader(
                    coverArt = playlist.coverArt,
                    kind = S.kindPlaylist,
                    title = playlist.name,
                    subtitle = playlist.comment?.takeIf { it.isNotBlank() },
                    meta = listOfNotNull(playlist.owner?.let { S.byOwner(it) }, S.songsCount(songs.size), formatTotal(playlist.duration))
                        .joinToString("  ·  "),
                    colors = colors,
                    placeholderIcon = Icons.AutoMirrored.Rounded.QueueMusic,
                    playEnabled = songs.isNotEmpty(),
                    onPlay = { app.play(songs) },
                    onShuffle = { app.play(songs.shuffled()) },
                ) {
                    GroupDownloadButton(songs)
                    OverflowMenu(
                        listOf(
                            MenuAction(S.rename, Icons.Rounded.Edit) { renaming = true },
                            MenuAction(S.deletePlaylist, Icons.Rounded.Delete) { deleting = true },
                        ),
                        tint = Color.White.copy(alpha = 0.75f),
                    )
                }
            }
            if (songs.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().height(260.dp)) {
                        EmptyView(Icons.AutoMirrored.Rounded.QueueMusic, S.playlistEmptyTitle, S.playlistEmptyText)
                    }
                }
            }
            itemsIndexed(songs, key = { i, s -> "$i-${s.id}" }) { index, song ->
                SongRow(
                    song = song,
                    isCurrent = ps.current?.id == song.id,
                    subtitle = listOfNotNull(song.artist, song.album).joinToString(" · "),
                    onClick = { app.play(songs, index) },
                    menu = songMenu(
                        song,
                        extra = listOf(
                            MenuAction(S.removeFromPlaylist, Icons.Rounded.RemoveCircleOutline) {
                                app.launchSafely { app.repository.removeFromPlaylist(playlist.id, index) }
                            },
                        ),
                    ),
                    modifier = Modifier.padding(horizontal = 20.dp),
                ) { CoverArt(song.coverArt, Modifier.size(40.dp), shape = MaterialTheme.shapes.small) }
            }
        }
    }

    if (renaming) {
        PlaylistNameDialog(S.rename, S.save, initial = playlist.name, onConfirm = { name ->
            renaming = false
            app.launchSafely { app.repository.renamePlaylist(playlist.id, name) }
        }, onDismiss = { renaming = false })
    }
    if (deleting) {
        ConfirmDialog(S.deletePlaylistTitle(playlist.name), S.deletePlaylistText, S.remove, onConfirm = {
            deleting = false
            app.nav.back()
            app.launchSafely { app.repository.deletePlaylist(playlist.id) }
        }, onDismiss = { deleting = false })
    }
}
