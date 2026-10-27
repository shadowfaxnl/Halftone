package app.halftone.desktop.download

import app.halftone.desktop.data.Paths
import app.halftone.desktop.data.Song
import app.halftone.desktop.data.SubsonicClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

enum class DownloadState { Queued, Downloading, Done, Failed }

data class DownloadItem(
    val song: Song,
    val state: DownloadState,
    /** 0..1, of null als de grootte onbekend is. */
    val progress: Float?,
    val bytes: Long,
) {
    val isDone get() = state == DownloadState.Done
    val isActive get() = state == DownloadState.Queued || state == DownloadState.Downloading
    val isFailed get() = state == DownloadState.Failed
}

enum class GroupDownloadState { None, Partial, Active, Done }

@Serializable
private data class IndexEntry(val song: Song, val file: String, val bytes: Long)

/**
 * Offline luisteren: het originele bestand komt in ~/.local/share/halftone/downloads.
 * De speler gebruikt een gedownload bestand altijd in plaats van de stream.
 */
class Downloads(
    private val http: OkHttpClient,
    private val client: SubsonicClient,
    private val json: Json,
    private val scope: CoroutineScope,
) {
    private val dir = File(Paths.data, "downloads").apply { mkdirs() }
    private val indexFile = File(dir, "index.json")
    private val indexSerializer = MapSerializer(String.serializer(), IndexEntry.serializer())
    private val index = mutableMapOf<String, IndexEntry>()

    private val _items = MutableStateFlow<Map<String, DownloadItem>>(emptyMap())
    val items: StateFlow<Map<String, DownloadItem>> = _items.asStateFlow()

    private val _bytesUsed = MutableStateFlow(0L)
    val bytesUsed: StateFlow<Long> = _bytesUsed.asStateFlow()

    private val queue = Channel<Song>(Channel.UNLIMITED)

    init {
        runCatching {
            if (indexFile.exists()) index += json.decodeFromString(indexSerializer, indexFile.readText())
        }
        // Alleen bestanden die echt nog bestaan.
        index.entries.removeAll { !File(dir, it.value.file).exists() }
        _items.value = index.mapValues { (_, e) -> DownloadItem(e.song, DownloadState.Done, 1f, e.bytes) }
        updateBytes()
        repeat(2) { scope.launch(Dispatchers.IO) { for (song in queue) fetch(song) } }
    }

    fun localFile(songId: String): File? =
        synchronized(index) { index[songId] }?.let { File(dir, it.file) }?.takeIf { it.exists() }

    fun download(songs: List<Song>) {
        val current = _items.value
        songs.distinctBy { it.id }
            .filter { current[it.id] == null || current[it.id]?.isFailed == true }
            .forEach { song ->
                _items.update { it + (song.id to DownloadItem(song, DownloadState.Queued, null, 0)) }
                queue.trySend(song)
            }
    }

    fun remove(ids: Collection<String>) {
        scope.launch(Dispatchers.IO) {
            ids.forEach { id ->
                synchronized(index) { index.remove(id) }?.let { File(dir, it.file).delete() }
            }
            _items.update { it - ids.toSet() }
            saveIndex()
            updateBytes()
        }
    }

    fun removeAll() = remove(_items.value.keys.toList())

    fun groupState(ids: List<String>, items: Map<String, DownloadItem>): GroupDownloadState {
        if (ids.isEmpty()) return GroupDownloadState.None
        val states = ids.map { items[it] }
        return when {
            states.all { it?.isDone == true } -> GroupDownloadState.Done
            states.any { it?.isActive == true } -> GroupDownloadState.Active
            states.any { it?.isDone == true } -> GroupDownloadState.Partial
            else -> GroupDownloadState.None
        }
    }

    private fun fetch(song: Song) {
        // Mogelijk inmiddels verwijderd uit de wachtrij.
        if (_items.value[song.id]?.state != DownloadState.Queued) return
        setItem(song, DownloadState.Downloading, 0f, 0)
        val safeId = song.id.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val ext = song.suffix?.takeIf { it.matches(Regex("[A-Za-z0-9]{1,5}")) } ?: "audio"
        val target = File(dir, "$safeId.$ext")
        val tmp = File(dir, "$safeId.part")
        try {
            val request = Request.Builder().url(client.downloadUrl(song.id)).build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}")
                val body = response.body ?: error("empty")
                val total = body.contentLength()
                var done = 0L
                var lastReport = 0L
                body.byteStream().use { input ->
                    tmp.outputStream().use { out ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            out.write(buffer, 0, n)
                            done += n
                            if (done - lastReport > 256 * 1024) {
                                lastReport = done
                                setItem(song, DownloadState.Downloading, if (total > 0) done.toFloat() / total else null, done)
                            }
                        }
                    }
                }
                if (!tmp.renameTo(target)) error("rename")
                synchronized(index) { index[song.id] = IndexEntry(song, target.name, target.length()) }
                saveIndex()
                setItem(song, DownloadState.Done, 1f, target.length())
                updateBytes()
            }
        } catch (e: Throwable) {
            tmp.delete()
            setItem(song, DownloadState.Failed, null, 0)
        }
    }

    private fun setItem(song: Song, state: DownloadState, progress: Float?, bytes: Long) {
        _items.update { it + (song.id to DownloadItem(song, state, progress, bytes)) }
    }

    private fun saveIndex() {
        runCatching {
            val snapshot = synchronized(index) { index.toMap() }
            indexFile.writeText(json.encodeToString(indexSerializer, snapshot))
        }
    }

    private fun updateBytes() {
        _bytesUsed.value = synchronized(index) { index.values.sumOf { it.bytes } }
    }
}
