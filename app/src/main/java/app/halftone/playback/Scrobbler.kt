package app.halftone.playback

import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import app.halftone.data.MusicRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Meldt luistergedrag aan de server, volgens de gangbare regels:
 * "speelt nu" zodra een nummer begint, en een echte scrobble na de helft
 * van het nummer of na vier minuten (wat eerder komt). Nummers korter dan
 * 30 seconden tellen niet. Mislukte scrobbles (offline) worden later opnieuw verstuurd.
 */
class Scrobbler(
    private val player: Player,
    private val repository: MusicRepository,
    private val isEnabled: () -> Boolean,
    private val scope: CoroutineScope,
) : Player.Listener {

    private data class Pending(val songId: String, val startedAtMs: Long)

    private var songId: String? = null
    private var startedAtMs = 0L
    private var listenedMs = 0L
    private var nowPlayingSent = false
    private var submitted = false
    private var lastTick = 0L
    private val pending = ArrayDeque<Pending>()
    private var ticker: Job? = null
    private var flushing = false

    fun start() {
        player.addListener(this)
        resetFor(player.currentMediaItem)
        ticker = scope.launch {
            while (isActive) {
                delay(1_000)
                tick()
            }
        }
    }

    fun stop() {
        ticker?.cancel()
        player.removeListener(this)
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        resetFor(mediaItem)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        lastTick = SystemClock.elapsedRealtime()
        if (isPlaying && !nowPlayingSent) sendNowPlaying()
    }

    private fun resetFor(item: MediaItem?) {
        songId = item?.mediaId?.takeIf { it.isNotEmpty() }
        startedAtMs = System.currentTimeMillis()
        listenedMs = 0L
        nowPlayingSent = false
        submitted = false
        lastTick = SystemClock.elapsedRealtime()
        if (player.isPlaying) sendNowPlaying()
    }

    private fun tick() {
        val now = SystemClock.elapsedRealtime()
        if (player.isPlaying) listenedMs += (now - lastTick).coerceIn(0L, 5_000L)
        lastTick = now

        val id = songId ?: return
        if (submitted || !isEnabled()) return
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: return
        if (duration < 30_000) return
        val threshold = minOf(duration / 2, 240_000L)
        if (listenedMs >= threshold) {
            submitted = true
            submit(Pending(id, startedAtMs))
        }
    }

    private fun sendNowPlaying() {
        val id = songId ?: return
        if (!isEnabled()) return
        nowPlayingSent = true
        scope.launch {
            runCatchingNonCancel { repository.scrobble(id, System.currentTimeMillis(), submission = false) }
        }
    }

    private fun submit(item: Pending) {
        scope.launch {
            val ok = runCatchingNonCancel { repository.scrobble(item.songId, item.startedAtMs, submission = true) }
            if (!ok) {
                pending.addLast(item)
                while (pending.size > 500) pending.removeFirst()
                return@launch
            }
            // Gelukt, dus er is weer verbinding: de achterstand ook versturen.
            if (flushing) return@launch
            flushing = true
            while (pending.isNotEmpty()) {
                val next = pending.first()
                val sent = runCatchingNonCancel {
                    repository.scrobble(next.songId, next.startedAtMs, submission = true)
                }
                if (!sent) break
                pending.removeFirst()
            }
            flushing = false
        }
    }

    private suspend fun runCatchingNonCancel(block: suspend () -> Unit): Boolean = try {
        block()
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        false
    }
}
