package app.halftone.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.halftone.ui.Navigator
import app.halftone.ui.common.LoadContent
import app.halftone.ui.common.LoadViewModel
import app.halftone.ui.common.LocalAppContainer
import app.halftone.ui.common.PlaylistCard
import app.halftone.ui.common.appViewModel
import app.halftone.ui.common.pressable
import app.halftone.ui.theme.MistDim
import androidx.compose.ui.res.stringResource
import app.halftone.R

@Composable
fun PlaylistsTab(navigator: Navigator, padding: PaddingValues) {
    val container = LocalAppContainer.current
    val vm = appViewModel(key = "playlists") { c ->
        LoadViewModel(refreshOn = c.repository.playlistVersion) { c.repository.playlists() }
    }
    val state by vm.state.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }

    LoadContent(state, onRetry = vm::reload) { playlists ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 4.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "new") { NewPlaylistCard(onClick = { creating = true }) }
            items(playlists, key = { it.id }) { playlist ->
                PlaylistCard(playlist, onClick = { navigator.playlist(playlist.id) })
            }
        }
    }

    if (creating) {
        PlaylistNameDialog(
            title = stringResource(R.string.new_playlist),
            confirmLabel = stringResource(R.string.create),
            onConfirm = { name ->
                creating = false
                container.launchSafely { container.repository.createPlaylist(name) }
            },
            onDismiss = { creating = false },
        )
    }
}

@Composable
private fun NewPlaylistCard(onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Column(Modifier.pressable(onClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.medium)
                .background(accent.copy(alpha = 0.10f))
                .background(Color.White.copy(alpha = 0.03f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(30.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.new_playlist), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.start_collection), style = MaterialTheme.typography.bodySmall, color = MistDim)
        Spacer(Modifier.padding(bottom = 6.dp))
    }
}
