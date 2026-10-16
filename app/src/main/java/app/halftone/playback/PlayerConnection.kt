package app.halftone.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import app.halftone.data.Song
import app.halftone.data.SubsonicClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class QueueEntry(val windowIndex: Int, val item: QueueItem)

data class PlayerState(
    /** Wachtrij in afspeelvolgorde (houdt rekening met shuffle). */
    val queue: List<QueueEntry> = emptyList(),
    val current: QueueItem? = null,
    val currentIndex: Int = C.INDEX_UNSET,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
)

/**
 * De brug tussen de UI en de afspeelservice. Alle UI-acties lopen hierdoorheen;
 * de toestand komt terug als StateFlow.
 */
class PlayerConnection(
    private val context: Context,
    private val client: SubsonicClient,
    private val scope: CoroutineScope,
) {
    private val controller = CompletableDeferred<MediaController>()

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    /** Apart van [state], zodat alleen de voortgangsbalk elke tik opnieuw tekent. */
    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish(player)
    }

    fun connect() {
        scope.launch {
            val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val c = try {
                MediaController.Builder(context, token).buildAsync().await()
            } catch (e: Exception) {
                return@launch
            }
            c.addListener(listener)
            controller.complete(c)
            publish(c)
            while (isActive) {
                _position.value = c.currentPosition
                delay(if (c.isPlaying) 250L else 1000L)
            }
        }
    }

    private fun publish(p: Player) {
        val timeline = p.currentTimeline
        val order = mutableListOf<Int>()
        if (!timeline.isEmpty) {
            var i = timeline.getFirstWindowIndex(p.shuffleModeEnabled)
            while (i != C.INDEX_UNSET) {
                order += i
                i = timeline.getNextWindowIndex(i, Player.REPEAT_MODE_OFF, p.shuffleModeEnabled)
            }
        }
        val queue = order
            .filter { it < p.mediaItemCount }
            .map { QueueEntry(it, p.getMediaItemAt(it).toQueueItem()) }
        val index = p.currentMediaItemIndex
        val current = if (p.mediaItemCount > 0 && index in 0 until p.mediaItemCount) {
            p.getMediaItemAt(index).toQueueItem()
        } else {
            null
        }
        val duration = p.duration.takeIf { it != C.TIME_UNSET && it > 0 }
            ?: ((current?.durationSec ?: 0) * 1000L)

        _state.value = PlayerState(
            queue = queue,
            current = current,
            currentIndex = index,
            isPlaying = p.isPlaying,
            isBuffering = p.playbackState == Player.STATE_BUFFERING,
            durationMs = duration,
            shuffle = p.shuffleModeEnabled,
            repeatMode = p.repeatMode,
        )
        _position.value = p.currentPosition
    }

    private fun withController(block: (MediaController) -> Unit) {
        scope.launch { block(controller.await()) }
    }

    fun playSongs(songs: List<Song>, startIndex: Int = 0) = withController { c ->
        if (songs.isEmpty()) return@withController
        c.shuffleModeEnabled = false
        c.setMediaItems(
            songs.map { it.toMediaItem(client) },
            startIndex.coerceIn(songs.indices),
            0L,
        )
        c.prepare()
        c.play()
    }

    fun playNext(song: Song) = withController { c ->
        if (c.mediaItemCount == 0) {
            playSongs(listOf(song))
        } else {
            c.addMediaItem(c.currentMediaItemIndex + 1, song.toMediaItem(client))
        }
    }

    fun addToQueue(song: Song) = withController { c ->
        if (c.mediaItemCount == 0) playSongs(listOf(song)) else c.addMediaItem(song.toMediaItem(client))
    }

    fun togglePlay() = withController { c ->
        when {
            c.isPlaying -> c.pause()
            else -> {
                if (c.playbackState == Player.STATE_IDLE) c.prepare()
                if (c.playbackState == Player.STATE_ENDED) c.seekToDefaultPosition(0)
                c.play()
            }
        }
    }

    fun next() = withController { it.seekToNext() }

    fun previous() = withController { it.seekToPrevious() }

    fun seekTo(positionMs: Long) = withController { c ->
        c.seekTo(positionMs)
        _position.value = positionMs
    }

    fun skipTo(windowIndex: Int) = withController { c ->
        if (windowIndex in 0 until c.mediaItemCount) {
            c.seekToDefaultPosition(windowIndex)
            c.play()
        }
    }

    fun removeAt(windowIndex: Int) = withController { c ->
        if (windowIndex in 0 until c.mediaItemCount) c.removeMediaItem(windowIndex)
    }

    fun toggleShuffle() = withController { it.shuffleModeEnabled = !it.shuffleModeEnabled }

    fun cycleRepeat() = withController { c ->
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun stopAndClear() = withController { c ->
        c.stop()
        c.clearMediaItems()
    }
}
