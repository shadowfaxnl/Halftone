package app.halftone

import android.content.Context
import android.widget.Toast
import app.halftone.data.Credentials
import app.halftone.data.Favorites
import app.halftone.data.MusicRepository
import app.halftone.data.Prefs
import app.halftone.data.SettingsStore
import app.halftone.data.Song
import app.halftone.data.SubsonicClient
import app.halftone.data.normalizeServerUrl
import app.halftone.data.randomSalt
import app.halftone.data.userMessage
import app.halftone.download.Downloads
import app.halftone.playback.PlayerConnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

sealed interface SessionState {
    data object Unknown : SessionState
    data object LoggedOut : SessionState
    data class LoggedIn(val credentials: Credentials) : SessionState
}

/** Eenvoudige handmatige dependency injection: één plek waar alles wordt aangemaakt. */
class AppContainer(private val context: Context) {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

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

    val client = SubsonicClient(okHttp, json)
    val settings = SettingsStore(context)
    val repository = MusicRepository(client)
    val favorites = Favorites(client, appScope, onError = ::toast)
    val downloads = Downloads(context, okHttp, client, json, appScope)
    val player = PlayerConnection(context, client, appScope)
    val actions = PlaybackActions()

    val session: StateFlow<SessionState> = settings.login
        .map { login ->
            client.credentials = login
            if (login == null) SessionState.LoggedOut else SessionState.LoggedIn(login)
        }
        .stateIn(appScope, SharingStarted.Eagerly, SessionState.Unknown)

    val prefs: StateFlow<Prefs> = settings.prefs
        .stateIn(appScope, SharingStarted.Eagerly, Prefs())

    /** Nummers waarvoor de "toevoegen aan afspeellijst"-sheet open staat; null = dicht. */
    private val _addToPlaylist = MutableStateFlow<List<Song>?>(null)
    val addToPlaylist: StateFlow<List<Song>?> = _addToPlaylist.asStateFlow()

    init {
        player.connect()
        appScope.launch {
            session.collect { if (it is SessionState.LoggedIn) favorites.refreshQuietly() }
        }
        appScope.launch {
            prefs.collect { client.maxBitRate = it.maxBitRate }
        }
    }

    suspend fun logIn(server: String, username: String, password: String) {
        val creds = Credentials(normalizeServerUrl(server), username.trim(), password, randomSalt())
        client.ping(creds)
        settings.save(creds)
    }

    /** Downloads blijven staan, zodat je na opnieuw inloggen niets opnieuw hoeft binnen te halen. */
    suspend fun logOut() {
        player.stopAndClear()
        repository.clearCache()
        favorites.clear()
        settings.clearLogin()
    }

    fun openAddToPlaylist(songs: List<Song>) {
        if (songs.isNotEmpty()) _addToPlaylist.value = songs
    }

    fun closeAddToPlaylist() {
        _addToPlaylist.value = null
    }

    /** Voert een netwerkactie uit en toont bij een fout een melding. */
    fun launchSafely(block: suspend () -> Unit) {
        appScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                toast(e.userMessage())
            }
        }
    }

    /** Acties die eerst iets moeten ophalen voordat er gespeeld wordt. */
    inner class PlaybackActions {
        fun playAlbum(albumId: String, shuffle: Boolean = false) = launchSafely {
            val songs = repository.album(albumId).song
            play(if (shuffle) songs.shuffled() else songs)
        }

        fun playArtist(artistId: String, shuffle: Boolean = false) = launchSafely {
            val songs = repository.artistSongs(artistId)
            play(if (shuffle) songs.shuffled() else songs)
        }

        fun playPlaylist(playlistId: String, shuffle: Boolean = false) = launchSafely {
            val songs = repository.playlist(playlistId).entry
            play(if (shuffle) songs.shuffled() else songs)
        }

        fun shuffleMix() = launchSafely { play(repository.randomSongs(80)) }

        fun downloadAlbum(albumId: String) = launchSafely {
            downloads.download(repository.album(albumId).song)
        }

        private fun play(songs: List<Song>) {
            if (songs.isEmpty()) {
                toast(Str.get(R.string.no_songs_found))
            } else {
                player.playSongs(songs)
            }
        }
    }

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
