package app.halftone.ui.artist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.halftone.data.ArtistWithAlbums
import app.halftone.data.COVER_LARGE
import app.halftone.ui.Navigator
import app.halftone.ui.common.AlbumCard
import app.halftone.data.StarKind
import app.halftone.ui.common.BackButton
import app.halftone.ui.common.HeartButton
import app.halftone.ui.common.LoadContent
import app.halftone.ui.common.bleed
import app.halftone.ui.common.LoadViewModel
import app.halftone.ui.common.LocalAppContainer
import app.halftone.ui.common.appViewModel
import app.halftone.ui.theme.Ink
import app.halftone.ui.theme.MistDim
import app.halftone.ui.theme.rememberCoverColors
import coil3.compose.AsyncImage

@Composable
fun ArtistScreen(artistId: String, navigator: Navigator, padding: PaddingValues) {
    val vm = appViewModel(key = "artist-$artistId") { c -> LoadViewModel { c.repository.artist(artistId) } }
    val state by vm.state.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        LoadContent(state, onRetry = vm::reload) { artist ->
            ArtistContent(artist, navigator, padding)
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
private fun ArtistContent(artist: ArtistWithAlbums, navigator: Navigator, padding: PaddingValues) {
    val container = LocalAppContainer.current
    val starred by container.favorites.ids.collectAsStateWithLifecycle()
    val albums = artist.album.sortedByDescending { it.year ?: 0 }
    val imageId = artist.coverArt ?: albums.firstOrNull()?.coverArt
    val colors = rememberCoverColors(container.client.coverArtUrl(imageId))
    val heroUrl = container.client.coverArtUrl(imageId, COVER_LARGE)

    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = padding.calculateBottomPadding() + 24.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Box(
                Modifier
                    .bleed(16.dp)
                    .height(380.dp)
                    .background(colors.backdrop),
            ) {
                if (heroUrl != null) {
                    AsyncImage(
                        model = heroUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(6.dp),
                        alpha = 0.85f,
                    )
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.45f to colors.backdropDeep.copy(alpha = 0.35f),
                                1f to Ink,
                            ),
                        ),
                )
                Column(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                ) {
                    Text(
                        artist.name,
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        maxLines = 2,
                    )
                    Text(
                        if (artist.albumCount == 1) "1 album" else "${artist.albumCount} albums",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MistDim,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(
                            onClick = { container.actions.playArtist(artist.id) },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accent,
                                contentColor = Color.Black,
                            ),
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Afspelen")
                        }
                        FilledTonalButton(
                            onClick = { container.actions.playArtist(artist.id, shuffle = true) },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.White.copy(alpha = 0.12f),
                                contentColor = Color.White,
                            ),
                        ) {
                            Icon(Icons.Rounded.Shuffle, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Shuffle")
                        }
                        HeartButton(
                            starred = artist.id in starred,
                            onToggle = { container.favorites.toggle(artist.id, StarKind.Artist) },
                            tint = colors.accent,
                        )
                    }
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                "Albums",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp, top = 16.dp),
            )
        }
        items(albums, key = { it.id }) { album ->
            AlbumCard(
                album,
                onClick = { navigator.album(album.id) },
                showArtist = false,
            )
        }
    }
}
