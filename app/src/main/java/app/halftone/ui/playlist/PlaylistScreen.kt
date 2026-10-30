package app.halftone.ui.playlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.halftone.data.PlaylistWithSongs
import app.halftone.ui.Navigator
import app.halftone.ui.common.BackButton
import app.halftone.ui.common.CollectionHeader
import app.halftone.ui.common.ConfirmDialog
import app.halftone.ui.common.CoverArt
import app.halftone.ui.common.CoverGlow
import app.halftone.ui.common.EmptyView
import app.halftone.ui.common.GroupDownloadButton
import app.halftone.ui.common.LoadContent
import app.halftone.ui.common.LoadViewModel
import app.halftone.ui.common.LocalAppContainer
import app.halftone.ui.common.MenuAction
import app.halftone.ui.common.OverflowMenu
import app.halftone.ui.common.SongRow
import app.halftone.ui.common.appViewModel
import app.halftone.ui.common.formatTotal
import app.halftone.ui.common.rememberGroupDownload
import app.halftone.ui.common.songMenu
import app.halftone.ui.theme.rememberCoverColors
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import app.halftone.R

@Composable
fun PlaylistScreen(playlistId: String, navigator: Navigator, padding: PaddingValues) {
    val vm = appViewModel(key = "playlist-$playlistId") { c ->
        LoadViewModel(refreshOn = c.repository.playlistVersion) { c.repository.playlist(playlistId) }
    }
    val state by vm.state.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        LoadContent(state, onRetry = vm::reload) { playlist ->
            PlaylistContent(playlist, navigator, padding)
        }
        BackButton(
            onBack = navigator::back,
            modifier = Modifier
                .statusBarsPadding()
                .padding(12.dp),
        )
    }
}

@Composable
private fun PlaylistContent(playlist: PlaylistWithSongs, navigator: Navigator, padding: PaddingValues) {
    val container = LocalAppContainer.current
    val playerState by container.player.state.collectAsStateWithLifecycle()
    val colors = rememberCoverColors(container.client.coverArtUrl(playlist.coverArt))
    val songs = playlist.entry
    val download = rememberGroupDownload(songs)
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        CoverGlow(colors)

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 24.dp),
        ) {
            item {
                CollectionHeader(
                    coverArt = playlist.coverArt,
                    title = playlist.name,
                    subtitle = playlist.comment?.takeIf { it.isNotBlank() },
                    meta = listOfNotNull(
                        playlist.owner?.let { stringResource(R.string.by_owner, it) },
                        pluralStringResource(R.plurals.songs_count, songs.size, songs.size),
                        formatTotal(playlist.duration),
                    ).joinToString("  ·  "),
                    colors = colors,
                    placeholderIcon = Icons.AutoMirrored.Rounded.QueueMusic,
                    playEnabled = songs.isNotEmpty(),
                    onPlay = { container.player.playSongs(songs) },
                    onShuffle = { container.player.playSongs(songs.shuffled()) },
                ) {
                    GroupDownloadButton(
                        state = download.state,
                        progress = download.progress,
                        onDownload = { container.downloads.download(songs) },
                        onRemove = { container.downloads.remove(songs.map { it.id }) },
                    )
                    OverflowMenu(
                        listOf(
                            MenuAction(stringResource(R.string.rename), Icons.Rounded.Edit) { renaming = true },
                            MenuAction(stringResource(R.string.delete_playlist), Icons.Rounded.Delete) { deleting = true },
                        ),
                        tint = Color.White.copy(alpha = 0.75f),
                    )
                }
            }

            if (songs.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                    ) {
                        EmptyView(
                            Icons.AutoMirrored.Rounded.QueueMusic,
                            stringResource(R.string.playlist_empty_title),
                            stringResource(R.string.playlist_empty_text),
                        )
                    }
                }
            }

            itemsIndexed(songs, key = { i, s -> "$i-${s.id}" }) { index, song ->
                val isCurrent = playerState.current?.mediaId == song.id
                SongRow(
                    title = song.title,
                    subtitle = listOfNotNull(song.artist, song.album).joinToString(" · "),
                    durationSec = song.duration,
                    isCurrent = isCurrent,
                    songId = song.id,
                    onClick = { container.player.playSongs(songs, index) },
                    menu = songMenu(
                        song,
                        onOpenAlbum = navigator::album,
                        onOpenArtist = navigator::artist,
                        extra = listOf(
                            MenuAction(stringResource(R.string.remove_from_playlist), Icons.Rounded.RemoveCircleOutline) {
                                container.launchSafely {
                                    container.repository.removeFromPlaylist(playlist.id, index)
                                }
                            },
                        ),
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    CoverArt(song.coverArt, Modifier.size(46.dp), shape = MaterialTheme.shapes.small)
                }
            }
        }
    }

    if (renaming) {
        PlaylistNameDialog(
            title = stringResource(R.string.rename),
            confirmLabel = stringResource(R.string.save),
            initial = playlist.name,
            onConfirm = { name ->
                renaming = false
                container.launchSafely { container.repository.renamePlaylist(playlist.id, name) }
            },
            onDismiss = { renaming = false },
        )
    }
    if (deleting) {
        ConfirmDialog(
            title = stringResource(R.string.delete_playlist_title, playlist.name),
            text = stringResource(R.string.delete_playlist_text),
            confirmLabel = stringResource(R.string.remove),
            onConfirm = {
                deleting = false
                navigator.back()
                container.launchSafely { container.repository.deletePlaylist(playlist.id) }
            },
            onDismiss = { deleting = false },
        )
    }
}
