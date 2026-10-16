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
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import app.halftone.HalftoneApp
import app.halftone.MainActivity
import app.halftone.R
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Speelt muziek af op de achtergrond. Media3 regelt de melding,
 * het vergrendelscherm, Bluetooth-knoppen en audiofocus.
 *
 * Gapless: ExoPlayer laadt het volgende nummer al terwijl het huidige speelt en
 * gebruikt de encoder-informatie in MP3/AAC/FLAC/Opus om stiltes aan het begin
 * en eind weg te halen. Met streamkwaliteit "Origineel" blijft die informatie intact.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null
    private var scrobbler: Scrobbler? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        val container = (application as HalftoneApp).container

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

        session = MediaSession.Builder(this, player)
            .setSessionActivity(openApp)
            .setCallback(SessionCallback())
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

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

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

    /**
     * Om veiligheidsredenen stuurt een MediaController geen stream-URI mee naar de sessie.
     * Hier bouwen we die opnieuw op uit het media-ID, met het ID als cachesleutel
     * zodat gedownloade nummers uit de downloadcache spelen.
     */
    private inner class SessionCallback : MediaSession.Callback {
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> {
            val container = (application as HalftoneApp).container
            val downloaded = container.downloads.items.value
            val resolved = mediaItems.map { item ->
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
            return Futures.immediateFuture(resolved)
        }
    }
}
