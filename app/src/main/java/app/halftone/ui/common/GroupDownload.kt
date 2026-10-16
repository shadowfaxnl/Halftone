package app.halftone.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.halftone.data.Song
import app.halftone.download.GroupDownloadState

data class GroupDownload(val state: GroupDownloadState, val progress: Float?)

/** Downloadtoestand en gemiddelde voortgang van een groep nummers. */
@Composable
fun rememberGroupDownload(songs: List<Song>): GroupDownload {
    val downloads = LocalAppContainer.current.downloads
    val items by downloads.items.collectAsStateWithLifecycle()
    val ids = remember(songs) { songs.map { it.id } }
    return remember(ids, items) {
        val state = downloads.groupState(ids, items)
        val progress = if (state == GroupDownloadState.Active && ids.isNotEmpty()) {
            ids.sumOf { id ->
                val item = items[id]
                when {
                    item == null -> 0.0
                    item.isDone -> 1.0
                    else -> (item.progress ?: 0f).toDouble()
                }
            }.toFloat() / ids.size
        } else {
            null
        }
        GroupDownload(state, progress)
    }
}
