package app.halftone.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.halftone.data.Starred
import app.halftone.ui.Navigator
import app.halftone.ui.common.AlbumCard
import app.halftone.ui.common.ArtistAvatar
import app.halftone.ui.common.CoverArt
import app.halftone.ui.common.EmptyView
import app.halftone.ui.common.LoadContent
import app.halftone.ui.common.LoadViewModel
import app.halftone.ui.common.LocalAppContainer
import app.halftone.ui.common.SectionHeader
import app.halftone.ui.common.SongRow
import app.halftone.ui.common.appViewModel
import app.halftone.ui.common.pressable
import app.halftone.ui.common.songMenu
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import androidx.compose.ui.res.stringResource
import app.halftone.R

@OptIn(FlowPreview::class)
@Composable
fun FavoritesTab(navigator: Navigator, padding: PaddingValues) {
    val vm = appViewModel(key = "favorites") { c ->
        // Opnieuw laden als je ergens een hartje aan- of uitzet.
        LoadViewModel(refreshOn = c.favorites.ids.debounce(600)) { c.favorites.load() }
    }
    val state by vm.state.collectAsStateWithLifecycle()

    LoadContent(state, onRetry = vm::reload) { starred ->
        if (starred.isEmpty) {
            EmptyView(
                Icons.Rounded.FavoriteBorder,
                stringResource(R.string.favorites_empty_title),
                stringResource(R.string.favorites_empty_text),
            )
        } else {
            FavoritesContent(starred, navigator, padding)
        }
    }
}

@Composable
private fun FavoritesContent(starred: Starred, navigator: Navigator, padding: PaddingValues) {
    val container = LocalAppContainer.current
    val playerState by container.player.state.collectAsStateWithLifecycle()
    val songs = starred.song

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 24.dp),
    ) {
        if (starred.artist.isNotEmpty()) {
            item(key = "artists") {
                Column {
                    SectionHeader(stringResource(R.string.artists))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        items(starred.artist, key = { it.id }) { artist ->
                            Column(
                                Modifier
                                    .width(96.dp)
                                    .pressable { navigator.artist(artist.id) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                ArtistAvatar(artist.coverArt, Modifier.size(96.dp))
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    artist.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
        if (starred.album.isNotEmpty()) {
            item(key = "albums") {
                Column {
                    Spacer(Modifier.height(8.dp))
                    SectionHeader(stringResource(R.string.albums))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(starred.album, key = { it.id }) { album ->
                            AlbumCard(album, onClick = { navigator.album(album.id) }, modifier = Modifier.width(140.dp))
                        }
                    }
                }
            }
        }
        if (songs.isNotEmpty()) {
            item(key = "songs-header") {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SectionHeader(stringResource(R.string.songs), Modifier.weight(1f))
                        SmallAction(Icons.Rounded.PlayArrow, stringResource(R.string.play)) { container.player.playSongs(songs) }
                        Spacer(Modifier.width(8.dp))
                        SmallAction(Icons.Rounded.Shuffle, stringResource(R.string.shuffle)) { container.player.playSongs(songs.shuffled()) }
                    }
                }
            }
            itemsIndexed(songs, key = { i, s -> "song-$i-${s.id}" }) { index, song ->
                SongRow(
                    title = song.title,
                    subtitle = listOfNotNull(song.artist, song.album).joinToString(" · "),
                    durationSec = song.duration,
                    isCurrent = playerState.current?.mediaId == song.id,
                    songId = song.id,
                    onClick = { container.player.playSongs(songs, index) },
                    menu = songMenu(song, onOpenAlbum = navigator::album, onOpenArtist = navigator::artist),
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    CoverArt(song.coverArt, Modifier.size(46.dp), shape = MaterialTheme.shapes.small)
                }
            }
        }
    }
}

@Composable
internal fun SmallAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            contentColor = MaterialTheme.colorScheme.primary,
        ),
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
