package app.halftone.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import app.halftone.HalftoneApp
import app.halftone.MainActivity
import app.halftone.R
import app.halftone.data.Song
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.future

/**
 * Speelt muziek af op de achtergrond. Media3 regelt de melding,
 * het vergrendelscherm, Bluetooth-knoppen en audiofocus.
 *
 * Gapless: ExoPlayer laadt het volgende nummer al terwijl het huidige speelt en
 * gebruikt de encoder-informatie in MP3/AAC/FLAC/Opus om stiltes aan het begin
 * en eind weg te halen. Met streamkwaliteit "Origineel" blijft die informatie intact.
 *
 * Als MediaLibraryService biedt hij ook een bladerboom aan ([AutoLibrary]), zodat
 * Android Auto je bibliotheek kan tonen, doorzoeken en met spraak kan afspelen.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaLibraryService() {

    private var session: MediaLibrarySession? = null
    private lateinit var library: AutoLibrary
    private var scrobbler: Scrobbler? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        val container = (application as HalftoneApp).container
        library = AutoLibrary(this, container)

        val network = DefaultDataSource.Factory(this, OkHttpDataSource.Factory(container.okHttp))
        // Eerst in de downloadcache kijken (op song-ID); anders gewoon streamen.
        // Streamen schrijft niet in die cache: die is alleen voor bewuste downloads.
        val dataSourceFactory = CacheDataSource.Factory()
            .setCache(container.downloads.cache)
            .setUpstreamDataSourceFactory(network)
            .setCacheWriteDataSinkFactory(null)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        // Ruimere buffer: het volgende nummer staat klaar voordat het huidige eindigt,
        // en korte haperingen in mobiel netwerk vallen niet op.
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 30_000,
                /* maxBufferMs = */ 120_000,
                /* bufferForPlaybackMs = */ 1_500,
                /* bufferForPlaybackAfterRebufferMs = */ 4_000,
            )
            .build()

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this).setDataSourceFactory(dataSourceFactory))
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        session = MediaLibrarySession.Builder(this, player, SessionCallback())
            .setSessionActivity(openApp)
            .build()

        // Het Halftone-raster als icoon in de mediamelding.
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this).build().apply {
                setSmallIcon(R.drawable.ic_stat_halftone)
            },
        )

        scrobbler = Scrobbler(
            player = player,
            repository = container.repository,
            isEnabled = { container.prefs.value.scrobble },
            scope = scope,
        ).also { it.start() }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        scrobbler?.stop()
        scope.cancel()
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    /** Zet nummers om naar speelbare items: stream-URL, en de downloadcache als het nummer offline staat. */
    private fun resolve(items: List<MediaItem>): MutableList<MediaItem> {
        val container = (application as HalftoneApp).container
        val downloaded = container.downloads.items.value
        return items.map { item ->
            runCatching {
                // Alleen volledig gedownloade nummers uit de cache halen. Zo mengen we nooit
                // een half gedownload origineel met een stream in een andere bitrate.
                val isDownloaded = downloaded[item.mediaId]?.isDone == true
                item.buildUpon()
                    .setUri(container.client.streamUrl(item.mediaId))
                    .setCustomCacheKey(if (isDownloaded) item.mediaId else "stream:${item.mediaId}")
                    .build()
            }.getOrDefault(item)
        }.toMutableList()
    }

    private fun songsToItems(songs: List<Song>): MutableList<MediaItem> {
        val client = (application as HalftoneApp).container.client
        return resolve(songs.map { it.toMediaItem(client) })
    }

    /** Laat Android Auto de hoesjes uit [ArtworkProvider] lezen. */
    private fun grantArtwork(packageName: String) {
        val base = ArtworkProvider.baseUri(this)
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        (listOf(packageName) + CAR_PACKAGES).distinct().forEach { pkg ->
            runCatching { grantUriPermission(pkg, base, flags) }
        }
    }

    /**
     * Voert een bladeractie uit en zet fouten om in een foutresultaat in plaats van een crash.
     * Een foutresultaat draagt geen waarde, dus de typeconversie hieronder is altijd veilig.
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T> libraryCall(block: suspend () -> LibraryResult<T>): ListenableFuture<LibraryResult<T>> =
        scope.future {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                LibraryResult.ofError(SessionError.ERROR_IO)
            }
        }

    /**
     * Om veiligheidsredenen stuurt een MediaController geen stream-URI mee naar de sessie.
     * Hier bouwen we die opnieuw op uit het media-ID, met het ID als cachesleutel
     * zodat gedownloade nummers uit de downloadcache spelen.
     *
     * Items uit Android Auto (een nummer in een album, "shuffle", een spraakopdracht)
     * worden eerst uitgebreid tot de nummers die erbij horen.
     */
    private inner class SessionCallback : MediaLibrarySession.Callback {

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> = scope.future {
            val out = mutableListOf<MediaItem>()
            for (item in mediaItems) {
                val expansion = runCatching { library.expand(item) }.getOrNull()
                when {
                    expansion == null -> out += resolve(listOf(item))
                    item.mediaId.startsWith(AutoLibrary.SONG) ->
                        expansion.songs.getOrNull(expansion.startIndex)?.let { out += songsToItems(listOf(it)) }
                    else -> out += songsToItems(expansion.songs)
                }
            }
            out
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = scope.future {
            val single = mediaItems.singleOrNull()
            val expansion = single?.let { runCatching { library.expand(it) }.getOrNull() }
            if (expansion != null && expansion.songs.isNotEmpty()) {
                MediaSession.MediaItemsWithStartPosition(songsToItems(expansion.songs), expansion.startIndex, 0L)
            } else {
                MediaSession.MediaItemsWithStartPosition(resolve(mediaItems), startIndex, startPositionMs)
            }
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            grantArtwork(browser.packageName)
            val rootParams = LibraryParams.Builder().setExtras(library.rootExtras()).build()
            return libraryCall { LibraryResult.ofItem(library.root(), rootParams) }
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> = libraryCall {
            library.item(mediaId)?.let { LibraryResult.ofItem(it, null) }
                ?: LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = libraryCall {
            LibraryResult.ofItemList(library.children(parentId).page(page, pageSize), params)
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<Void>> = libraryCall {
            val count = library.search(query).size
            session.notifySearchResultChanged(browser, query, count, params)
            LibraryResult.ofVoid()
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = libraryCall {
            LibraryResult.ofItemList(library.search(query).page(page, pageSize), params)
        }
    }

    private companion object {
        val CAR_PACKAGES = listOf(
            "com.google.android.projection.gearhead", // Android Auto
            "com.android.car.media",                  // Android Automotive OS
            "com.google.android.carassistant",
        )

        fun <T> List<T>.page(page: Int, pageSize: Int): List<T> {
            if (pageSize <= 0 || pageSize == Int.MAX_VALUE) return this
            val from = page.toLong() * pageSize
            if (from >= size) return emptyList()
            return subList(from.toInt(), minOf(size, from.toInt() + pageSize))
        }
    }
}
