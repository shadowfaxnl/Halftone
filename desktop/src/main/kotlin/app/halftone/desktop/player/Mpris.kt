@file:Suppress("FunctionName", "LocalVariableName")

package app.halftone.desktop.player

import app.halftone.desktop.data.COVER_LARGE
import app.halftone.desktop.data.Song
import app.halftone.desktop.data.SubsonicClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.interfaces.Properties
import org.freedesktop.dbus.messages.DBusSignal
import org.freedesktop.dbus.types.Variant

@DBusInterfaceName("org.mpris.MediaPlayer2")
interface MediaPlayer2 : DBusInterface {
    fun Raise()
    fun Quit()
}

@DBusInterfaceName("org.mpris.MediaPlayer2.Player")
interface MediaPlayer2Player : DBusInterface {
    fun Next()
    fun Previous()
    fun Pause()
    fun PlayPause()
    fun Stop()
    fun Play()
    fun Seek(Offset: Long)
    fun SetPosition(TrackId: DBusPath, Position: Long)
    fun OpenUri(Uri: String)

    /** Signaal na een sprong in de tijd, zodat desktops de voortgangsbalk bijwerken. */
    class Seeked(path: String, val Position: Long) : DBusSignal(path, Position)
}

/**
 * MPRIS: de standaard waarmee Linux-desktops mediaspelers bedienen.
 * Hiermee werken de mediatoetsen op je toetsenbord, het mediapaneel van GNOME en KDE,
 * koptelefoonknoppen en tools als playerctl.
 * Lukt het niet (geen sessiebus), dan werkt de app gewoon zonder.
 */
class Mpris(
    private val player: Player,
    private val client: SubsonicClient,
    private val scope: CoroutineScope,
    private val onRaise: () -> Unit,
    private val onQuit: () -> Unit,
) {
    private var connection: DBusConnection? = null
    private val obj = MprisObject()

    fun start() {
        runCatching {
            val conn = DBusConnectionBuilder.forSessionBus().build()
            conn.requestBusName(BUS_NAME)
            conn.exportObject(PATH, obj)
            connection = conn
        }.onFailure { return }

        scope.launch(Dispatchers.Default) {
            player.state
                .map { Triple(it.current?.id, it.isPlaying, it.durationSec.toLong()) }
                .distinctUntilChanged()
                .collect { emit("Metadata", "PlaybackStatus", "CanGoNext", "CanGoPrevious", "CanPlay") }
        }
        scope.launch(Dispatchers.Default) {
            player.state.map { Triple(it.shuffle, it.repeat, it.volume) }.distinctUntilChanged()
                .collect { emit("Shuffle", "LoopStatus", "Volume") }
        }
        player.onSeeked = { sec ->
            runCatching { connection?.sendMessage(MediaPlayer2Player.Seeked(PATH, (sec * 1_000_000).toLong())) }
        }
    }

    fun stop() {
        runCatching { connection?.releaseBusName(BUS_NAME) }
        runCatching { connection?.close() }
        connection = null
    }

    private fun emit(vararg names: String) {
        val conn = connection ?: return
        runCatching {
            val changed: Map<String, Variant<*>> = names.associateWith { playerProperty(it) }
            conn.sendMessage(Properties.PropertiesChanged(PATH, PLAYER_IFACE, changed, emptyList()))
        }
    }

    // ───────────── Eigenschappen ─────────────

    private fun trackPath(song: Song?): DBusPath =
        if (song == null) {
            DBusPath("/org/mpris/MediaPlayer2/TrackList/NoTrack")
        } else {
            DBusPath("/app/halftone/track/" + song.id.replace(Regex("[^A-Za-z0-9_]"), "_").ifEmpty { "x" })
        }

    private fun metadata(): Variant<*> {
        val s = player.state.value
        val song = s.current
        val map = linkedMapOf<String, Variant<*>>("mpris:trackid" to Variant(trackPath(song)))
        if (song != null) {
            val lengthUs = ((s.durationSec.takeIf { it > 0 } ?: song.duration.toDouble()) * 1_000_000).toLong()
            map["mpris:length"] = Variant(lengthUs)
            map["xesam:title"] = Variant(song.title)
            song.album?.let { map["xesam:album"] = Variant(it) }
            song.artist?.let { map["xesam:artist"] = Variant(listOf(it), "as") }
            song.track?.let { map["xesam:trackNumber"] = Variant(it) }
            client.coverArtUrl(song.coverArt, COVER_LARGE)?.let { map["mpris:artUrl"] = Variant(it) }
        }
        return Variant(map, "a{sv}")
    }

    private fun playerProperty(name: String): Variant<*> {
        val s = player.state.value
        return when (name) {
            "PlaybackStatus" -> Variant(
                when {
                    s.current == null -> "Stopped"
                    s.isPlaying -> "Playing"
                    else -> "Paused"
                },
            )
            "LoopStatus" -> Variant(
                when (s.repeat) {
                    RepeatMode.Off -> "None"
                    RepeatMode.All -> "Playlist"
                    RepeatMode.One -> "Track"
                },
            )
            "Rate", "MinimumRate", "MaximumRate" -> Variant(1.0)
            "Shuffle" -> Variant(s.shuffle)
            "Metadata" -> metadata()
            "Volume" -> Variant(s.volume / 100.0)
            "Position" -> Variant((player.position.value * 1_000_000).toLong())
            "CanGoNext" -> Variant(s.index < s.queue.size - 1 || s.repeat == RepeatMode.All)
            "CanGoPrevious" -> Variant(s.current != null)
            "CanPlay", "CanPause", "CanSeek" -> Variant(s.current != null)
            "CanControl" -> Variant(true)
            else -> Variant("")
        }
    }

    private fun rootProperty(name: String): Variant<*> = when (name) {
        "CanQuit", "CanRaise" -> Variant(true)
        "HasTrackList", "Fullscreen", "CanSetFullscreen" -> Variant(false)
        "Identity" -> Variant("Halftone")
        "DesktopEntry" -> Variant("halftone")
        "SupportedUriSchemes" -> Variant(listOf<String>(), "as")
        "SupportedMimeTypes" -> Variant(listOf<String>(), "as")
        else -> Variant("")
    }

    private val playerNames = listOf(
        "PlaybackStatus", "LoopStatus", "Rate", "Shuffle", "Metadata", "Volume", "Position",
        "MinimumRate", "MaximumRate", "CanGoNext", "CanGoPrevious", "CanPlay", "CanPause", "CanSeek", "CanControl",
    )
    private val rootNames = listOf(
        "CanQuit", "CanRaise", "HasTrackList", "Fullscreen", "CanSetFullscreen",
        "Identity", "DesktopEntry", "SupportedUriSchemes", "SupportedMimeTypes",
    )

    private inner class MprisObject : MediaPlayer2, MediaPlayer2Player, Properties {
        override fun getObjectPath(): String = PATH
        override fun isRemote(): Boolean = false

        override fun Raise() = onRaise()
        override fun Quit() = onQuit()

        override fun Next() = player.next()
        override fun Previous() = player.previous()
        override fun Pause() = player.pause()
        override fun PlayPause() = player.togglePlay()
        override fun Stop() = player.pause()
        override fun Play() = player.play()
        override fun Seek(Offset: Long) {
            player.seekTo(player.position.value + Offset / 1_000_000.0)
        }
        override fun SetPosition(TrackId: DBusPath, Position: Long) {
            if (TrackId.path == trackPath(player.state.value.current).path) player.seekTo(Position / 1_000_000.0)
        }
        override fun OpenUri(Uri: String) = Unit

        @Suppress("UNCHECKED_CAST")
        override fun <A> Get(interfaceName: String, propertyName: String): A =
            (if (interfaceName == PLAYER_IFACE) playerProperty(propertyName) else rootProperty(propertyName)) as A

        override fun <A> Set(interfaceName: String, propertyName: String, value: A) {
            val raw = if (value is Variant<*>) value.value else value
            when (propertyName) {
                "Volume" -> (raw as? Double)?.let { player.setVolume((it * 100).toInt()) }
                "Shuffle" -> (raw as? Boolean)?.let { player.setShuffle(it) }
                "LoopStatus" -> when (raw as? String) {
                    "None" -> player.setRepeat(RepeatMode.Off)
                    "Playlist" -> player.setRepeat(RepeatMode.All)
                    "Track" -> player.setRepeat(RepeatMode.One)
                }
            }
        }

        override fun GetAll(interfaceName: String): Map<String, Variant<*>> =
            if (interfaceName == PLAYER_IFACE) {
                playerNames.associateWith { playerProperty(it) }
            } else {
                rootNames.associateWith { rootProperty(it) }
            }
    }

    companion object {
        private const val BUS_NAME = "org.mpris.MediaPlayer2.halftone"
        private const val PATH = "/org/mpris/MediaPlayer2"
        private const val PLAYER_IFACE = "org.mpris.MediaPlayer2.Player"
    }
}
