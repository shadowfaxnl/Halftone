package app.halftone.desktop.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.halftone.desktop.data.AlbumWithSongs
import app.halftone.desktop.data.StarKind
import app.halftone.desktop.i18n.S
import app.halftone.desktop.ui.common.CollectionHeader
import app.halftone.desktop.ui.common.CoverGlow
import app.halftone.desktop.ui.common.GroupDownloadButton
import app.halftone.desktop.ui.common.HeartButton
import app.halftone.desktop.ui.common.LoadContent
import app.halftone.desktop.ui.common.LocalApp
import app.halftone.desktop.ui.common.MenuAction
import app.halftone.desktop.ui.common.OverflowMenu
import app.halftone.desktop.ui.common.SongRow
import app.halftone.desktop.ui.common.TrackNumber
import app.halftone.desktop.ui.common.formatTotal
import app.halftone.desktop.ui.common.observe
import app.halftone.desktop.ui.common.rememberLoad
import app.halftone.desktop.ui.common.songMenu
import app.halftone.desktop.ui.theme.MistFaint
import app.halftone.desktop.ui.theme.rememberCoverColors

@Composable
fun AlbumScreen(id: String) {
    val app = LocalApp.current
    val load = rememberLoad("album-$id") { app.repository.album(id) }
    LoadContent(load) { AlbumContent(it) }
}

@Composable
private fun AlbumContent(album: AlbumWithSongs) {
    val app = LocalApp.current
    val ps = app.player.state.observe()
    val starred = app.favorites.ids.observe()
    val colors = rememberCoverColors(app.okHttp, app.client.coverArtUrl(album.coverArt, 64))
    val multiDisc = album.song.mapNotNull { it.discNumber }.distinct().size > 1

    Box(Modifier.fillMaxSize()) {
        CoverGlow(colors)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                CollectionHeader(
                    coverArt = album.coverArt,
                    kind = S.kindAlbum,
                    title = album.name,
                    subtitle = album.artist,
                    onSubtitleClick = album.artistId?.let { aid -> { app.nav.artist(aid) } },
                    meta = listOfNotNull(album.year?.toString(), album.genre, S.songsCount(album.song.size), formatTotal(album.duration))
                        .joinToString("  ·  "),
                    colors = colors,
                    onPlay = { app.play(album.song) },
                    onShuffle = { app.play(album.song.shuffled()) },
                ) {
                    HeartButton(album.id in starred, { app.favorites.toggle(album.id, StarKind.Album) }, tint = colors.accent)
                    GroupDownloadButton(album.song)
                    IconButton(onClick = { app.addToPlaylist = album.song }) {
                        Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, S.addToPlaylist, tint = Color.White.copy(alpha = 0.75f))
                    }
                    OverflowMenu(
                        listOf(
                            MenuAction(S.addAlbumToQueue, Icons.AutoMirrored.Rounded.QueueMusic) {
                                app.player.addAllToQueue(album.song)
                                app.toast(S.albumAddedToQueue)
                            },
                        ),
                        tint = Color.White.copy(alpha = 0.75f),
                    )
                }
            }
            itemsIndexed(album.song, key = { i, s -> "$i-${s.id}" }) { index, song ->
                Column(Modifier.padding(horizontal = 20.dp)) {
                    val prevDisc = album.song.getOrNull(index - 1)?.discNumber
                    if (multiDisc && song.discNumber != prevDisc) {
                        Text(S.disc(song.discNumber ?: 1), style = MaterialTheme.typography.labelSmall, color = MistFaint, modifier = Modifier.padding(start = 14.dp, top = 16.dp, bottom = 6.dp))
                    }
                    val isCurrent = ps.current?.id == song.id
                    SongRow(
                        song = song,
                        isCurrent = isCurrent,
                        subtitle = song.artist.takeIf { it != album.artist },
                        onClick = { app.play(album.song, index) },
                        menu = songMenu(song),
                    ) { TrackNumber(song.track, isCurrent, ps.isPlaying) }
                }
            }
        }
    }
}
