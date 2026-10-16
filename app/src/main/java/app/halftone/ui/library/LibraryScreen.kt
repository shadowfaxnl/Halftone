package app.halftone.ui.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.halftone.data.Album
import app.halftone.data.Artist
import app.halftone.ui.Navigator
import app.halftone.ui.common.AlbumCard
import app.halftone.ui.common.ArtistAvatar
import app.halftone.ui.common.LoadContent
import app.halftone.ui.common.LoadViewModel
import app.halftone.ui.common.appViewModel
import app.halftone.ui.downloads.DownloadsTab
import app.halftone.ui.favorites.FavoritesTab
import app.halftone.ui.playlist.PlaylistsTab
import app.halftone.ui.theme.Ink
import app.halftone.ui.theme.MistDim
import app.halftone.ui.theme.MistFaint

private enum class LibraryTab(val label: String) {
    Playlists("Afspeellijsten"),
    Favorites("Favorieten"),
    Artists("Artiesten"),
    Albums("Albums"),
    Downloads("Downloads"),
}
private enum class AlbumSort(val label: String) { Name("A–Z"), Artist("Artiest"), Year("Jaar") }

@Composable
fun LibraryScreen(navigator: Navigator, padding: PaddingValues) {
    var tab by rememberSaveable { mutableStateOf(LibraryTab.Playlists) }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .statusBarsPadding()
                .padding(top = 20.dp, bottom = 12.dp),
        ) {
            Text(
                "Bibliotheek",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.size(16.dp))
            SegmentedTabs(tab, onSelect = { tab = it })
        }
        AnimatedContent(
            targetState = tab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "tab",
        ) { current ->
            when (current) {
                LibraryTab.Playlists -> PlaylistsTab(navigator, padding)
                LibraryTab.Favorites -> FavoritesTab(navigator, padding)
                LibraryTab.Artists -> ArtistsTab(navigator, padding)
                LibraryTab.Albums -> AlbumsTab(navigator, padding)
                LibraryTab.Downloads -> DownloadsTab(navigator, padding)
            }
        }
    }
}

@Composable
private fun SegmentedTabs(selected: LibraryTab, onSelect: (LibraryTab) -> Unit) {
    Row(
        Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.07f))
            .padding(4.dp),
    ) {
        LibraryTab.entries.forEach { tab ->
            val isSelected = tab == selected
            val bg by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                label = "tabBg",
            )
            val fg by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.onPrimary else MistDim,
                label = "tabFg",
            )
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(bg)
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                Text(tab.label, style = MaterialTheme.typography.labelLarge, color = fg)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArtistsTab(navigator: Navigator, padding: PaddingValues) {
    val vm = appViewModel(key = "artists") { c -> LoadViewModel { c.repository.artists() } }
    val state by vm.state.collectAsStateWithLifecycle()

    LoadContent(state, onRetry = vm::reload) { artists ->
        val grouped = remember(artists) {
            artists
                .sortedBy { it.name.lowercase() }
                .groupBy { a -> a.name.firstOrNull()?.uppercaseChar()?.takeIf { it.isLetter() } ?: '#' }
                .toSortedMap(compareBy<Char> { if (it == '#') '~' else it })
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 24.dp),
        ) {
            grouped.forEach { (letter, group) ->
                stickyHeader(key = "h-$letter") {
                    Text(
                        letter.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Ink.copy(alpha = 0.92f))
                            .padding(horizontal = 24.dp, vertical = 6.dp),
                    )
                }
                items(group, key = { it.id }) { artist ->
                    ArtistRow(artist) { navigator.artist(artist.id) }
                }
            }
        }
    }
}

@Composable
private fun ArtistRow(artist: Artist, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtistAvatar(artist.coverArt, Modifier.size(54.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                artist.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (artist.albumCount == 1) "1 album" else "${artist.albumCount} albums",
                style = MaterialTheme.typography.bodySmall,
                color = MistFaint,
            )
        }
    }
}

@Composable
private fun AlbumsTab(navigator: Navigator, padding: PaddingValues) {
    val vm = appViewModel(key = "all-albums") { c -> LoadViewModel { c.repository.allAlbums() } }
    val state by vm.state.collectAsStateWithLifecycle()
    var sort by rememberSaveable { mutableStateOf(AlbumSort.Name) }

    LoadContent(state, onRetry = vm::reload) { albums ->
        val sorted = remember(albums, sort) { albums.sortedWith(sort.comparator()) }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AlbumSort.entries) { option ->
                        FilterChip(
                            selected = sort == option,
                            onClick = { sort = option },
                            label = { Text(option.label) },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }
            }
            items(sorted, key = { it.id }) { album ->
                AlbumCard(album, onClick = { navigator.album(album.id) })
            }
        }
    }
}

private fun AlbumSort.comparator(): Comparator<Album> = when (this) {
    AlbumSort.Name -> compareBy<Album> { it.name.lowercase() }
    AlbumSort.Artist -> compareBy<Album> { it.artist?.lowercase() ?: "" }.thenBy { it.year ?: 0 }
    AlbumSort.Year -> compareByDescending<Album> { it.year ?: 0 }.thenBy { it.name.lowercase() }
}
