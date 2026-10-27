package app.halftone.desktop.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import app.halftone.desktop.AppState
import app.halftone.desktop.data.userMessage
import app.halftone.desktop.i18n.S
import kotlinx.coroutines.CancellationException

val LocalApp = staticCompositionLocalOf<AppState> { error("AppState ontbreekt") }

sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Failed(val message: String) : Load<Nothing>
    data class Ready<T>(val data: T) : Load<T>
}

@Stable
class LoadHandle<T> internal constructor() {
    var state: Load<T> by mutableStateOf(Load.Loading)
        internal set
    internal var tick by mutableIntStateOf(0)

    /** Opnieuw laden met laadscherm. */
    fun reload() {
        state = Load.Loading
        tick++
    }

    /** Stil verversen: de huidige inhoud blijft staan tot er iets nieuws is. */
    fun refresh() {
        tick++
    }
}

/**
 * Laadt gegevens voor een scherm. Ververst stil als [refreshKey] verandert
 * (bijvoorbeeld als een afspeellijst is aangepast).
 */
@Composable
fun <T> rememberLoad(key: Any?, refreshKey: Any? = null, loader: suspend () -> T): LoadHandle<T> {
    val handle = remember(key) { LoadHandle<T>() }
    LaunchedEffect(key, refreshKey, handle.tick) {
        val result: Load<T> = try {
            Load.Ready(loader())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Load.Failed(e.userMessage())
        }
        // Bij stil verversen een fout niet over bestaande inhoud heen zetten.
        if (result is Load.Ready || handle.state !is Load.Ready) handle.state = result
    }
    return handle
}

fun formatDuration(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

fun formatTotal(seconds: Int): String {
    val minutes = seconds / 60
    return if (minutes >= 60) S.durationHoursMinutes(minutes / 60, minutes % 60) else S.durationMinutes(minutes)
}

fun formatBytes(bytes: Long): String = when {
    bytes >= 1L shl 30 -> "%.1f GB".format(bytes / (1L shl 30).toDouble())
    bytes >= 1L shl 20 -> "%.0f MB".format(bytes / (1L shl 20).toDouble())
    else -> "%.0f kB".format(bytes / 1024.0)
}
