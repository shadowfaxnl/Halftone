package app.halftone.download

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import app.halftone.data.COVER_LARGE
import app.halftone.data.COVER_SMALL
import app.halftone.data.Song
import app.halftone.data.SubsonicClient
import coil3.imageLoader
import coil3.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.Executors

/** Eén gedownload (of downloadend) nummer. */
data class DownloadItem(
    val song: Song?,
    val state: Int,
    /** 0..1, of null als nog onbekend. */
    val progress: Float?,
    val bytes: Long,
) {
    val isDone: Boolean get() = state == Download.STATE_COMPLETED
    val isActive: Boolean
        get() = state == Download.STATE_QUEUED ||
            state == Download.STATE_DOWNLOADING ||
            state == Download.STATE_RESTARTING
    val isFailed: Boolean get() = state == Download.STATE_FAILED
}

/** Wat een groep nummers (album, afspeellijst) aan downloads heeft. */
enum class GroupDownloadState { None, Partial, Active, Done }

/**
 * Offline luisteren via Media3. Nummers komen in een eigen cache die nooit
 * automatisch wordt opgeruimd. De afspeler leest eerst uit deze cache (op song-ID),
 * dus een gedownload nummer speelt altijd lokaal, ook zonder verbinding.
 */
@OptIn(UnstableApi::class)
class Downloads(
    private val context: Context,
    okHttp: OkHttpClient,
    private val client: SubsonicClient,
    private val json: Json,
    private val scope: CoroutineScope,
) {
    private val databaseProvider = StandaloneDatabaseProvider(context)

    val cache: Cache = SimpleCache(
        File(context.filesDir, "downloads"),
        NoOpCacheEvictor(),
        databaseProvider,
    )

    val manager = DownloadManager(
        context,
        databaseProvider,
        cache,
        OkHttpDataSource.Factory(okHttp),
        Executors.newFixedThreadPool(2),
    ).apply {
        maxParallelDownloads = 2
    }

    private val _items = MutableStateFlow<Map<String, DownloadItem>>(emptyMap())
    val items: StateFlow<Map<String, DownloadItem>> = _items.asStateFlow()

    private val _bytesUsed = MutableStateFlow(0L)
    val bytesUsed: StateFlow<Long> = _bytesUsed.asStateFlow()

    private val _permissionRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** De UI vraagt dan om toestemming voor meldingen (voortgang van downloads). */
    val permissionRequests: SharedFlow<Unit> = _permissionRequests.asSharedFlow()

    init {
        manager.addListener(object : DownloadManager.Listener {
            override fun onInitialized(downloadManager: DownloadManager) {
                reloadIndex()
            }

            override fun onDownloadChanged(
                downloadManager: DownloadManager,
                download: Download,
                finalException: Exception?,
            ) {
                upsert(download)
                if (download.state == Download.STATE_COMPLETED) updateBytes()
            }

            override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
                _items.update { it - download.request.id }
                updateBytes()
            }
        })
        reloadIndex()

        // Voortgang wordt niet als gebeurtenis gemeld; zolang er iets loopt, kijken we zelf.
        scope.launch {
            while (true) {
                delay(700)
                if (_items.value.values.any { it.isActive }) {
                    manager.currentDownloads.forEach(::upsert)
                }
            }
        }
    }

    private fun reloadIndex() {
        scope.launch {
            val all = withContext(Dispatchers.IO) {
                buildMap {
                    manager.downloadIndex.getDownloads().use { cursor ->
                        while (cursor.moveToNext()) {
                            val d = cursor.download
                            put(d.request.id, d.toItem())
                        }
                    }
                }
            }
            _items.value = all
            updateBytes()
        }
    }

    private fun upsert(download: Download) {
        _items.update { it + (download.request.id to download.toItem()) }
    }

    private fun updateBytes() {
        scope.launch {
            _bytesUsed.value = withContext(Dispatchers.IO) { runCatching { cache.cacheSpace }.getOrDefault(0L) }
        }
    }

    private fun Download.toItem(): DownloadItem {
        val song = runCatching {
            json.decodeFromString<Song>(String(request.data, Charsets.UTF_8))
        }.getOrNull()
        val percent = percentDownloaded
        return DownloadItem(
            song = song,
            state = state,
            progress = if (percent >= 0f) percent / 100f else null,
            bytes = bytesDownloaded,
        )
    }

    fun download(songs: List<Song>) {
        val current = _items.value
        val todo = songs.distinctBy { it.id }.filter { song ->
            val existing = current[song.id]
            existing == null || existing.isFailed
        }
        if (todo.isEmpty()) return
        askForNotificationPermission()

        todo.forEach { song ->
            val request = DownloadRequest.Builder(song.id, Uri.parse(client.downloadUrl(song.id)))
                .setCustomCacheKey(song.id)
                .setData(json.encodeToString(song).toByteArray(Charsets.UTF_8))
                .build()
            DownloadService.sendAddDownload(context, HalftoneDownloadService::class.java, request, false)
        }

        // Hoesjes alvast in de beeldcache zetten, zodat ze offline ook verschijnen.
        todo.mapNotNull { it.coverArt }.distinct().forEach { cover ->
            listOf(COVER_SMALL, COVER_LARGE).forEach { size ->
                client.coverArtUrl(cover, size)?.let { url ->
                    context.imageLoader.enqueue(ImageRequest.Builder(context).data(url).build())
                }
            }
        }
    }

    fun remove(songIds: Collection<String>) {
        songIds.forEach { id ->
            DownloadService.sendRemoveDownload(context, HalftoneDownloadService::class.java, id, false)
        }
    }

    fun removeAll() {
        DownloadService.sendRemoveAllDownloads(context, HalftoneDownloadService::class.java, false)
    }

    fun groupState(songIds: List<String>, items: Map<String, DownloadItem>): GroupDownloadState {
        if (songIds.isEmpty()) return GroupDownloadState.None
        val states = songIds.map { items[it] }
        return when {
            states.all { it?.isDone == true } -> GroupDownloadState.Done
            states.any { it?.isActive == true } -> GroupDownloadState.Active
            states.any { it?.isDone == true } -> GroupDownloadState.Partial
            else -> GroupDownloadState.None
        }
    }

    private fun askForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            _permissionRequests.tryEmit(Unit)
        }
    }
}
