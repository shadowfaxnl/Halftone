package app.halftone.desktop.ui.screens

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.halftone.desktop.data.ArtistWithAlbums
import app.halftone.desktop.data.COVER_LARGE
import app.halftone.desktop.data.StarKind
import app.halftone.desktop.i18n.S
import app.halftone.desktop.ui.common.AlbumCard
import app.halftone.desktop.ui.common.HeartButton
import app.halftone.desktop.ui.common.LoadContent
import app.halftone.desktop.ui.common.LocalApp
import app.halftone.desktop.ui.common.observe
import app.halftone.desktop.ui.common.rememberLoad
import app.halftone.desktop.ui.theme.Ink
import app.halftone.desktop.ui.theme.MistDim
import app.halftone.desktop.ui.theme.rememberCoverColors
import coil3.compose.AsyncImage

@Composable
fun ArtistScreen(id: String) {
    val app = LocalApp.current
    val load = rememberLoad("artist-$id") { app.repository.artist(id) }
    LoadContent(load) { ArtistContent(it) }
}

@Composable
private fun ArtistContent(artist: ArtistWithAlbums) {
    val app = LocalApp.current
    val starred = app.favorites.ids.observe()
    val albums = artist.album.sortedByDescending { it.year ?: 0 }
    val imageId = artist.coverArt ?: albums.firstOrNull()?.coverArt
    val colors = rememberCoverColors(app.okHttp, app.client.coverArtUrl(imageId, 64))
    val heroUrl = app.client.coverArtUrl(imageId, COVER_LARGE)

    LazyVerticalGrid(
        columns = GridCells.Adaptive(196.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 32.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Box(Modifier.fillMaxWidth().height(320.dp).padding(horizontal = 0.dp).background(colors.backdrop)) {
                if (heroUrl != null) {
                    AsyncImage(heroUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(12.dp), alpha = 0.8f)
                }
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Transparent, 0.5f to colors.backdropDeep.copy(alpha = 0.4f), 1f to Ink)))
                Column(Modifier.align(Alignment.BottomStart).padding(24.dp)) {
                    Text(S.kindArtist.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                    Text(artist.name, style = MaterialTheme.typography.displayMedium, color = Color.White, maxLines = 2)
                    Text(S.albumsCount(artist.albumCount), style = MaterialTheme.typography.bodyMedium, color = MistDim)
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { app.playArtist(artist.id) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.Black),
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null)
                            Spacer(Modifier.width(6.dp))
                            Text(S.play)
                        }
                        FilledTonalButton(
                            onClick = { app.playArtist(artist.id, shuffle = true) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color.White.copy(alpha = 0.12f), contentColor = Color.White),
                        ) {
                            Icon(Icons.Rounded.Shuffle, null)
                            Spacer(Modifier.width(6.dp))
                            Text(S.shuffle)
                        }
                        HeartButton(artist.id in starred, { app.favorites.toggle(artist.id, StarKind.Artist) }, tint = colors.accent)
                    }
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(S.albums, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 10.dp, top = 20.dp, bottom = 4.dp))
        }
        items(albums, key = { it.id }) { a ->
            AlbumCard(a, onClick = { app.nav.album(a.id) }, showArtist = false)
        }
    }
}
