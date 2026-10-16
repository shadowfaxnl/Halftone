package app.halftone.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class StarKind(val param: String) {
    Song("id"),
    Album("albumId"),
    Artist("artistId"),
}

/**
 * Houdt bij welke nummers, albums en artiesten een ster (hartje) hebben.
 * Wijzigingen verschijnen meteen in de UI en worden daarna naar de server gestuurd;
 * lukt dat niet, dan draaien we de wijziging terug.
 */
class Favorites(
    private val client: SubsonicClient,
    private val scope: CoroutineScope,
    private val onError: (String) -> Unit,
) {
    private val _ids = MutableStateFlow<Set<String>>(emptySet())
    val ids: StateFlow<Set<String>> = _ids.asStateFlow()

    suspend fun load(): Starred {
        val starred = client.call("getStarred2").starred2 ?: Starred()
        _ids.value = buildSet {
            starred.artist.forEach { add(it.id) }
            starred.album.forEach { add(it.id) }
            starred.song.forEach { add(it.id) }
        }
        return starred
    }

    fun refreshQuietly() {
        scope.launch { runCatching { load() } }
    }

    fun toggle(id: String, kind: StarKind) {
        val wasStarred = id in _ids.value
        _ids.update { if (wasStarred) it - id else it + id }
        scope.launch {
            try {
                client.call(if (wasStarred) "unstar" else "star", mapOf(kind.param to id))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _ids.update { if (wasStarred) it + id else it - id }
                onError(e.userMessage())
            }
        }
    }

    fun clear() {
        _ids.value = emptySet()
    }
}
