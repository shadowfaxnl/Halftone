package app.halftone.playback

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import app.halftone.AppContainer
import app.halftone.R
import app.halftone.SessionState
import app.halftone.Str
import app.halftone.data.Album
import app.halftone.data.Artist
import app.halftone.data.Playlist
import app.halftone.data.Song
import kotlinx.coroutines.flow.first

/**
 * De bladerboom die Android Auto (en andere mediabrowsers) te zien krijgen.
 *
 * Opbouw:
 *   root
 *   ├── home        shuffle, recent gespeeld, nieuw toegevoegd, vaak gedraaid
 *   ├── playlists   je afspeellijsten
 *   ├── favorites   favoriete albums, artiesten en nummers
 *   └── library     artiesten, downloads
 *
 * Nummers in een lijst krijgen als ID "song|<lijst>|<nummer>". Tik je er een aan,
 * dan speelt de hele lijst vanaf dat nummer, zoals in de app.
 */
class AutoLibrary(private val context: Context, private val container: AppContainer) {

    data class Expansion(val songs: List<Song>, val startIndex: Int)

    private val repo get() = container.repository

    /** Wacht tot de inloggegevens geladen zijn; nodig als Auto de app koud opstart. */
    suspend fun isLoggedIn(): Boolean =
        container.session.first { it !is SessionState.Unknown } is SessionState.LoggedIn

    fun root(): MediaItem = folder(ROOT, "Halftone", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)

    /** Extra's voor de root: zoeken aan, mappen als raster, nummers als lijst. */
    fun rootExtras(): Bundle = bundleOf(
        SEARCH_SUPPORTED to true,
        STYLE_BROWSABLE to STYLE_GRID,
        STYLE_PLAYABLE to STYLE_LIST,
    )

    suspend fun item(id: String): MediaItem? = when (id) {
        ROOT -> root()
        HOME, PLAYLISTS, FAVORITES, LIBRARY, ARTISTS, DOWNLOADS -> tabs().plus(libraryFolders()).firstOrNull { it.mediaId == id }
        else -> null
    }

    suspend fun children(parentId: String): List<MediaItem> {
        if (parentId == ROOT) return tabs()
        if (!isLoggedIn()) return listOf(message(SIGN_IN, Str.get(R.string.auto_sign_in)))
        return when {
            parentId == HOME -> home()
            parentId == PLAYLISTS -> repo.playlists().map(::playlistItem)
            parentId == FAVORITES -> favorites()
            parentId == LIBRARY -> libraryFolders()
            parentId == ARTISTS -> repo.artists().map { artistItem(it) }
            parentId == DOWNLOADS -> downloadedSongs().map { songItem(it, DOWNLOADS) }
            parentId.startsWith(ALBUM) -> repo.album(parentId.removePrefix(ALBUM)).song.map { songItem(it, parentId) }
            parentId.startsWith(PLAYLIST) -> repo.playlist(parentId.removePrefix(PLAYLIST)).entry.map { songItem(it, parentId) }
            parentId.startsWith(ARTIST) -> repo.artist(parentId.removePrefix(ARTIST)).album
                .sortedByDescending { it.year ?: 0 }
                .map { albumItem(it) }
            else -> emptyList()
        }
    }

    suspend fun search(query: String): List<MediaItem> {
        if (!isLoggedIn() || query.isBlank()) return emptyList()
        val result = repo.search(query.trim())
        val artists = Str.get(R.string.artists)
        val albums = Str.get(R.string.albums)
        val songs = Str.get(R.string.songs)
        return result.song.map { songItem(it, SEARCH, group = songs) } +
            result.album.map { albumItem(it, group = albums) } +
            result.artist.map { artistItem(it, group = artists) }
    }

    /**
     * Zet een aangetikt item om in iets afspeelbaars. Geeft null terug voor gewone nummers
     * die de app zelf in de wachtrij zet; die hoeven niet uitgebreid te worden.
     */
    suspend fun expand(item: MediaItem): Expansion? {
        val id = item.mediaId
        val query = item.requestMetadata.searchQuery
        val fromBrowser = (id.isEmpty() && query != null) || id.startsWith(SONG) || id.startsWith("shuffle:") ||
            id.startsWith(ALBUM) || id.startsWith(ARTIST) || id.startsWith(PLAYLIST)
        if (!fromBrowser || !isLoggedIn()) return null
        return when {
            id.isEmpty() && query != null -> Expansion(voiceSearch(query), 0)
            id == SHUFFLE_ALL -> Expansion(repo.randomSongs(100), 0)
            id == SHUFFLE_FAVORITES -> Expansion(container.favorites.load().song.shuffled(), 0)
            id.startsWith(ALBUM) -> Expansion(repo.album(id.removePrefix(ALBUM)).song, 0)
            id.startsWith(PLAYLIST) -> Expansion(repo.playlist(id.removePrefix(PLAYLIST)).entry, 0)
            id.startsWith(ARTIST) -> Expansion(repo.artistSongs(id.removePrefix(ARTIST)).shuffled(), 0)
            id.startsWith(SONG) -> {
                val parts = id.split("|", limit = 3)
                if (parts.size < 3) return null
                val (_, parent, songId) = parts
                val songs = songsIn(parent)
                val index = songs.indexOfFirst { it.id == songId }
                when {
                    index >= 0 -> Expansion(songs, index)
                    else -> repo.song(songId)?.let { Expansion(listOf(it), 0) }
                }
            }
            else -> null
        }
    }

    // ───────────── Inhoud ─────────────

    private fun tabs() = listOf(
        folder(HOME, Str.get(R.string.auto_home), MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
        folder(PLAYLISTS, Str.get(R.string.playlists), MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS, childStyle = STYLE_LIST),
        folder(FAVORITES, Str.get(R.string.favorites), MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
        folder(LIBRARY, Str.get(R.string.library), MediaMetadata.MEDIA_TYPE_FOLDER_MIXED, childStyle = STYLE_LIST),
    )

    private fun libraryFolders() = listOf(
        folder(ARTISTS, Str.get(R.string.artists), MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS, childStyle = STYLE_LIST),
        folder(DOWNLOADS, Str.get(R.string.downloads), MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
    )

    private suspend fun home(): List<MediaItem> {
        val home = runCatching { repo.home() }.getOrNull()
            ?: return offline()
        return buildList {
            add(action(SHUFFLE_ALL, Str.get(R.string.auto_shuffle_all)))
            home.recent.take(12).forEach { add(albumItem(it, group = Str.get(R.string.recently_played))) }
            home.newest.take(12).forEach { add(albumItem(it, group = Str.get(R.string.recently_added))) }
            home.frequent.take(12).forEach { add(albumItem(it, group = Str.get(R.string.most_played))) }
            home.discover.take(12).forEach { add(albumItem(it, group = Str.get(R.string.discover))) }
        }
    }

    /** Geen verbinding met je server: laat zien wat er gedownload is. */
    private fun offline(): List<MediaItem> {
        val songs = downloadedSongs()
        if (songs.isEmpty()) throw IllegalStateException("offline")
        val group = Str.get(R.string.auto_offline)
        return songs.map { songItem(it, DOWNLOADS, group = group) }
    }

    private suspend fun favorites(): List<MediaItem> {
        val starred = container.favorites.load()
        if (starred.isEmpty) return listOf(message("fav-empty", Str.get(R.string.favorites_empty_title)))
        return buildList {
            if (starred.song.isNotEmpty()) add(action(SHUFFLE_FAVORITES, Str.get(R.string.auto_shuffle_favorites)))
            starred.album.forEach { add(albumItem(it, group = Str.get(R.string.albums))) }
            starred.artist.forEach { add(artistItem(it, group = Str.get(R.string.artists))) }
            starred.song.forEach { add(songItem(it, FAVORITES, group = Str.get(R.string.songs))) }
        }
    }

    private suspend fun songsIn(parent: String): List<Song> = when {
        parent.startsWith(ALBUM) -> repo.album(parent.removePrefix(ALBUM)).song
        parent.startsWith(PLAYLIST) -> repo.playlist(parent.removePrefix(PLAYLIST)).entry
        parent == FAVORITES -> container.favorites.load().song
        parent == DOWNLOADS -> downloadedSongs()
        else -> emptyList()
    }

    private fun downloadedSongs(): List<Song> =
        container.downloads.items.value.values
            .filter { it.isDone }
            .mapNotNull { it.song }
            .sortedWith(compareBy({ it.artist?.lowercase() }, { it.album?.lowercase() }, { it.discNumber ?: 0 }, { it.track ?: 0 }))

    /** "Hey Google, speel … op Halftone". */
    private suspend fun voiceSearch(raw: String): List<Song> {
        val query = raw.trim()
        if (query.isEmpty()) return repo.randomSongs(100)
        val result = repo.search(query)
        result.artist.firstOrNull { it.name.equals(query, ignoreCase = true) }
            ?.let { return repo.artistSongs(it.id).shuffled() }
        result.album.firstOrNull { it.name.equals(query, ignoreCase = true) }
            ?.let { return repo.album(it.id).song }
        if (result.song.isNotEmpty()) return result.song
        result.album.firstOrNull()?.let { return repo.album(it.id).song }
        result.artist.firstOrNull()?.let { return repo.artistSongs(it.id).shuffled() }
        return emptyList()
    }

    // ───────────── Bouwstenen ─────────────

    private fun folder(id: String, title: String, type: Int, childStyle: Int? = null): MediaItem =
        build(
            id = id,
            title = title,
            browsable = true,
            playable = false,
            type = type,
            extras = childStyle?.let { bundleOf(STYLE_BROWSABLE to it, STYLE_PLAYABLE to STYLE_LIST) },
        )

    private fun albumItem(album: Album, group: String? = null) = build(
        id = ALBUM + album.id,
        title = album.name,
        subtitle = album.artist,
        cover = album.coverArt,
        browsable = true,
        playable = false,
        type = MediaMetadata.MEDIA_TYPE_ALBUM,
        extras = group?.let { bundleOf(GROUP_TITLE to it) },
    )

    private fun artistItem(artist: Artist, group: String? = null) = build(
        id = ARTIST + artist.id,
        title = artist.name,
        cover = artist.coverArt,
        browsable = true,
        playable = false,
        type = MediaMetadata.MEDIA_TYPE_ARTIST,
        extras = bundleOf(STYLE_BROWSABLE to STYLE_GRID).apply { group?.let { putString(GROUP_TITLE, it) } },
    )

    private fun playlistItem(playlist: Playlist) = build(
        id = PLAYLIST + playlist.id,
        title = playlist.name,
        subtitle = playlist.comment?.takeIf { it.isNotBlank() },
        cover = playlist.coverArt,
        browsable = true,
        playable = false,
        type = MediaMetadata.MEDIA_TYPE_PLAYLIST,
        extras = bundleOf(STYLE_PLAYABLE to STYLE_LIST),
    )

    private fun songItem(song: Song, parent: String, group: String? = null) = build(
        id = "$SONG$parent|${song.id}",
        title = song.title,
        subtitle = listOfNotNull(song.artist, song.album).joinToString(" · ").ifEmpty { null },
        cover = song.coverArt,
        browsable = false,
        playable = true,
        type = MediaMetadata.MEDIA_TYPE_MUSIC,
        extras = group?.let { bundleOf(GROUP_TITLE to it) },
    )

    private fun action(id: String, title: String) = build(
        id = id,
        title = title,
        browsable = false,
        playable = true,
        type = MediaMetadata.MEDIA_TYPE_PLAYLIST,
    )

    /** Een regel tekst zonder actie, bijvoorbeeld "log eerst in". */
    private fun message(id: String, title: String) = build(
        id = id,
        title = title,
        browsable = false,
        playable = false,
        type = MediaMetadata.MEDIA_TYPE_MIXED,
    )

    private fun build(
        id: String,
        title: String,
        browsable: Boolean,
        playable: Boolean,
        type: Int,
        subtitle: String? = null,
        cover: String? = null,
        extras: Bundle? = null,
    ): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setDisplayTitle(title)
            .setSubtitle(subtitle)
            .setArtist(subtitle)
            .setArtworkUri(ArtworkProvider.coverUri(context, cover))
            .setIsBrowsable(browsable)
            .setIsPlayable(playable)
            .setMediaType(type)
            .apply { if (extras != null) setExtras(extras) }
            .build()
        return MediaItem.Builder().setMediaId(id).setMediaMetadata(metadata).build()
    }

    companion object {
        const val ROOT = "root"
        const val HOME = "home"
        const val PLAYLISTS = "playlists"
        const val FAVORITES = "favorites"
        const val LIBRARY = "library"
        const val ARTISTS = "artists"
        const val DOWNLOADS = "downloads"
        const val SEARCH = "search"
        const val SIGN_IN = "sign-in"
        const val SHUFFLE_ALL = "shuffle:all"
        const val SHUFFLE_FAVORITES = "shuffle:favorites"
        const val ALBUM = "album:"
        const val ARTIST = "artist:"
        const val PLAYLIST = "playlist:"
        const val SONG = "song|"

        // Sleutels die Android Auto begrijpt (zie androidx.media.utils.MediaConstants).
        private const val SEARCH_SUPPORTED = "android.media.browse.SEARCH_SUPPORTED"
        private const val STYLE_BROWSABLE = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
        private const val STYLE_PLAYABLE = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"
        private const val GROUP_TITLE = "android.media.browse.CONTENT_STYLE_GROUP_TITLE_HINT"
        private const val STYLE_LIST = 1
        private const val STYLE_GRID = 2
    }
}
