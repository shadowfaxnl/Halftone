package app.halftone.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.halftone.desktop.data.Album
import app.halftone.desktop.data.Song
import app.halftone.desktop.i18n.S
import app.halftone.desktop.ui.common.AlbumCard
import app.halftone.desktop.ui.common.ArtistAvatar
import app.halftone.desktop.ui.common.CoverArt
import app.halftone.desktop.ui.common.EmptyView
import app.halftone.desktop.ui.common.LoadContent
import app.halftone.desktop.ui.common.LocalApp
import app.halftone.desktop.ui.common.PlaylistCard
import app.halftone.desktop.ui.common.ScreenTitle
import app.halftone.desktop.ui.common.SectionHeader
import app.halftone.desktop.ui.common.SmallAction
import app.halftone.desktop.ui.common.SongRow
import app.halftone.desktop.ui.common.formatBytes
import app.halftone.desktop.ui.common.observe
import app.halftone.desktop.ui.common.pressable
import app.halftone.desktop.ui.common.rememberLoad
import app.halftone.desktop.ui.common.songMenu
import app.halftone.desktop.ui.theme.MistDim
import app.halftone.desktop.ui.theme.MistFaint

private val GridPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 32.dp)

// ───────────── Afspeellijsten ─────────────

@Composable
fun PlaylistsScreen() {
    val app = LocalApp.current
    val version = app.repository.playlistVersion.observe()
    val load = rememberLoad("playlists", refreshKey = version) { app.repository.playlists() }
    var creating by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle(S.playlists)
        LoadContent(load) { playlists ->
            LazyVerticalGrid(GridCells.Adaptive(196.dp), Modifier.fillMaxSize(), contentPadding = GridPadding) {
                item(key = "new") { NewPlaylistCard { creating = true } }
                items(playlists, key = { it.id }) { p -> PlaylistCard(p, onClick = { app.nav.playlist(p.id) }) }
            }
        }
    }
    if (creating) {
        PlaylistNameDialog(S.newPlaylist, S.create, onConfirm = { name ->
            creating = false
            app.launchSafely { app.repository.createPlaylist(name) }
        }, onDismiss = { creating = false })
    }
}

@Composable
private fun NewPlaylistCard(onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Column(Modifier.padding(10.dp).pressable(onClick)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.medium).background(accent.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(50)).background(accent), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(30.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(S.newPlaylist, style = MaterialTheme.typography.titleSmall)
        Text(S.startCollection, style = MaterialTheme.typography.bodySmall, color = MistDim)
    }
}

// ───────────── Favorieten ─────────────

@Composable
fun FavoritesScreen() {
    val app = LocalApp.current
    val ids = app.favorites.ids.observe()
    val load = rememberLoad("favorites", refreshKey = ids) { app.favorites.load() }
    val ps = app.player.state.observe()

    Column(Modifier.fillMaxSize()) {
        ScreenTitle(S.favorites)
        LoadContent(load) { starred ->
            if (starred.isEmpty) {
                EmptyView(Icons.Rounded.FavoriteBorder, S.favoritesEmptyTitle, S.favoritesEmptyText)
                return@LoadContent
            }
            LazyVerticalGrid(GridCells.Adaptive(196.dp), Modifier.fillMaxSize(), contentPadding = GridPadding) {
                if (starred.artist.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { SectionHeader(S.artists, Modifier.padding(start = 0.dp)) }
                    items(starred.artist, key = { "ar-" + it.id }) { a ->
                        Column(Modifier.padding(10.dp).pressable { app.nav.artist(a.id) }, horizontalAlignment = Alignment.CenterHorizontally) {
                            ArtistAvatar(a.coverArt, Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            Text(a.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                if (starred.album.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { SectionHeader(S.albums) }
                    items(starred.album, key = { "al-" + it.id }) { a -> AlbumCard(a, onClick = { app.nav.album(a.id) }) }
                }
                if (starred.song.isNotEmpty()) {
                    val songs = starred.song
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 10.dp)) {
                            SectionHeader(S.songs, Modifier.weight(1f))
                            SmallAction(Icons.Rounded.PlayArrow, S.play) { app.play(songs) }
                            Spacer(Modifier.width(8.dp))
                            SmallAction(Icons.Rounded.Shuffle, S.shuffle) { app.play(songs.shuffled()) }
                        }
                    }
                    songs.forEachIndexed { index, song ->
                        item(key = "so-$index-${song.id}", span = { GridItemSpan(maxLineSpan) }) {
                            SongRow(
                                song = song,
                                isCurrent = ps.current?.id == song.id,
                                subtitle = listOfNotNull(song.artist, song.album).joinToString(" · "),
                                onClick = { app.play(songs, index) },
                                menu = songMenu(song),
                            ) { CoverArt(song.coverArt, Modifier.size(40.dp), shape = MaterialTheme.shapes.small) }
                        }
                    }
                }
            }
        }
    }
}

// ───────────── Artiesten ─────────────

@Composable
fun ArtistsScreen() {
    val app = LocalApp.current
    val load = rememberLoad("artists") { app.repository.artists() }
    Column(Modifier.fillMaxSize()) {
        ScreenTitle(S.artists)
        LoadContent(load) { artists ->
            val grouped = remember(artists) {
                artists.sortedBy { it.name.lowercase() }
                    .groupBy { a -> a.name.firstOrNull()?.uppercaseChar()?.takeIf { it.isLetter() } ?: '#' }
                    .toSortedMap(compareBy<Char> { if (it == '#') '~' else it })
            }
            LazyVerticalGrid(GridCells.Adaptive(160.dp), Modifier.fillMaxSize(), contentPadding = GridPadding) {
                grouped.forEach { (letter, group) ->
                    item(key = "h-$letter", span = { GridItemSpan(maxLineSpan) }) {
                        Text(letter.toString(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 10.dp, top = 16.dp, bottom = 4.dp))
                    }
                    items(group, key = { it.id }) { a ->
                        Column(Modifier.padding(10.dp).pressable { app.nav.artist(a.id) }, horizontalAlignment = Alignment.CenterHorizontally) {
                            ArtistAvatar(a.coverArt, Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            Text(a.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                            Text(S.albumsCount(a.albumCount), style = MaterialTheme.typography.bodySmall, color = MistFaint)
                        }
                    }
                }
            }
        }
    }
}

// ───────────── Albums ─────────────

private enum class AlbumSort { Name, Artist, Year }

@Composable
fun AlbumsScreen() {
    val app = LocalApp.current
    val load = rememberLoad("all-albums") { app.repository.allAlbums() }
    var sort by remember { mutableStateOf(AlbumSort.Name) }
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScreenTitle(S.albums, Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(end = 32.dp, top = 12.dp)) {
                listOf(AlbumSort.Name to S.sortAz, AlbumSort.Artist to S.sortArtist, AlbumSort.Year to S.sortYear).forEach { (opt, label) ->
                    FilterChip(
                        selected = sort == opt,
                        onClick = { sort = opt },
                        label = { Text(label) },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
        }
        LoadContent(load) { albums ->
            val sorted = remember(albums, sort) {
                when (sort) {
                    AlbumSort.Name -> albums.sortedBy { it.name.lowercase() }
                    AlbumSort.Artist -> albums.sortedWith(compareBy<Album>({ it.artist?.lowercase() ?: "" }, { it.year ?: 0 }))
                    AlbumSort.Year -> albums.sortedWith(compareByDescending<Album> { it.year ?: 0 }.thenBy { it.name.lowercase() })
                }
            }
            LazyVerticalGrid(GridCells.Adaptive(196.dp), Modifier.fillMaxSize(), contentPadding = GridPadding) {
                items(sorted, key = { it.id }) { a -> AlbumCard(a, onClick = { app.nav.album(a.id) }) }
            }
        }
    }
}

// ───────────── Downloads ─────────────

@Composable
fun DownloadsScreen() {
    val app = LocalApp.current
    val items = app.downloads.items.observe()
    val bytes = app.downloads.bytesUsed.observe()
    val ps = app.player.state.observe()
    val songs: List<Song> = remember(items) {
        items.values.map { it.song }.sortedWith(
            compareBy<Song>({ it.artist?.lowercase() ?: "" }, { it.album?.lowercase() ?: "" }, { it.discNumber ?: 1 }, { it.track ?: 0 }),
        )
    }
    val ready = remember(songs, items) { songs.filter { items[it.id]?.isDone == true } }
    val active = items.values.count { it.isActive }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                ScreenTitle(S.downloads, Modifier.padding(bottom = 0.dp))
                if (songs.isNotEmpty()) {
                    val info = buildString {
                        append(S.songsOffline(ready.size)).append("  ·  ").append(formatBytes(bytes))
                        if (active > 0) append("  ·  ").append(S.downloadsInProgress(active))
                    }
                    Text(info, style = MaterialTheme.typography.bodyMedium, color = MistDim, modifier = Modifier.padding(start = 32.dp, bottom = 12.dp))
                }
            }
            if (ready.isNotEmpty()) {
                Row(Modifier.padding(end = 32.dp)) {
                    SmallAction(Icons.Rounded.PlayArrow, S.play) { app.play(ready) }
                    Spacer(Modifier.width(8.dp))
                    SmallAction(Icons.Rounded.Shuffle, S.shuffle) { app.play(ready.shuffled()) }
                }
            }
        }
        if (songs.isEmpty()) {
            EmptyView(Icons.Rounded.DownloadForOffline, S.downloadsEmptyTitle, S.downloadsEmptyText)
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp)) {
            itemsIndexed(songs, key = { _, s -> s.id }) { _, song ->
                val isReady = items[song.id]?.isDone == true
                SongRow(
                    song = song,
                    isCurrent = ps.current?.id == song.id,
                    subtitle = listOfNotNull(song.artist, song.album).joinToString(" · "),
                    onClick = { if (isReady) app.play(ready, ready.indexOfFirst { it.id == song.id }.coerceAtLeast(0)) },
                    menu = songMenu(song),
                    modifier = Modifier.background(Color.Transparent),
                ) { CoverArt(song.coverArt, Modifier.size(40.dp), shape = MaterialTheme.shapes.small) }
            }
        }
    }
}
