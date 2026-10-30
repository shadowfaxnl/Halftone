package app.halftone.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.halftone.AppContainer
import app.halftone.data.userMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import app.halftone.R
import app.halftone.Str

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer ontbreekt")
}

/** Maakt een ViewModel die toegang heeft tot de [AppContainer]. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    key: String? = null,
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = LocalAppContainer.current
    return viewModel(
        key = key,
        factory = viewModelFactory { initializer { create(container) } },
    )
}

sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Failed(val message: String) : Load<Nothing>
    data class Ready<T>(val data: T) : Load<T>
}

/**
 * Een ViewModel die één ding ophaalt, met laden, fout en verversen.
 * Geef [refreshOn] mee om stil te verversen zodra die flow iets nieuws uitzendt
 * (bijvoorbeeld als een afspeellijst is aangepast).
 */
class LoadViewModel<T>(
    refreshOn: Flow<*>? = null,
    private val loader: suspend () -> T,
) : ViewModel() {
    private val _state = MutableStateFlow<Load<T>>(Load.Loading)
    val state: StateFlow<Load<T>> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    init {
        reload()
        if (refreshOn != null) {
            viewModelScope.launch {
                refreshOn.drop(1).collect { quietRefresh() }
            }
        }
    }

    private fun quietRefresh() {
        viewModelScope.launch {
            val result = attempt()
            if (result is Load.Ready) _state.value = result
        }
    }

    fun reload() {
        viewModelScope.launch {
            _state.value = Load.Loading
            _state.value = attempt()
        }
    }

    /** Ververst zonder de huidige inhoud weg te halen. */
    fun refresh() {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            val result = attempt()
            if (result is Load.Ready || _state.value !is Load.Ready) _state.value = result
            _refreshing.value = false
        }
    }

    private suspend fun attempt(): Load<T> = try {
        Load.Ready(loader())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Load.Failed(e.userMessage())
    }
}

fun formatDuration(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

fun formatMs(ms: Long): String = formatDuration((ms / 1000).toInt())

fun formatBytes(bytes: Long): String = when {
    bytes >= 1L shl 30 -> "%.1f GB".format(bytes / (1L shl 30).toDouble())
    bytes >= 1L shl 20 -> "%.0f MB".format(bytes / (1L shl 20).toDouble())
    else -> "%.0f kB".format(bytes / 1024.0)
}

fun formatTotal(seconds: Int): String {
    val minutes = seconds / 60
    return if (minutes >= 60) {
        Str.get(R.string.duration_h_min, minutes / 60, minutes % 60)
    } else {
        Str.get(R.string.duration_min, minutes)
    }
}
