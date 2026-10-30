package app.halftone.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import app.halftone.R
import app.halftone.Str

/*
 * Subsonic JSON-modellen (API 1.16.1, zoals Navidrome die levert).
 * Alles wat niet strikt nodig is heeft een standaardwaarde, zodat
 * servers die velden weglaten de app niet laten struikelen.
 */

@Serializable
data class Envelope(
    @SerialName("subsonic-response") val response: SubsonicResponse,
)

@Serializable
data class SubsonicResponse(
    val status: String,
    val version: String? = null,
    val type: String? = null,
    val serverVersion: String? = null,
    val error: SubsonicError? = null,
    val artists: ArtistsIndex? = null,
    val artist: ArtistWithAlbums? = null,
    val album: AlbumWithSongs? = null,
    val albumList2: AlbumList? = null,
    val searchResult3: SearchResult? = null,
    val randomSongs: SongList? = null,
    val starred2: Starred? = null,
    val playlists: PlaylistIndex? = null,
    val playlist: PlaylistWithSongs? = null,
    val song: Song? = null,
)

@Serializable
data class SubsonicError(
    val code: Int = 0,
    val message: String? = null,
)

@Serializable
data class ArtistsIndex(
    val index: List<IndexEntry> = emptyList(),
)

@Serializable
data class IndexEntry(
    val name: String = "",
    val artist: List<Artist> = emptyList(),
)

@Serializable
data class Artist(
    val id: String,
    val name: String = Str.get(R.string.unknown_artist),
    val coverArt: String? = null,
    val albumCount: Int = 0,
    val artistImageUrl: String? = null,
)

@Serializable
data class ArtistWithAlbums(
    val id: String,
    val name: String = Str.get(R.string.unknown_artist),
    val coverArt: String? = null,
    val albumCount: Int = 0,
    val album: List<Album> = emptyList(),
)

@Serializable
data class Album(
    val id: String,
    val name: String = Str.get(R.string.unknown_album),
    val artist: String? = null,
    val artistId: String? = null,
    val coverArt: String? = null,
    val songCount: Int = 0,
    val duration: Int = 0,
    val year: Int? = null,
    val genre: String? = null,
)

@Serializable
data class AlbumWithSongs(
    val id: String,
    val name: String = Str.get(R.string.unknown_album),
    val artist: String? = null,
    val artistId: String? = null,
    val coverArt: String? = null,
    val songCount: Int = 0,
    val duration: Int = 0,
    val year: Int? = null,
    val genre: String? = null,
    val song: List<Song> = emptyList(),
)

@Serializable
data class Song(
    val id: String,
    val title: String = Str.get(R.string.unknown_song),
    val album: String? = null,
    val albumId: String? = null,
    val artist: String? = null,
    val artistId: String? = null,
    val track: Int? = null,
    val discNumber: Int? = null,
    val year: Int? = null,
    val duration: Int = 0,
    val coverArt: String? = null,
    val suffix: String? = null,
    val bitRate: Int? = null,
)

@Serializable
data class AlbumList(
    val album: List<Album> = emptyList(),
)

@Serializable
data class SearchResult(
    val artist: List<Artist> = emptyList(),
    val album: List<Album> = emptyList(),
    val song: List<Song> = emptyList(),
) {
    val isEmpty: Boolean get() = artist.isEmpty() && album.isEmpty() && song.isEmpty()
}

@Serializable
data class SongList(
    val song: List<Song> = emptyList(),
)

@Serializable
data class Starred(
    val artist: List<Artist> = emptyList(),
    val album: List<Album> = emptyList(),
    val song: List<Song> = emptyList(),
) {
    val isEmpty: Boolean get() = artist.isEmpty() && album.isEmpty() && song.isEmpty()
}

@Serializable
data class PlaylistIndex(
    val playlist: List<Playlist> = emptyList(),
)

@Serializable
data class Playlist(
    val id: String,
    val name: String = Str.get(R.string.untitled_playlist),
    val comment: String? = null,
    val owner: String? = null,
    @SerialName("public") val isPublic: Boolean = false,
    val songCount: Int = 0,
    val duration: Int = 0,
    val coverArt: String? = null,
)

@Serializable
data class PlaylistWithSongs(
    val id: String,
    val name: String = Str.get(R.string.untitled_playlist),
    val comment: String? = null,
    val owner: String? = null,
    @SerialName("public") val isPublic: Boolean = false,
    val songCount: Int = 0,
    val duration: Int = 0,
    val coverArt: String? = null,
    val entry: List<Song> = emptyList(),
)
