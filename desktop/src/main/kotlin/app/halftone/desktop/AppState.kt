package app.halftone.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.halftone.desktop.data.Credentials
import app.halftone.desktop.data.Favorites
import app.halftone.desktop.data.MusicRepository
import app.halftone.desktop.data.SettingsStore
import app.halftone.desktop.data.Song
import app.halftone.desktop.data.SubsonicClient
import app.halftone.desktop.data.normalizeServerUrl
import app.halftone.desktop.data.randomSalt
import app.halftone.desktop.data.userMessage
import app.halftone.desktop.download.Downloads
import app.halftone.desktop.i18n.S
import app.halftone.desktop.player.Mpris
import app.halftone.desktop.player.Player
import app.halftone.desktop.player.Scrobbler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

const val APP_VERSION = "0.4.1"

/** De schermen van de app. */
sealed interface Screen {
    data object Home : Screen
    data object Search : Screen
    data object Playlists : Screen
    data object Favorites : Screen
    data object Artists : Screen
    data object Albums : Screen
    data object Downloads : Screen
    data object Settings : Screen
    data class Album(val id: String) : Screen
    data class Artist(val id: String) : Screen
    data class Playlist(val id: String) : Screen
}

/** Navigatie met een eenvoudige terug-stapel. */
class Navigator {
    val stack = mutableStateListOf<Screen>(Screen.Home)
    val current: Screen get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1

    fun go(screen: Screen) {
        if (current != screen) stack.add(screen)
    }

    /** Hoofdonderdelen uit de zijbalk beginnen een nieuwe stapel. */
    fun root(screen: Screen) {
        stack.clear()
        stack.add(screen)
    }

    fun back() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    fun album(id: String) = go(Screen.Album(id))
    fun artist(id: String) = go(Screen.Artist(id))
    fun playlist(id: String) = go(Screen.Playlist(id))
}

/** Alles wat de app nodig heeft, op één plek. */
@OptIn(kotlinx.coroutines.FlowPreview::class)
class AppState {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        isLenient = true
    }

    val okHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val settings = SettingsStore(json)
    val client = SubsonicClient(okHttp, json)
    val repository = MusicRepository(client)
    val favorites = Favorites(client, scope, onError = ::toast)
    val downloads = Downloads(okHttp, client, json, scope)
    val player = Player(client, downloads, settings.state.value.volume)
    val nav = Navigator()

    var loggedIn by mutableStateOf(false)
        private set
    var nowPlayingOpen by mutableStateOf(false)
    var queueOpen by mutableStateOf(false)
    var addToPlaylist by mutableStateOf<List<Song>?>(null)
    /** Waar staat de toetsenbordfocus in een tekstveld? Dan geen sneltoetsen voor spatie. */
    var typing by mutableStateOf(false)

    private val _toasts = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toasts: SharedFlow<String> = _toasts.asSharedFlow()

    /** Wordt door het venster ingevuld, zodat MPRIS het venster naar voren kan halen. */
    var raiseWindow: () -> Unit = {}
    var quitApp: () -> Unit = {}

    private var mpris: Mpris? = null

    init {
        val creds = settings.state.value.credentials
        client.credentials = creds
        client.maxBitRate = settings.state.value.maxBitRate
        loggedIn = creds != null
        if (loggedIn) favorites.refreshQuietly()

        scope.launch {
            settings.state.map { it.maxBitRate }.distinctUntilChanged().collect { client.maxBitRate = it }
        }
        scope.launch {
            // Volume bewaren, met een kleine vertraging zodat slepen niet elke stap schrijft.
            player.state.map { it.volume }.distinctUntilChanged().debounce(400).collect { v ->
                settings.update { it.copy(volume = v) }
            }
        }

        Scrobbler(player, repository, { settings.state.value.scrobble }, scope).start()
        mpris = Mpris(player, client, scope, onRaise = { raiseWindow() }, onQuit = { quitApp() }).also { it.start() }
    }

    suspend fun logIn(server: String, username: String, password: String) {
        val creds = Credentials(normalizeServerUrl(server), username.trim(), password, randomSalt())
        client.ping(creds)
        client.credentials = creds
        settings.saveLogin(creds)
        loggedIn = true
        nav.root(Screen.Home)
        favorites.refreshQuietly()
    }

    fun logOut() {
        player.stopAndClear()
        repository.clearCache()
        favorites.clear()
        settings.clearLogin()
        client.credentials = null
        loggedIn = false
        nowPlayingOpen = false
        queueOpen = false
    }

    fun toast(message: String) {
        _toasts.tryEmit(message)
    }

    fun launchSafely(block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                toast(e.userMessage())
            }
        }
    }

    // ───────────── Acties ─────────────

    fun play(songs: List<Song>, start: Int = 0) {
        if (songs.isEmpty()) toast(S.noSongsFound) else player.playSongs(songs, start)
    }

    fun playAlbum(id: String, shuffle: Boolean = false) = launchSafely {
        val songs = repository.album(id).song
        play(if (shuffle) songs.shuffled() else songs)
    }

    fun playArtist(id: String, shuffle: Boolean = false) = launchSafely {
        val songs = repository.artistSongs(id)
        play(if (shuffle) songs.shuffled() else songs)
    }

    fun shuffleMix() = launchSafely { play(repository.randomSongs(80)) }

    fun shutdown() {
        mpris?.stop()
        player.shutdown()
    }
}
