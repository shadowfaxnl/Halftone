package app.halftone.ui.downloads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.halftone.data.Song
import app.halftone.ui.Navigator
import app.halftone.ui.common.BackButton
import app.halftone.ui.common.CoverArt
import app.halftone.ui.common.EmptyView
import app.halftone.ui.common.LocalAppContainer
import app.halftone.ui.common.SongRow
import app.halftone.ui.common.formatBytes
import app.halftone.ui.common.songMenu
import app.halftone.ui.favorites.SmallAction
import app.halftone.ui.theme.MistDim

/** Losse pagina, bereikbaar vanaf het foutscherm als je offline bent. */
@Composable
fun DownloadsScreen(navigator: Navigator, padding: PaddingValues) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Text(
                "Downloads",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 72.dp, top = 18.dp, bottom = 12.dp),
            )
            DownloadsTab(navigator, padding)
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
fun DownloadsTab(navigator: Navigator, padding: PaddingValues) {
    val container = LocalAppContainer.current
    val items by container.downloads.items.collectAsStateWithLifecycle()
    val bytes by container.downloads.bytesUsed.collectAsStateWithLifecycle()
    val playerState by container.player.state.collectAsStateWithLifecycle()

    val songs: List<Song> = remember(items) {
        items.values
            .filter { it.isDone || it.isActive || it.isFailed }
            .mapNotNull { it.song }
            .sortedWith(
                compareBy<Song>(
                    { it.artist?.lowercase() ?: "" },
                    { it.album?.lowercase() ?: "" },
                    { it.discNumber ?: 1 },
                    { it.track ?: 0 },
                ),
            )
    }
    val ready = remember(songs, items) { songs.filter { items[it.id]?.isDone == true } }
    val activeCount = items.values.count { it.isActive }

    if (songs.isEmpty()) {
        EmptyView(
            Icons.Rounded.DownloadForOffline,
            "Nog niets offline",
            "Tik op het downloadicoon bij een album of afspeellijst, of kies Downloaden in het menu van een nummer.",
        )
        return
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 24.dp),
    ) {
        item(key = "header") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (ready.size == 1) "1 nummer offline" else "${ready.size} nummers offline",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        buildString {
                            append(formatBytes(bytes))
                            if (activeCount > 0) append("  ·  nog $activeCount bezig")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MistDim,
                    )
                }
                if (ready.isNotEmpty()) {
                    SmallAction(Icons.Rounded.PlayArrow, "Afspelen") { container.player.playSongs(ready) }
                    Spacer(Modifier.width(8.dp))
                    SmallAction(Icons.Rounded.Shuffle, "Shuffle") { container.player.playSongs(ready.shuffled()) }
                }
            }
        }
        itemsIndexed(songs, key = { _, s -> s.id }) { _, song ->
            val isReady = items[song.id]?.isDone == true
            SongRow(
                title = song.title,
                subtitle = listOfNotNull(song.artist, song.album).joinToString(" · "),
                durationSec = song.duration,
                isCurrent = playerState.current?.mediaId == song.id,
                songId = song.id,
                onClick = {
                    if (isReady) {
                        container.player.playSongs(ready, ready.indexOfFirst { it.id == song.id }.coerceAtLeast(0))
                    }
                },
                menu = songMenu(song, onOpenAlbum = navigator::album, onOpenArtist = navigator::artist),
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                CoverArt(song.coverArt, Modifier.size(46.dp), shape = MaterialTheme.shapes.small)
            }
        }
    }
}
