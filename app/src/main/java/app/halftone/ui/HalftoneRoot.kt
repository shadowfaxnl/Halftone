package app.halftone.ui

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.halftone.SessionState
import app.halftone.data.COVER_SMALL
import app.halftone.ui.album.AlbumScreen
import app.halftone.ui.artist.ArtistScreen
import app.halftone.ui.common.LocalAppContainer
import app.halftone.ui.downloads.DownloadsScreen
import app.halftone.ui.home.HomeScreen
import app.halftone.ui.playlist.AddToPlaylistSheet
import app.halftone.ui.playlist.PlaylistScreen
import app.halftone.ui.settings.SettingsScreen
import app.halftone.ui.library.LibraryScreen
import app.halftone.ui.login.LoginScreen
import app.halftone.ui.player.MiniPlayer
import app.halftone.ui.player.NowPlayingScreen
import app.halftone.ui.search.SearchScreen
import app.halftone.ui.theme.Ink
import app.halftone.ui.theme.HalftoneTheme
import app.halftone.ui.theme.rememberCoverColors

@Composable
fun HalftoneRoot() {
    val container = LocalAppContainer.current
    val session by container.session.collectAsStateWithLifecycle()
    Crossfade(targetState = session::class, animationSpec = tween(400), label = "session") { kind ->
        when (kind) {
            SessionState.LoggedIn::class -> MainScaffold()
            SessionState.LoggedOut::class -> HalftoneTheme { LoginScreen() }
            else -> Box(Modifier.fillMaxSize().background(Ink))
        }
    }
}

/** Navigatie tussen schermen. Eén plek, zodat elk scherm alleen lambda's krijgt. */
class Navigator(private val nav: NavHostController) {
    fun album(id: String) = nav.navigate("album/${Uri.encode(id)}")
    fun artist(id: String) = nav.navigate("artist/${Uri.encode(id)}")
    fun playlist(id: String) = nav.navigate("playlist/${Uri.encode(id)}")
    fun settings() = nav.navigate("settings")
    fun downloads() = nav.navigate("downloads")
    fun back() {
        nav.popBackStack()
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Home", Icons.Rounded.Home),
    Tab("library", "Bibliotheek", Icons.Rounded.LibraryMusic),
    Tab("search", "Zoeken", Icons.Rounded.Search),
)

@Composable
private fun MainScaffold() {
    val container = LocalAppContainer.current
    val playerState by container.player.state.collectAsStateWithLifecycle()
    val current = playerState.current

    // De hele app kleurt mee met het hoesje dat nu speelt.
    val coverUrl = container.client.coverArtUrl(current?.coverArt, COVER_SMALL)
    val colors = rememberCoverColors(coverUrl)

    val navController = rememberNavController()
    val navigator = remember(navController) { Navigator(navController) }
    var playerOpen by rememberSaveable { mutableStateOf(false) }
    val addToPlaylist by container.addToPlaylist.collectAsStateWithLifecycle()

    NotificationPermissionRequester()

    HalftoneTheme(accent = colors.accent) {
        val backdrop by animateColorAsState(colors.backdrop, tween(900), label = "backdrop")
        Box(
            Modifier
                .fillMaxSize()
                .background(Ink)
                .background(
                    Brush.verticalGradient(
                        0f to backdrop.copy(alpha = if (current != null) 0.9f else 0.5f),
                        0.45f to Ink,
                    ),
                ),
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0),
                bottomBar = {
                    Column {
                        AnimatedVisibility(
                            visible = current != null,
                            enter = slideInVertically { it } + fadeIn(),
                            exit = slideOutVertically { it } + fadeOut(),
                        ) {
                            MiniPlayer(
                                state = playerState,
                                position = container.player.position,
                                onOpen = { playerOpen = true },
                                onToggle = container.player::togglePlay,
                                onNext = container.player::next,
                            )
                        }
                        BottomBar(navController)
                    }
                },
            ) { padding ->
                HalftoneNavHost(navController, navigator, padding)
            }

            AnimatedVisibility(
                visible = playerOpen && current != null,
                enter = slideInVertically(tween(420)) { it } + fadeIn(tween(300)),
                exit = slideOutVertically(tween(360)) { it } + fadeOut(tween(300)),
            ) {
                NowPlayingScreen(
                    colors = colors,
                    onClose = { playerOpen = false },
                    onOpenAlbum = {
                        playerOpen = false
                        navigator.album(it)
                    },
                    onOpenArtist = {
                        playerOpen = false
                        navigator.artist(it)
                    },
                )
            }

            // Eén gedeeld onderblad voor "toevoegen aan afspeellijst", vanuit elk scherm.
            addToPlaylist?.let { songs ->
                AddToPlaylistSheet(songs = songs, onDismiss = container::closeAddToPlaylist)
            }
        }
    }
}

/** Vraagt om meldingen te mogen tonen zodra de eerste download start (Android 13+). */
@Composable
private fun NotificationPermissionRequester() {
    val container = LocalAppContainer.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        container.downloads.permissionRequests.collect {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController) {
    val entry by navController.currentBackStackEntryAsState()
    val destination = entry?.destination
    NavigationBar(
        containerColor = Ink.copy(alpha = 0.94f),
        tonalElevation = 0.dp,
    ) {
        tabs.forEach { tab ->
            val selected = destination?.hierarchy?.any { it.route == tab.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun HalftoneNavHost(
    navController: NavHostController,
    navigator: Navigator,
    padding: PaddingValues,
) {
    NavHost(
        navController = navController,
        startDestination = "home",
        enterTransition = { fadeIn(tween(260)) + slideInHorizontally(tween(320)) { it / 10 } },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(260)) },
        popExitTransition = { fadeOut(tween(200)) },
    ) {
        composable("home") { HomeScreen(navigator, padding) }
        composable("library") { LibraryScreen(navigator, padding) }
        composable("search") { SearchScreen(navigator, padding) }
        composable("album/{id}") { entry ->
            AlbumScreen(entry.arguments?.getString("id").orEmpty(), navigator, padding)
        }
        composable("artist/{id}") { entry ->
            ArtistScreen(entry.arguments?.getString("id").orEmpty(), navigator, padding)
        }
        composable("playlist/{id}") { entry ->
            PlaylistScreen(entry.arguments?.getString("id").orEmpty(), navigator, padding)
        }
        composable("settings") { SettingsScreen(navigator, padding) }
        composable("downloads") { DownloadsScreen(navigator, padding) }
    }
}
