package app.halftone.desktop.ui.screens

import androidx.compose.foundation.HorizontalScrollbar
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.halftone.desktop.Screen
import app.halftone.desktop.data.Album
import app.halftone.desktop.data.COVER_LARGE
import app.halftone.desktop.data.HomeData
import app.halftone.desktop.i18n.S
import app.halftone.desktop.ui.common.AlbumCard
import app.halftone.desktop.ui.common.CoverArt
import app.halftone.desktop.ui.common.LoadContent
import app.halftone.desktop.ui.common.LocalApp
import app.halftone.desktop.ui.common.PlaylistCard
import app.halftone.desktop.ui.common.SectionHeader
import app.halftone.desktop.ui.common.pressable
import app.halftone.desktop.ui.common.rememberLoad
import app.halftone.desktop.ui.theme.MistDim
import app.halftone.desktop.ui.theme.rememberCoverColors
import coil3.compose.AsyncImage
import java.time.LocalTime

@Composable
fun HomeScreen() {
    val app = LocalApp.current
    val home = rememberLoad("home") { app.repository.home() }
    LoadContent(home, errorAction = S.listenDownloads to { app.nav.root(Screen.Downloads) }) { data ->
        HomeContent(data)
    }
}

@Composable
private fun HomeContent(data: HomeData) {
    val app = LocalApp.current
    val greeting = remember {
        when (LocalTime.now().hour) {
            in 5..11 -> S.goodMorning
            in 12..17 -> S.goodAfternoon
            in 18..23 -> S.goodEvening
            else -> S.goodNight
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            Column(Modifier.padding(start = 32.dp, end = 32.dp, top = 32.dp, bottom = 20.dp)) {
                Text(greeting, style = MaterialTheme.typography.headlineLarge)
                Text(S.homeSubtitle, style = MaterialTheme.typography.bodyLarge, color = MistDim)
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickAction(S.shuffleMix, Icons.Rounded.Shuffle, app::shuffleMix)
                    QuickAction(S.surpriseMe, Icons.Rounded.AutoAwesome) {
                        (data.discover + data.newest).randomOrNull()?.let { app.playAlbum(it.id) }
                    }
                }
            }
        }
        data.hero?.let { hero -> item { HeroCard(hero) } }
        if (data.playlists.isNotEmpty()) {
            item(key = "playlists") {
                Shelf(S.yourPlaylists) {
                    items(data.playlists, key = { it.id }) { p ->
                        PlaylistCard(p, onClick = { app.nav.playlist(p.id) }, modifier = Modifier.width(196.dp))
                    }
                }
            }
        }
        albumShelf(S.recentlyPlayed, data.recent)
        albumShelf(S.recentlyAdded, data.newest)
        albumShelf(S.mostPlayed, data.frequent)
        albumShelf(S.discover, data.discover)
    }
}

private fun LazyListScope.albumShelf(title: String, albums: List<Album>) {
    if (albums.isEmpty()) return
    item(key = "shelf-$title") {
        val app = LocalApp.current
        Shelf(title) {
            items(albums, key = { it.id }) { a ->
                AlbumCard(a, onClick = { app.nav.album(a.id) }, modifier = Modifier.width(196.dp))
            }
        }
    }
}

@Composable
private fun Shelf(title: String, content: LazyListScope.() -> Unit) {
    val state = rememberLazyListState()
    Column(Modifier.padding(top = 16.dp)) {
        SectionHeader(title)
        LazyRow(state = state, contentPadding = PaddingValues(horizontal = 22.dp), content = content)
        HorizontalScrollbar(
            rememberScrollbarAdapter(state),
            Modifier.padding(horizontal = 32.dp).fillMaxWidth().padding(top = 4.dp),
        )
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.07f)).pressable(onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun HeroCard(album: Album) {
    val app = LocalApp.current
    val url = app.client.coverArtUrl(album.coverArt, COVER_LARGE)
    val colors = rememberCoverColors(app.okHttp, app.client.coverArtUrl(album.coverArt, 64))
    val shape = RoundedCornerShape(26.dp)
    Box(
        Modifier.padding(horizontal = 32.dp).fillMaxWidth().height(240.dp)
            .shadow(24.dp, shape, ambientColor = colors.accent, spotColor = colors.accent)
            .clip(shape).background(colors.backdrop).pressable { app.nav.album(album.id) },
    ) {
        if (url != null) {
            AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(40.dp), alpha = 0.55f)
        }
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(colors.backdropDeep.copy(alpha = 0.2f), colors.backdropDeep.copy(alpha = 0.9f)))))
        Row(Modifier.fillMaxSize().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            CoverArt(album.coverArt, Modifier.size(200.dp).shadow(16.dp, MaterialTheme.shapes.medium), size = COVER_LARGE)
            Spacer(Modifier.width(28.dp))
            Column(Modifier.weight(1f)) {
                Text(S.featured, style = MaterialTheme.typography.labelSmall, color = colors.accent)
                Spacer(Modifier.height(6.dp))
                Text(album.name, style = MaterialTheme.typography.headlineMedium, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(album.artist.orEmpty(), style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.75f), maxLines = 1)
                Spacer(Modifier.height(16.dp))
                Box(
                    Modifier.size(52.dp).clip(CircleShape).background(colors.accent)
                        .pointerHoverIcon(PointerIcon.Hand).clickable { app.playAlbum(album.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.PlayArrow, S.play, tint = Color.Black, modifier = Modifier.size(30.dp))
                }
            }
        }
    }
}
