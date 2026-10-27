@file:OptIn(FlowPreview::class)

package app.halftone.desktop.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.halftone.desktop.data.SearchResult
import app.halftone.desktop.data.userMessage
import app.halftone.desktop.i18n.S
import app.halftone.desktop.ui.common.AlbumCard
import app.halftone.desktop.ui.common.ArtistAvatar
import app.halftone.desktop.ui.common.CoverArt
import app.halftone.desktop.ui.common.EmptyView
import app.halftone.desktop.ui.common.LocalApp
import app.halftone.desktop.ui.common.SectionHeader
import app.halftone.desktop.ui.common.SongRow
import app.halftone.desktop.ui.common.observe
import app.halftone.desktop.ui.common.pressable
import app.halftone.desktop.ui.common.songMenu
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private sealed interface SearchUi {
    data object Idle : SearchUi
    data object Loading : SearchUi
    data class Failed(val message: String) : SearchUi
    data class Results(val query: String, val result: SearchResult) : SearchUi
}

/** De zoekopdracht blijft bewaard als je even naar een album springt en terugkomt. */
private var lastQuery by mutableStateOf("")

@Composable
fun SearchScreen() {
    val app = LocalApp.current
    var ui by remember { mutableStateOf<SearchUi>(SearchUi.Idle) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { focus.requestFocus() }
        snapshotFlow { lastQuery.trim() }
            .debounce(250)
            .distinctUntilChanged()
            .collectLatest { q ->
                if (q.length < 2) {
                    ui = SearchUi.Idle
                    return@collectLatest
                }
                ui = SearchUi.Loading
                ui = try {
                    SearchUi.Results(q, app.repository.search(q))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    SearchUi.Failed(e.userMessage())
                }
            }
    }

    Column(Modifier.fillMaxSize()) {
        TextField(
            value = lastQuery,
            onValueChange = { lastQuery = it },
            placeholder = { Text(S.searchHint) },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = {
                if (lastQuery.isNotEmpty()) {
                    IconButton(onClick = { lastQuery = "" }) { Icon(Icons.Rounded.Close, S.clear) }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.09f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 28.dp, bottom = 8.dp)
                .widthIn(max = 640.dp).fillMaxWidth()
                .focusRequester(focus)
                .onFocusChanged { app.typing = it.isFocused },
        )

        when (val s = ui) {
            SearchUi.Idle -> EmptyView(Icons.Rounded.Search, S.searchIdle, "")
            SearchUi.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            is SearchUi.Failed -> EmptyView(Icons.Rounded.TravelExplore, s.message, "")
            is SearchUi.Results -> if (s.result.isEmpty) {
                EmptyView(Icons.Rounded.TravelExplore, S.searchNone(s.query), "")
            } else {
                Results(s.result)
            }
        }
    }
}

@Composable
private fun Results(result: SearchResult) {
    val app = LocalApp.current
    val ps = app.player.state.observe()
    LazyVerticalGrid(GridCells.Adaptive(180.dp), Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 32.dp)) {
        if (result.song.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { SectionHeader(S.songs, Modifier.padding(start = 0.dp)) }
            result.song.take(12).forEachIndexed { index, song ->
                item(key = "s-$index-${song.id}", span = { GridItemSpan(maxLineSpan) }) {
                    SongRow(
                        song = song,
                        isCurrent = ps.current?.id == song.id,
                        subtitle = listOfNotNull(song.artist, song.album).joinToString(" · "),
                        onClick = { app.play(result.song, index) },
                        menu = songMenu(song),
                    ) { CoverArt(song.coverArt, Modifier.size(40.dp), shape = MaterialTheme.shapes.small) }
                }
            }
        }
        if (result.artist.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { SectionHeader(S.artists) }
            items(result.artist, key = { "a-" + it.id }) { a ->
                Column(Modifier.padding(10.dp).pressable { app.nav.artist(a.id) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    ArtistAvatar(a.coverArt, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text(a.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (result.album.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { SectionHeader(S.albums) }
            items(result.album, key = { "al-" + it.id }) { a -> AlbumCard(a, onClick = { app.nav.album(a.id) }) }
        }
    }
}
