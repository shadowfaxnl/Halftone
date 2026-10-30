package app.halftone.ui.album

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.halftone.data.AlbumWithSongs
import app.halftone.data.StarKind
import app.halftone.ui.Navigator
import app.halftone.ui.common.BackButton
import app.halftone.ui.common.CollectionHeader
import app.halftone.ui.common.CoverGlow
import app.halftone.ui.common.GroupDownloadButton
import app.halftone.ui.common.HeartButton
import app.halftone.ui.common.LoadContent
import app.halftone.ui.common.LoadViewModel
import app.halftone.ui.common.LocalAppContainer
import app.halftone.ui.common.MenuAction
import app.halftone.ui.common.OverflowMenu
import app.halftone.ui.common.SongRow
import app.halftone.ui.common.TrackNumber
import app.halftone.ui.common.appViewModel
import app.halftone.ui.common.formatTotal
import app.halftone.ui.common.rememberGroupDownload
import app.halftone.ui.common.songMenu
import app.halftone.ui.theme.MistFaint
import app.halftone.ui.theme.rememberCoverColors
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import app.halftone.R
import app.halftone.Str

@Composable
fun AlbumScreen(albumId: String, navigator: Navigator, padding: PaddingValues) {
    val vm = appViewModel(key = "album-$albumId") { c -> LoadViewModel { c.repository.album(albumId) } }
    val state by vm.state.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        LoadContent(state, onRetry = vm::reload) { album ->
            AlbumContent(album, navigator, padding)
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
private fun AlbumContent(album: AlbumWithSongs, navigator: Navigator, padding: PaddingValues) {
    val container = LocalAppContainer.current
    val playerState by container.player.state.collectAsStateWithLifecycle()
    val starred by container.favorites.ids.collectAsStateWithLifecycle()
    val colors = rememberCoverColors(container.client.coverArtUrl(album.coverArt))
    val download = rememberGroupDownload(album.song)
    val multiDisc = album.song.mapNotNull { it.discNumber }.distinct().size > 1

    Box(Modifier.fillMaxSize()) {
        CoverGlow(colors)

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 24.dp),
        ) {
            item {
                CollectionHeader(
                    coverArt = album.coverArt,
                    title = album.name,
                    subtitle = album.artist,
                    onSubtitleClick = album.artistId?.let { id -> { navigator.artist(id) } },
                    meta = listOfNotNull(
                        album.year?.toString(),
                        album.genre,
                        pluralStringResource(R.plurals.songs_count, album.song.size, album.song.size),
                        formatTotal(album.duration),
                    ).joinToString("  ·  "),
                    colors = colors,
                    onPlay = { container.player.playSongs(album.song) },
                    onShuffle = { container.player.playSongs(album.song.shuffled()) },
                ) {
                    HeartButton(
                        starred = album.id in starred,
                        onToggle = { container.favorites.toggle(album.id, StarKind.Album) },
                        tint = colors.accent,
                    )
                    GroupDownloadButton(
                        state = download.state,
                        progress = download.progress,
                        onDownload = { container.downloads.download(album.song) },
                        onRemove = { container.downloads.remove(album.song.map { it.id }) },
                    )
                    IconButton(onClick = { container.openAddToPlaylist(album.song) }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.PlaylistAdd,
                            stringResource(R.string.add_to_playlist),
                            tint = Color.White.copy(alpha = 0.75f),
                        )
                    }
                    OverflowMenu(
                        listOf(
                            MenuAction(stringResource(R.string.add_album_to_queue), Icons.AutoMirrored.Rounded.QueueMusic) {
                                album.song.forEach(container.player::addToQueue)
                                container.toast(Str.get(R.string.album_added_to_queue))
                            },
                        ),
                        tint = Color.White.copy(alpha = 0.75f),
                    )
                }
            }

            itemsIndexed(album.song, key = { i, s -> "$i-${s.id}" }) { index, song ->
                Column {
                    val previousDisc = album.song.getOrNull(index - 1)?.discNumber
                    if (multiDisc && song.discNumber != previousDisc) {
                        Text(
                            stringResource(R.string.disc_n, song.discNumber ?: 1),
                            style = MaterialTheme.typography.labelSmall,
                            color = MistFaint,
                            modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 6.dp),
                        )
                    }
                    val isCurrent = playerState.current?.mediaId == song.id
                    SongRow(
                        title = song.title,
                        subtitle = song.artist.takeIf { it != album.artist },
                        durationSec = song.duration,
                        isCurrent = isCurrent,
                        songId = song.id,
                        onClick = { container.player.playSongs(album.song, index) },
                        menu = songMenu(song, onOpenArtist = navigator::artist),
                        modifier = Modifier.padding(horizontal = 8.dp),
                    ) {
                        TrackNumber(song.track, isCurrent, playerState.isPlaying)
                    }
                }
            }
        }
    }
}
