package app.halftone.playback

import android.net.Uri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import app.halftone.data.COVER_LARGE
import app.halftone.data.Song
import app.halftone.data.SubsonicClient
import app.halftone.R
import app.halftone.Str

private const val EXTRA_ALBUM_ID = "halftone.albumId"
private const val EXTRA_ARTIST_ID = "halftone.artistId"
private const val EXTRA_COVER = "halftone.coverArt"
private const val EXTRA_DURATION = "halftone.duration"

/** Wat de UI van een nummer in de wachtrij nodig heeft. */
data class QueueItem(
    val mediaId: String,
    val title: String,
    val artist: String,
    val album: String,
    val coverArt: String?,
    val albumId: String?,
    val artistId: String?,
    val durationSec: Int,
)

fun Song.toMediaItem(client: SubsonicClient): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setAlbumArtist(artist)
        .setTrackNumber(track)
        .setDiscNumber(discNumber)
        .setReleaseYear(year)
        .setArtworkUri(client.coverArtUrl(coverArt, COVER_LARGE)?.let(Uri::parse))
        .setIsBrowsable(false)
        .setIsPlayable(true)
        .setExtras(
            bundleOf(
                EXTRA_ALBUM_ID to albumId,
                EXTRA_ARTIST_ID to artistId,
                EXTRA_COVER to coverArt,
                EXTRA_DURATION to duration,
            ),
        )
        .build()
    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(client.streamUrl(id))
        .setMediaMetadata(metadata)
        .build()
}

/** Terug naar een Song, voor acties als "toevoegen aan afspeellijst" vanuit de speler. */
fun QueueItem.toSong(): Song = Song(
    id = mediaId,
    title = title,
    album = album.ifEmpty { null },
    albumId = albumId,
    artist = artist.ifEmpty { null },
    artistId = artistId,
    duration = durationSec,
    coverArt = coverArt,
)

fun MediaItem.toQueueItem(): QueueItem {
    val meta = mediaMetadata
    val extras = meta.extras
    return QueueItem(
        mediaId = mediaId,
        title = meta.title?.toString() ?: Str.get(R.string.unknown_song),
        artist = meta.artist?.toString().orEmpty(),
        album = meta.albumTitle?.toString().orEmpty(),
        coverArt = extras?.getString(EXTRA_COVER),
        albumId = extras?.getString(EXTRA_ALBUM_ID),
        artistId = extras?.getString(EXTRA_ARTIST_ID),
        durationSec = extras?.getInt(EXTRA_DURATION) ?: 0,
    )
}
