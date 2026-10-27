package app.halftone.desktop.player

import app.halftone.desktop.data.MusicRepository
import app.halftone.desktop.data.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Meldt luistergedrag aan de server: "speelt nu" bij de start van een nummer,
 * en een scrobble na de helft of na vier minuten. Nummers korter dan 30 seconden tellen niet.
 * Wat offline niet lukt, gaat later alsnog mee.
 */
class Scrobbler(
    private val player: Player,
    private val repository: MusicRepository,
    private val isEnabled: () -> Boolean,
    private val scope: CoroutineScope,
) {
    private data class Pending(val songId: String, val startedAtMs: Long)

    @Volatile private var song: Song? = null
    @Volatile private var startedAtMs = 0L
    @Volatile private var listenedMs = 0L
    @Volatile private var submitted = false
    private val pending = ArrayDeque<Pending>()
    private var flushing = false

    fun start() {
        player.onTrackStart = { s -> trackStarted(s) }
        scope.launch(Dispatchers.Default) {
            var last = System.currentTimeMillis()
            while (isActive) {
                delay(1_000)
                val now = System.currentTimeMillis()
                if (player.state.value.isPlaying) listenedMs += (now - last).coerceIn(0L, 5_000L)
                last = now
                check()
            }
        }
    }

    private fun trackStarted(s: Song) {
        song = s
        startedAtMs = System.currentTimeMillis()
        listenedMs = 0
        submitted = false
        if (isEnabled()) {
            scope.launch(Dispatchers.IO) {
                safely { repository.scrobble(s.id, System.currentTimeMillis(), submission = false) }
            }
        }
    }

    private fun check() {
        val s = song ?: return
        if (submitted || !isEnabled()) return
        val durationMs = ((player.state.value.durationSec.takeIf { it > 0 } ?: s.duration.toDouble()) * 1000).toLong()
        if (durationMs < 30_000) return
        if (listenedMs >= minOf(durationMs / 2, 240_000L)) {
            submitted = true
            submit(Pending(s.id, startedAtMs))
        }
    }

    private fun submit(item: Pending) {
        scope.launch(Dispatchers.IO) {
            if (!safely { repository.scrobble(item.songId, item.startedAtMs, submission = true) }) {
                synchronized(pending) {
                    pending.addLast(item)
                    while (pending.size > 500) pending.removeFirst()
                }
                return@launch
            }
            synchronized(pending) {
                if (flushing) return@launch
                flushing = true
            }
            while (true) {
                val next = synchronized(pending) { pending.firstOrNull() } ?: break
                if (!safely { repository.scrobble(next.songId, next.startedAtMs, submission = true) }) break
                synchronized(pending) { pending.removeFirst() }
            }
            synchronized(pending) { flushing = false }
        }
    }

    private suspend fun safely(block: suspend () -> Unit): Boolean = try {
        block()
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        false
    }
}
