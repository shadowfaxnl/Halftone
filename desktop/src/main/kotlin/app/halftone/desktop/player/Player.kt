package app.halftone.desktop.player

import app.halftone.desktop.data.Song
import app.halftone.desktop.data.SubsonicClient
import app.halftone.desktop.download.Downloads
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class RepeatMode { Off, All, One }

data class PlayerState(
    /** Wachtrij in afspeelvolgorde; dezelfde volgorde als de afspeellijst in mpv. */
    val queue: List<Song> = emptyList(),
    val index: Int = -1,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val durationSec: Double = 0.0,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.Off,
    val volume: Int = 80,
) {
    val current: Song? get() = queue.getOrNull(index)
}

/**
 * De speler. Kotlin houdt de wachtrij bij, mpv speelt hem af.
 * Doordat mpv de hele wachtrij als afspeellijst kent, laadt hij het volgende nummer
 * vooraf en lopen nummers naadloos (gapless) in elkaar over.
 */
class Player(
    private val client: SubsonicClient,
    private val downloads: Downloads,
    initialVolume: Int,
) {
    private val mpv: Mpv? = Mpv.create("Halftone")
    val available: Boolean get() = mpv != null

    private val _state = MutableStateFlow(PlayerState(volume = initialVolume))
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _position = MutableStateFlow(0.0)
    val position: StateFlow<Double> = _position.asStateFlow()

    /** Wordt aangeroepen als er een ander nummer begint (ook bij herhalen). */
    var onTrackStart: ((Song) -> Unit)? = null
    /** Na een sprong in de tijd, voor MPRIS. */
    var onSeeked: ((Double) -> Unit)? = null

    /** Oorspronkelijke volgorde, om shuffle weer uit te kunnen zetten. */
    private var original: List<Song> = emptyList()
    private val lock = Any()

    @Volatile private var paused = false
    @Volatile private var idle = true

    init {
        mpv?.let { m ->
            m.set("volume", initialVolume.toString())
            m.observe("time-pos", Mpv.FORMAT_DOUBLE)
            m.observe("duration", Mpv.FORMAT_DOUBLE)
            m.observe("pause", Mpv.FORMAT_FLAG)
            m.observe("idle-active", Mpv.FORMAT_FLAG)
            m.observe("paused-for-cache", Mpv.FORMAT_FLAG)
            m.observe("playlist-pos", Mpv.FORMAT_INT64)
            m.startEventLoop(::onEvent)
        }
    }

    private fun onEvent(e: MpvEvent) {
        when (e) {
            is MpvEvent.Property -> when (e.name) {
                "time-pos" -> _position.value = (e.value as? Double) ?: 0.0
                "duration" -> _state.update { it.copy(durationSec = (e.value as? Double) ?: it.current?.duration?.toDouble() ?: 0.0) }
                "pause" -> {
                    paused = e.value == true
                    _state.update { it.copy(isPlaying = !paused && !idle && it.current != null) }
                }
                "paused-for-cache" -> _state.update { it.copy(isBuffering = e.value == true) }
                "idle-active" -> {
                    idle = e.value == true
                    if (idle) {
                        _state.update { it.copy(isPlaying = false, isBuffering = false) }
                        _position.value = 0.0
                    }
                }
                "playlist-pos" -> {
                    val pos = (e.value as? Long)?.toInt() ?: -1
                    if (pos >= 0) {
                        _state.update { it.copy(index = pos, durationSec = it.queue.getOrNull(pos)?.duration?.toDouble() ?: 0.0) }
                    }
                }
            }
            is MpvEvent.FileLoaded -> {
                idle = false
                _state.update { it.copy(isPlaying = !paused && it.current != null) }
                _state.value.current?.let { onTrackStart?.invoke(it) }
            }
            else -> Unit
        }
    }

    private fun urlFor(song: Song): String =
        downloads.localFile(song.id)?.absolutePath ?: client.streamUrl(song.id)

    // ───────────── Bediening ─────────────

    fun playSongs(songs: List<Song>, startIndex: Int = 0) {
        val m = mpv ?: return
        if (songs.isEmpty()) return
        val start = startIndex.coerceIn(songs.indices)
        synchronized(lock) {
            original = songs
            _state.update { it.copy(queue = songs, index = start, shuffle = false, durationSec = songs[start].duration.toDouble()) }
            m.command("stop")
            songs.forEach { m.command("loadfile", urlFor(it), "append") }
            m.set("playlist-pos", start.toString())
            m.set("pause", "no")
        }
    }

    fun playNext(song: Song) {
        val m = mpv ?: return
        synchronized(lock) {
            val s = _state.value
            if (s.queue.isEmpty()) {
                playSongs(listOf(song)); return
            }
            val target = s.index + 1
            m.command("loadfile", urlFor(song), "append")
            m.command("playlist-move", s.queue.size.toString(), target.toString())
            val q = s.queue.toMutableList().apply { add(target, song) }
            original = original + song
            _state.update { it.copy(queue = q) }
        }
    }

    fun addToQueue(song: Song) = addAllToQueue(listOf(song))

    fun addAllToQueue(songs: List<Song>) {
        val m = mpv ?: return
        if (songs.isEmpty()) return
        synchronized(lock) {
            if (_state.value.queue.isEmpty()) {
                playSongs(songs); return
            }
            songs.forEach { m.command("loadfile", urlFor(it), "append") }
            original = original + songs
            _state.update { it.copy(queue = it.queue + songs) }
        }
    }

    fun togglePlay() {
        val m = mpv ?: return
        val s = _state.value
        if (s.current == null) return
        if (s.isPlaying) {
            m.set("pause", "yes")
        } else {
            // Na het einde van de wachtrij staat mpv stil: dan het nummer opnieuw laden.
            if (idle) m.set("playlist-pos", s.index.coerceAtLeast(0).toString())
            m.set("pause", "no")
        }
    }

    fun play() { if (!_state.value.isPlaying) togglePlay() }
    fun pause() { if (_state.value.isPlaying) togglePlay() }

    fun next() {
        val m = mpv ?: return
        val s = _state.value
        if (s.index < s.queue.size - 1 || s.repeat == RepeatMode.All) m.command("playlist-next", "force")
    }

    fun previous() {
        val m = mpv ?: return
        if (_position.value > 3.0 || _state.value.index <= 0) {
            seekTo(0.0)
        } else {
            m.command("playlist-prev", "force")
        }
    }

    fun seekTo(seconds: Double) {
        val m = mpv ?: return
        m.command("seek", "%.3f".format(java.util.Locale.ROOT, seconds.coerceAtLeast(0.0)), "absolute")
        _position.value = seconds
        onSeeked?.invoke(seconds)
    }

    fun skipTo(index: Int) {
        val m = mpv ?: return
        if (index !in _state.value.queue.indices) return
        m.set("playlist-pos", index.toString())
        m.set("pause", "no")
    }

    fun removeAt(index: Int) {
        val m = mpv ?: return
        synchronized(lock) {
            val s = _state.value
            if (index !in s.queue.indices || index == s.index) return
            m.command("playlist-remove", index.toString())
            val removed = s.queue[index]
            val q = s.queue.toMutableList().apply { removeAt(index) }
            original = original.toMutableList().apply { remove(removed) }
            _state.update { it.copy(queue = q, index = if (index < it.index) it.index - 1 else it.index) }
        }
    }

    fun toggleShuffle() {
        val m = mpv ?: return
        synchronized(lock) {
            val s = _state.value
            if (s.queue.isEmpty()) {
                _state.update { it.copy(shuffle = !it.shuffle) }
                return
            }
            val history = s.queue.subList(0, s.index + 1)
            val upcoming: List<Song> = if (!s.shuffle) {
                s.queue.drop(s.index + 1).shuffled()
            } else {
                // Terug naar de oorspronkelijke volgorde, vanaf het huidige nummer.
                val cur = s.current
                val at = original.indexOfFirst { it === cur }.takeIf { it >= 0 } ?: original.indexOfFirst { it.id == cur?.id }
                val rest = if (at >= 0) original.drop(at + 1) else s.queue.drop(s.index + 1)
                val seen = history.map { System.identityHashCode(it) }.toSet()
                rest.filter { System.identityHashCode(it) !in seen }
            }
            // Alles na het huidige nummer vervangen.
            for (i in s.queue.size - 1 downTo s.index + 1) m.command("playlist-remove", i.toString())
            upcoming.forEach { m.command("loadfile", urlFor(it), "append") }
            _state.update { it.copy(queue = history + upcoming, shuffle = !s.shuffle) }
        }
    }

    fun cycleRepeat() {
        val m = mpv ?: return
        val next = when (_state.value.repeat) {
            RepeatMode.Off -> RepeatMode.All
            RepeatMode.All -> RepeatMode.One
            RepeatMode.One -> RepeatMode.Off
        }
        setRepeat(next, m)
    }

    fun setRepeat(mode: RepeatMode, m: Mpv? = mpv) {
        m ?: return
        m.set("loop-playlist", if (mode == RepeatMode.All) "inf" else "no")
        m.set("loop-file", if (mode == RepeatMode.One) "inf" else "no")
        _state.update { it.copy(repeat = mode) }
    }

    fun setShuffle(on: Boolean) {
        if (_state.value.shuffle != on) toggleShuffle()
    }

    fun setVolume(volume: Int) {
        val v = volume.coerceIn(0, 100)
        mpv?.set("volume", v.toString())
        _state.update { it.copy(volume = v) }
    }

    fun stopAndClear() {
        mpv?.command("stop")
        original = emptyList()
        _position.value = 0.0
        _state.update { PlayerState(volume = it.volume, repeat = it.repeat) }
    }

    fun shutdown() {
        mpv?.destroy()
    }
}
