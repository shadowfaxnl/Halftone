package app.halftone.ui.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.halftone.data.MusicRepository
import app.halftone.data.SearchResult
import app.halftone.data.userMessage
import app.halftone.ui.Navigator
import app.halftone.ui.common.AlbumCard
import app.halftone.ui.common.ArtistAvatar
import app.halftone.ui.common.CoverArt
import app.halftone.ui.common.ErrorView
import app.halftone.ui.common.LoadingView
import app.halftone.ui.common.LocalAppContainer
import app.halftone.ui.common.SectionHeader
import app.halftone.ui.common.SongRow
import app.halftone.ui.common.appViewModel
import app.halftone.ui.common.pressable
import app.halftone.ui.common.songMenu
import app.halftone.ui.theme.MistDim
import app.halftone.ui.theme.MistFaint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.compose.ui.res.stringResource
import app.halftone.R

sealed interface SearchUi {
    data object Idle : SearchUi
    data object Loading : SearchUi
    data class Failed(val message: String) : SearchUi
    data class Results(val query: String, val result: SearchResult) : SearchUi
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(private val repository: MusicRepository) : ViewModel() {
    val query = MutableStateFlow("")
    private val retryTick = MutableStateFlow(0)

    fun retry() {
        retryTick.value++
    }

    private val settledQuery = query
        .map { it.trim() }
        .debounce { if (it.isEmpty()) 0L else 320L }
        .distinctUntilChanged()

    val ui: StateFlow<SearchUi> = combine(settledQuery, retryTick) { q, _ -> q }
        .flatMapLatest { q ->
            if (q.length < 2) {
                flowOf(SearchUi.Idle)
            } else {
                flow {
                    emit(SearchUi.Loading)
                    emit(
                        try {
                            SearchUi.Results(q, repository.search(q))
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Throwable) {
                            SearchUi.Failed(e.userMessage())
                        },
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUi.Idle)
}

@Composable
fun SearchScreen(navigator: Navigator, padding: PaddingValues) {
    val vm = appViewModel(key = "search") { SearchViewModel(it.repository) }
    val query by vm.query.collectAsStateWithLifecycle()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
        ) {
            Text(stringResource(R.string.search), style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(16.dp))
            TextField(
                value = query,
                onValueChange = { vm.query.value = it },
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { vm.query.value = "" }) {
                            Icon(Icons.Rounded.Close, stringResource(R.string.clear))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.09f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        AnimatedContent(
            targetState = ui,
            contentKey = { it::class },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "search",
        ) { state ->
            when (state) {
                SearchUi.Idle -> Hint(stringResource(R.string.search_idle), Icons.Rounded.Search)
                SearchUi.Loading -> LoadingView()
                is SearchUi.Failed -> ErrorView(state.message, onRetry = vm::retry)
                is SearchUi.Results ->
                    if (state.result.isEmpty) {
                        Hint(stringResource(R.string.search_none, state.query), Icons.Rounded.TravelExplore)
                    } else {
                        Results(state.result, navigator, padding)
                    }
            }
        }
    }
}

@Composable
private fun Hint(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = MistFaint, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(14.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge, color = MistDim, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun Results(result: SearchResult, navigator: Navigator, padding: PaddingValues) {
    val container = LocalAppContainer.current
    val playerState by container.player.state.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 24.dp),
    ) {
        if (result.artist.isNotEmpty()) {
            item {
                Column {
                    SectionHeader(stringResource(R.string.artists))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        items(result.artist, key = { it.id }) { artist ->
                            Column(
                                Modifier
                                    .width(96.dp)
                                    .pressable { navigator.artist(artist.id) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                ArtistAvatar(artist.coverArt, Modifier.size(96.dp))
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    artist.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
        if (result.album.isNotEmpty()) {
            item {
                Column {
                    Spacer(Modifier.height(8.dp))
                    SectionHeader(stringResource(R.string.albums))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(result.album, key = { it.id }) { album ->
                            AlbumCard(album, onClick = { navigator.album(album.id) }, modifier = Modifier.width(140.dp))
                        }
                    }
                }
            }
        }
        if (result.song.isNotEmpty()) {
            item {
                Column {
                    Spacer(Modifier.height(8.dp))
                    SectionHeader(stringResource(R.string.songs))
                }
            }
            itemsIndexed(result.song, key = { i, s -> "$i-${s.id}" }) { index, song ->
                val isCurrent = playerState.current?.mediaId == song.id
                SongRow(
                    title = song.title,
                    subtitle = listOfNotNull(song.artist, song.album).joinToString(" · "),
                    durationSec = song.duration,
                    isCurrent = isCurrent,
                    songId = song.id,
                    onClick = { container.player.playSongs(result.song, index) },
                    menu = songMenu(song, onOpenAlbum = navigator::album, onOpenArtist = navigator::artist),
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    CoverArt(song.coverArt, Modifier.size(46.dp), shape = MaterialTheme.shapes.small)
                }
            }
        }
    }
}
