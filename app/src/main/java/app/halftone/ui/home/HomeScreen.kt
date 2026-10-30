package app.halftone.ui.home

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.halftone.data.Album
import app.halftone.data.COVER_LARGE
import app.halftone.data.HomeData
import app.halftone.ui.Navigator
import app.halftone.ui.common.AlbumCard
import app.halftone.ui.common.CoverArt
import app.halftone.ui.common.ErrorAction
import app.halftone.ui.common.HalftoneMark
import app.halftone.ui.common.PlaylistCard
import app.halftone.ui.common.LoadContent
import app.halftone.ui.common.LoadViewModel
import app.halftone.ui.common.LocalAppContainer
import app.halftone.ui.common.SectionHeader
import app.halftone.ui.common.appViewModel
import app.halftone.ui.common.pressable
import app.halftone.ui.theme.MistDim
import app.halftone.ui.theme.rememberCoverColors
import coil3.compose.AsyncImage
import java.time.LocalTime
import androidx.compose.ui.res.stringResource
import app.halftone.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navigator: Navigator, padding: PaddingValues) {
    val vm = appViewModel(key = "home") { c -> LoadViewModel { c.repository.home() } }
    val state by vm.state.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = vm::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LoadContent(
            state,
            onRetry = vm::reload,
            errorAction = ErrorAction(stringResource(R.string.listen_downloads), navigator::downloads),
        ) { data ->
            HomeContent(data, navigator, padding)
        }
    }
}

@Composable
private fun HomeContent(data: HomeData, navigator: Navigator, padding: PaddingValues) {
    val container = LocalAppContainer.current
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 24.dp),
    ) {
        item { Header(onSettings = navigator::settings) }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                QuickAction(
                    stringResource(R.string.shuffle_mix),
                    Icons.Rounded.Shuffle,
                    Modifier.weight(1f),
                    onClick = container.actions::shuffleMix,
                )
                QuickAction(
                    stringResource(R.string.surprise_me),
                    Icons.Rounded.AutoAwesome,
                    Modifier.weight(1f),
                    onClick = {
                        val pick = (data.discover + data.newest).randomOrNull()
                        if (pick != null) container.actions.playAlbum(pick.id)
                    },
                )
            }
        }
        data.hero?.let { hero ->
            item {
                Column {
                    Spacer(Modifier.height(24.dp))
                    HeroCard(
                        album = hero,
                        onOpen = { navigator.album(hero.id) },
                        onPlay = { container.actions.playAlbum(hero.id) },
                    )
                }
            }
        }
        if (data.playlists.isNotEmpty()) {
            item(key = "playlists") {
                Column {
                    Spacer(Modifier.height(20.dp))
                    SectionHeader(stringResource(R.string.your_playlists))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(data.playlists, key = { it.id }) { playlist ->
                            PlaylistCard(
                                playlist,
                                onClick = { navigator.playlist(playlist.id) },
                                modifier = Modifier.width(152.dp),
                            )
                        }
                    }
                }
            }
        }
        albumShelf(R.string.recently_played, data.recent, navigator)
        albumShelf(R.string.recently_added, data.newest, navigator)
        albumShelf(R.string.most_played, data.frequent, navigator)
        albumShelf(R.string.discover, data.discover, navigator)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.albumShelf(
    title: Int,
    albums: List<Album>,
    navigator: Navigator,
) {
    if (albums.isEmpty()) return
    item(key = "shelf-$title") {
        Column {
            Spacer(Modifier.height(20.dp))
            SectionHeader(stringResource(title))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(albums, key = { it.id }) { album ->
                    AlbumCard(album, onClick = { navigator.album(album.id) }, modifier = Modifier.width(152.dp))
                }
            }
        }
    }
}

@Composable
private fun Header(onSettings: () -> Unit) {
    val greetingRes = remember {
        when (LocalTime.now().hour) {
            in 5..11 -> R.string.good_morning
            in 12..17 -> R.string.good_afternoon
            in 18..23 -> R.string.good_evening
            else -> R.string.good_night
        }
    }
    val greeting = stringResource(greetingRes)

    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 8.dp, top = 20.dp, bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HalftoneMark(size = 44.dp, tile = true)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(greeting, style = MaterialTheme.typography.headlineLarge)
            Text(
                stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MistDim,
            )
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings))
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .pressable(onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.titleSmall)
    }
}

/** Een uitgelicht album: wazig hoesje als achtergrond, scherp hoesje ervoor. */
@Composable
private fun HeroCard(album: Album, onOpen: () -> Unit, onPlay: () -> Unit) {
    val client = LocalAppContainer.current.client
    val url = client.coverArtUrl(album.coverArt, COVER_LARGE)
    val colors = rememberCoverColors(client.coverArtUrl(album.coverArt))
    val shape = RoundedCornerShape(28.dp)

    Box(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .height(212.dp)
            .shadow(24.dp, shape, ambientColor = colors.accent, spotColor = colors.accent)
            .clip(shape)
            .background(colors.backdrop)
            .pressable(onOpen),
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(36.dp),
                alpha = 0.55f,
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(colors.backdropDeep.copy(alpha = 0.2f), colors.backdropDeep.copy(alpha = 0.85f)),
                    ),
                ),
        )
        Row(
            Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverArt(
                album.coverArt,
                Modifier
                    .size(160.dp)
                    .shadow(16.dp, MaterialTheme.shapes.medium),
                size = COVER_LARGE,
            )
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.featured),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.accent,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    album.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White,
                )
                Text(
                    album.artist.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colors.accent)
                        .clickable(onClick = onPlay),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.PlayArrow, stringResource(R.string.play), tint = Color.Black, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}
