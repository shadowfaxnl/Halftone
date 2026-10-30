package app.halftone.data

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap
import app.halftone.R
import app.halftone.Str

data class HomeData(
    val hero: Album?,
    val recent: List<Album>,
    val newest: List<Album>,
    val frequent: List<Album>,
    val discover: List<Album>,
    val playlists: List<Playlist>,
)

class MusicRepository(private val client: SubsonicClient) {

    private val albumCache = ConcurrentHashMap<String, AlbumWithSongs>()
    @Volatile private var artistsCache: List<Artist>? = null

    /** Telt op bij elke wijziging aan afspeellijsten, zodat schermen weten dat ze moeten verversen. */
    private val _playlistVersion = MutableStateFlow(0)
    val playlistVersion: StateFlow<Int> = _playlistVersion.asStateFlow()

    suspend fun albumList(type: String, size: Int = 20, offset: Int = 0): List<Album> =
        client.call(
            "getAlbumList2",
            mapOf("type" to type, "size" to size, "offset" to offset),
        ).albumList2?.album.orEmpty()

    suspend fun home(): HomeData = coroutineScope {
        val recent = async { runCatching { albumList("recent", 16) } }
        val newest = async { runCatching { albumList("newest", 16) } }
        val frequent = async { runCatching { albumList("frequent", 16) } }
        val random = async { runCatching { albumList("random", 17) } }
        val playlists = async { runCatching { playlists() } }
        val results = listOf(recent, newest, frequent, random).awaitAll()
        // Alleen falen als werkelijk niets lukte.
        if (results.all { it.isFailure }) throw results.first().exceptionOrNull()!!
        val discover = random.await().getOrDefault(emptyList())
        HomeData(
            hero = discover.firstOrNull(),
            recent = recent.await().getOrDefault(emptyList()),
            newest = newest.await().getOrDefault(emptyList()),
            frequent = frequent.await().getOrDefault(emptyList()),
            discover = discover.drop(1),
            playlists = playlists.await().getOrDefault(emptyList()),
        )
    }

    /** Haalt alle albums op in pagina's van 500. */
    suspend fun allAlbums(): List<Album> {
        val all = mutableListOf<Album>()
        var offset = 0
        while (true) {
            val page = albumList("alphabeticalByName", size = 500, offset = offset)
            all += page
            if (page.size < 500 || all.size >= 20_000) break
            offset += 500
        }
        return all
    }

    suspend fun artists(force: Boolean = false): List<Artist> {
        if (!force) artistsCache?.let { return it }
        val list = client.call("getArtists").artists?.index.orEmpty().flatMap { it.artist }
        artistsCache = list
        return list
    }

    suspend fun artist(id: String): ArtistWithAlbums =
        client.call("getArtist", mapOf("id" to id)).artist
            ?: throw SubsonicException(70, Str.get(R.string.error_artist_not_found))

    suspend fun album(id: String): AlbumWithSongs {
        albumCache[id]?.let { return it }
        val album = client.call("getAlbum", mapOf("id" to id)).album
            ?: throw SubsonicException(70, Str.get(R.string.error_album_not_found))
        albumCache[id] = album
        return album
    }

    /** Alle nummers van een artiest, album voor album, in volgorde. */
    suspend fun artistSongs(id: String): List<Song> = coroutineScope {
        val artist = artist(id)
        artist.album
            .sortedWith(compareBy<Album> { it.year ?: Int.MAX_VALUE }.thenBy { it.name })
            .map { album -> async { album(album.id).song } }
            .awaitAll()
            .flatten()
    }

    suspend fun search(query: String): SearchResult =
        client.call(
            "search3",
            mapOf("query" to query, "artistCount" to 10, "albumCount" to 20, "songCount" to 40),
        ).searchResult3 ?: SearchResult()

    suspend fun song(id: String): Song? =
        client.call("getSong", mapOf("id" to id)).song

    suspend fun randomSongs(size: Int = 80): List<Song> =
        client.call("getRandomSongs", mapOf("size" to size)).randomSongs?.song.orEmpty()

    // ───────────── Afspeellijsten ─────────────

    suspend fun playlists(): List<Playlist> =
        client.call("getPlaylists").playlists?.playlist.orEmpty()

    suspend fun playlist(id: String): PlaylistWithSongs =
        client.call("getPlaylist", mapOf("id" to id)).playlist
            ?: throw SubsonicException(70, Str.get(R.string.error_playlist_not_found))

    suspend fun createPlaylist(name: String, songIds: List<String> = emptyList()) {
        client.call("createPlaylist", mapOf("name" to name, "songId" to songIds))
        playlistsChanged()
    }

    suspend fun addToPlaylist(playlistId: String, songIds: List<String>) {
        if (songIds.isEmpty()) return
        // In delen, zodat de URL niet te lang wordt bij grote albums.
        songIds.chunked(100).forEach { chunk ->
            client.call("updatePlaylist", mapOf("playlistId" to playlistId, "songIdToAdd" to chunk))
        }
        playlistsChanged()
    }

    suspend fun removeFromPlaylist(playlistId: String, index: Int) {
        client.call("updatePlaylist", mapOf("playlistId" to playlistId, "songIndexToRemove" to index))
        playlistsChanged()
    }

    suspend fun renamePlaylist(playlistId: String, name: String) {
        client.call("updatePlaylist", mapOf("playlistId" to playlistId, "name" to name))
        playlistsChanged()
    }

    suspend fun deletePlaylist(playlistId: String) {
        client.call("deletePlaylist", mapOf("id" to playlistId))
        playlistsChanged()
    }

    private fun playlistsChanged() {
        _playlistVersion.update { it + 1 }
    }

    // ───────────── Scrobbelen ─────────────

    /**
     * submission = false meldt "speelt nu"; true telt het nummer als beluisterd.
     * [timeMs] is het moment waarop het nummer begon.
     */
    suspend fun scrobble(songId: String, timeMs: Long, submission: Boolean) {
        client.call(
            "scrobble",
            mapOf("id" to songId, "time" to timeMs, "submission" to submission),
        )
    }

    fun clearCache() {
        albumCache.clear()
        artistsCache = null
    }
}
