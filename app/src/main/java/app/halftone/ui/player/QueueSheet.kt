package app.halftone.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.halftone.playback.PlayerState
import app.halftone.ui.common.CoverArt
import app.halftone.ui.common.EqualizerBars
import app.halftone.ui.theme.Ink2
import app.halftone.ui.theme.MistDim
import app.halftone.ui.theme.MistFaint
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import app.halftone.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    state: PlayerState,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val currentPosition = state.queue.indexOfFirst { it.windowIndex == state.currentIndex }.coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentPosition)
    val accent = MaterialTheme.colorScheme.primary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Ink2,
    ) {
        Column(Modifier.fillMaxHeight(0.85f)) {
            Row(
                Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(stringResource(R.string.queue), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.width(10.dp))
                Text(
                    pluralStringResource(R.plurals.songs_count, state.queue.size, state.queue.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MistFaint,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                items(state.queue, key = { it.windowIndex }) { entry ->
                    val isCurrent = entry.windowIndex == state.currentIndex
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(if (isCurrent) accent.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { onSelect(entry.windowIndex) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CoverArt(entry.item.coverArt, Modifier.size(46.dp), shape = MaterialTheme.shapes.small)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                entry.item.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCurrent) accent else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                entry.item.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MistDim,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (isCurrent) {
                            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                EqualizerBars(playing = state.isPlaying, color = accent)
                            }
                        } else {
                            IconButton(onClick = { onRemove(entry.windowIndex) }) {
                                Icon(Icons.Rounded.Close, stringResource(R.string.remove), tint = MistFaint)
                            }
                        }
                    }
                }
            }
        }
    }
}
