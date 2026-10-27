package app.halftone.desktop.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.halftone.desktop.Screen
import app.halftone.desktop.i18n.S
import app.halftone.desktop.ui.common.HalftoneMark
import app.halftone.desktop.ui.common.LoadContent
import app.halftone.desktop.ui.common.LocalApp
import app.halftone.desktop.ui.common.observe
import app.halftone.desktop.ui.common.rememberLoad
import app.halftone.desktop.ui.screens.AddToPlaylistDialog
import app.halftone.desktop.ui.screens.AlbumScreen
import app.halftone.desktop.ui.screens.AlbumsScreen
import app.halftone.desktop.ui.screens.ArtistScreen
import app.halftone.desktop.ui.screens.ArtistsScreen
import app.halftone.desktop.ui.screens.DownloadsScreen
import app.halftone.desktop.ui.screens.FavoritesScreen
import app.halftone.desktop.ui.screens.HomeScreen
import app.halftone.desktop.ui.screens.PlaylistScreen
import app.halftone.desktop.ui.screens.PlaylistsScreen
import app.halftone.desktop.ui.screens.SearchScreen
import app.halftone.desktop.ui.screens.SettingsScreen
import app.halftone.desktop.ui.theme.CoverColors
import app.halftone.desktop.ui.theme.Ink
import app.halftone.desktop.ui.theme.Ink2
import app.halftone.desktop.ui.theme.Ink3
import app.halftone.desktop.ui.theme.Line
import app.halftone.desktop.ui.theme.MistDim
import app.halftone.desktop.ui.theme.MistFaint

/** Het hoofdvenster na inloggen: zijbalk, inhoud, wachtrij en spelerbalk. */
@Composable
fun Shell(colors: CoverColors) {
    val app = LocalApp.current
    val ps = app.player.state.observe()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { app.toasts.collect { snackbar.showSnackbar(it) } }
    val backdrop by animateColorAsState(colors.backdrop, tween(900), label = "backdrop")

    Box(Modifier.fillMaxSize().background(Ink)) {
        Column(Modifier.fillMaxSize()) {
            if (!app.player.available) MpvMissingBanner()
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Sidebar()
                Box(
                    Modifier.weight(1f).fillMaxHeight().background(
                        Brush.verticalGradient(0f to backdrop.copy(alpha = if (ps.current != null) 0.55f else 0.3f), 0.4f to Ink),
                    ),
                ) {
                    AnimatedContent(app.nav.current, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) }, label = "screen") { screen ->
                        when (screen) {
                            Screen.Home -> HomeScreen()
                            Screen.Search -> SearchScreen()
                            Screen.Playlists -> PlaylistsScreen()
                            Screen.Favorites -> FavoritesScreen()
                            Screen.Artists -> ArtistsScreen()
                            Screen.Albums -> AlbumsScreen()
                            Screen.Downloads -> DownloadsScreen()
                            Screen.Settings -> SettingsScreen()
                            is Screen.Album -> AlbumScreen(screen.id)
                            is Screen.Artist -> ArtistScreen(screen.id)
                            is Screen.Playlist -> PlaylistScreen(screen.id)
                        }
                    }
                    if (app.nav.canGoBack) {
                        IconButton(
                            onClick = app.nav::back,
                            modifier = Modifier.padding(12.dp).align(Alignment.TopEnd).clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.35f)).pointerHoverIcon(PointerIcon.Hand),
                        ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, S.back, tint = Color.White) }
                    }
                }
                AnimatedVisibility(app.queueOpen, enter = expandHorizontally() + fadeIn(), exit = shrinkHorizontally() + fadeOut()) {
                    Row {
                        Box(Modifier.width(1.dp).fillMaxHeight().background(Line))
                        QueuePanel()
                    }
                }
            }
            PlayerBar()
        }

        AnimatedVisibility(
            app.nowPlayingOpen && ps.current != null,
            enter = slideInVertically(tween(380)) { it } + fadeIn(tween(260)),
            exit = slideOutVertically(tween(320)) { it } + fadeOut(tween(260)),
            modifier = Modifier.fillMaxSize().padding(bottom = 89.dp),
        ) { NowPlaying(colors) }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 104.dp)) { data ->
            Snackbar(data, containerColor = Ink3, contentColor = MaterialTheme.colorScheme.onSurface, shape = RoundedCornerShape(12.dp))
        }

        app.addToPlaylist?.let { songs -> AddToPlaylistDialog(songs, onDismiss = { app.addToPlaylist = null }) }
    }
}

@Composable
private fun Sidebar() {
    val app = LocalApp.current
    val current = app.nav.current
    val version = app.repository.playlistVersion.observe()
    val playlists = rememberLoad("sidebar-playlists", refreshKey = version) { app.repository.playlists() }

    Column(Modifier.width(232.dp).fillMaxHeight().background(Ink2).padding(vertical = 18.dp)) {
        Row(Modifier.padding(start = 20.dp, bottom = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            HalftoneMark(size = 30.dp, tile = true)
            Spacer(Modifier.width(10.dp))
            Text("halftone", style = MaterialTheme.typography.headlineSmall)
        }
        NavItem(Icons.Rounded.Home, S.home, current == Screen.Home) { app.nav.root(Screen.Home) }
        NavItem(Icons.Rounded.Search, S.search, current == Screen.Search) { app.nav.root(Screen.Search) }
        Text(S.library.uppercase(), style = MaterialTheme.typography.labelSmall, color = MistFaint, modifier = Modifier.padding(start = 24.dp, top = 22.dp, bottom = 8.dp))
        NavItem(Icons.AutoMirrored.Rounded.QueueMusic, S.playlists, current == Screen.Playlists) { app.nav.root(Screen.Playlists) }
        NavItem(Icons.Rounded.Favorite, S.favorites, current == Screen.Favorites) { app.nav.root(Screen.Favorites) }
        NavItem(Icons.Rounded.Person, S.artists, current == Screen.Artists) { app.nav.root(Screen.Artists) }
        NavItem(Icons.Rounded.Album, S.albums, current == Screen.Albums) { app.nav.root(Screen.Albums) }
        NavItem(Icons.Rounded.DownloadForOffline, S.downloads, current == Screen.Downloads) { app.nav.root(Screen.Downloads) }

        Box(Modifier.padding(horizontal = 20.dp, vertical = 14.dp).fillMaxWidth().height(1.dp).background(Line))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            LoadContent(playlists) { list ->
                Column {
                    list.forEach { p ->
                        val selected = current == Screen.Playlist(p.id)
                        Text(
                            p.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selected) MaterialTheme.colorScheme.primary else MistDim,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().pointerHoverIcon(PointerIcon.Hand)
                                .clickable { app.nav.go(Screen.Playlist(p.id)) }
                                .padding(horizontal = 24.dp, vertical = 7.dp),
                        )
                    }
                }
            }
        }
        NavItem(Icons.Rounded.Settings, S.settings, current == Screen.Settings) { app.nav.root(Screen.Settings) }
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        Modifier.padding(horizontal = 10.dp).fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(if (selected) accent.copy(alpha = 0.14f) else Color.Transparent)
            .pointerHoverIcon(PointerIcon.Hand).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (selected) accent else MistDim, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, color = if (selected) MaterialTheme.colorScheme.onSurface else MistDim)
    }
}

@Composable
private fun MpvMissingBanner() {
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.error.copy(alpha = 0.16f)).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Rounded.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(S.mpvMissingTitle, style = MaterialTheme.typography.titleSmall)
            Text(S.mpvMissingText, style = MaterialTheme.typography.bodySmall, color = MistDim)
        }
    }
}
