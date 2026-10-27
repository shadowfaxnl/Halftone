@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)

package app.halftone.desktop.ui.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.halftone.desktop.data.Album
import app.halftone.desktop.data.COVER_SMALL
import app.halftone.desktop.data.Playlist
import app.halftone.desktop.data.Song
import app.halftone.desktop.data.StarKind
import app.halftone.desktop.download.GroupDownloadState
import app.halftone.desktop.i18n.S
import app.halftone.desktop.ui.theme.Ink3
import app.halftone.desktop.ui.theme.Ink4
import app.halftone.desktop.ui.theme.MistDim
import app.halftone.desktop.ui.theme.MistFaint
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.runtime.collectAsState

/** collectAsState met een vaste schrijfwijze, zodat schermen er kort uitzien. */
@Composable
fun <T> StateFlow<T>.observe(): T = collectAsState().value

// ───────────────────────── Hoesjes ─────────────────────────

@Composable
fun CoverArt(
    coverArt: String?,
    modifier: Modifier = Modifier,
    size: Int = COVER_SMALL,
    shape: Shape = MaterialTheme.shapes.medium,
    placeholderIcon: ImageVector = Icons.Rounded.Album,
) {
    val client = LocalApp.current.client
    val url = remember(coverArt, size) { client.coverArtUrl(coverArt, size) }
    Box(
        modifier.clip(shape).background(Brush.linearGradient(listOf(Ink4, Ink3))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(placeholderIcon, null, tint = MistFaint, modifier = Modifier.fillMaxSize(0.38f))
        if (url != null) {
            AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}

/** Kaart met hoesje; bij aanwijzen verschijnt een afspeelknop. */
@Composable
fun MediaCard(
    coverArt: String?,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onPlay: (() -> Unit)? = null,
    placeholderIcon: ImageVector = Icons.Rounded.Album,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg by animateFloatAsState(if (hovered) 0.06f else 0f, label = "cardBg")
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(Color.White.copy(alpha = bg))
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(10.dp),
    ) {
        Box {
            CoverArt(
                coverArt,
                Modifier.fillMaxWidth().aspectRatio(1f).shadow(10.dp, MaterialTheme.shapes.medium, clip = false),
                placeholderIcon = placeholderIcon,
            )
            if (onPlay != null) {
                val alpha by animateFloatAsState(if (hovered) 1f else 0f, tween(160), label = "playFab")
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .scale(0.85f + 0.15f * alpha)
                        .size(46.dp)
                        .shadow(10.dp * alpha, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
                        .clickable(enabled = hovered, onClick = onPlay),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.PlayArrow, S.play, tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = alpha), modifier = Modifier.size(28.dp))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (!subtitle.isNullOrEmpty()) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MistDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun AlbumCard(album: Album, onClick: () -> Unit, modifier: Modifier = Modifier, showArtist: Boolean = true) {
    val app = LocalApp.current
    MediaCard(
        coverArt = album.coverArt,
        title = album.name,
        subtitle = listOfNotNull(album.artist.takeIf { showArtist }, album.year?.toString()).joinToString(" · "),
        onClick = onClick,
        onPlay = { app.playAlbum(album.id) },
        modifier = modifier,
    )
}

@Composable
fun PlaylistCard(playlist: Playlist, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val app = LocalApp.current
    MediaCard(
        coverArt = playlist.coverArt,
        title = playlist.name,
        subtitle = S.songsCount(playlist.songCount),
        onClick = onClick,
        onPlay = { app.launchSafely { app.play(app.repository.playlist(playlist.id).entry) } },
        modifier = modifier,
        placeholderIcon = Icons.AutoMirrored.Rounded.QueueMusic,
    )
}

@Composable
fun ArtistAvatar(coverArt: String?, modifier: Modifier = Modifier) {
    CoverArt(coverArt, modifier.aspectRatio(1f), shape = CircleShape, placeholderIcon = Icons.Rounded.Person)
}

// ───────────────────────── Nummers ─────────────────────────

data class MenuAction(val label: String, val icon: ImageVector, val onClick: () -> Unit)

/** Het menu bij een nummer; ook te openen met de rechtermuisknop. */
@Composable
fun songMenu(song: Song, extra: List<MenuAction> = emptyList()): List<MenuAction> {
    val app = LocalApp.current
    val starred = app.favorites.ids.observe()
    val downloads = app.downloads.items.observe()
    val isStarred = song.id in starred
    val dl = downloads[song.id]
    return buildList {
        add(MenuAction(S.playNext, Icons.AutoMirrored.Rounded.QueueMusic) { app.player.playNext(song); app.toast(S.playingNext) })
        add(MenuAction(S.addToQueue, Icons.AutoMirrored.Rounded.QueueMusic) { app.player.addToQueue(song); app.toast(S.addedToQueue) })
        add(MenuAction(S.addToPlaylist, Icons.AutoMirrored.Rounded.PlaylistAdd) { app.addToPlaylist = listOf(song) })
        add(
            MenuAction(
                if (isStarred) S.removeFromFavorites else S.addToFavorites,
                if (isStarred) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            ) { app.favorites.toggle(song.id, StarKind.Song) },
        )
        if (dl == null || dl.isFailed) {
            add(MenuAction(S.download, Icons.Rounded.Download) { app.downloads.download(listOf(song)) })
        } else {
            add(MenuAction(S.removeDownload, Icons.Rounded.Delete) { app.downloads.remove(listOf(song.id)) })
        }
        song.albumId?.let { id -> add(MenuAction(S.goToAlbum, Icons.Rounded.Album) { app.nav.album(id) }) }
        song.artistId?.let { id -> add(MenuAction(S.goToArtist, Icons.Rounded.Person) { app.nav.artist(id) }) }
        addAll(extra)
    }
}

/** Een rij in een nummerlijst: klik speelt af, rechtsklik opent het menu. */
@Composable
fun SongRow(
    song: Song,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = song.artist,
    menu: List<MenuAction> = emptyList(),
    leading: @Composable () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(
                when {
                    isCurrent -> accent.copy(alpha = 0.10f)
                    hovered -> Color.White.copy(alpha = 0.05f)
                    else -> Color.Transparent
                },
            )
            .hoverable(interaction)
            .onPointerEvent(PointerEventType.Press) { if (it.buttons.isSecondaryPressed && menu.isNotEmpty()) menuOpen = true }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isCurrent) accent else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                SongBadges(song.id)
                if (!subtitle.isNullOrBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MistDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Text(formatDuration(song.duration), style = MaterialTheme.typography.bodySmall, color = MistFaint, modifier = Modifier.padding(horizontal = 10.dp))
        if (menu.isNotEmpty()) {
            OverflowMenu(menu, expanded = menuOpen, onExpandedChange = { menuOpen = it }, visible = hovered || menuOpen)
        }
    }
}

@Composable
private fun SongBadges(songId: String) {
    val app = LocalApp.current
    val starred = app.favorites.ids.observe()
    val downloads = app.downloads.items.observe()
    val accent = MaterialTheme.colorScheme.primary
    val dl = downloads[songId]
    if (songId in starred) {
        Icon(Icons.Rounded.Favorite, S.favorite, tint = accent, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(5.dp))
    }
    when {
        dl?.isDone == true -> {
            Icon(Icons.Rounded.DownloadDone, S.downloaded, tint = accent, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(5.dp))
        }
        dl?.isActive == true -> {
            val p = dl.progress
            if (p != null) {
                CircularProgressIndicator(progress = { p }, color = accent, trackColor = Color.White.copy(alpha = 0.15f), strokeWidth = 1.6.dp, modifier = Modifier.size(11.dp))
            } else {
                CircularProgressIndicator(color = accent, strokeWidth = 1.6.dp, modifier = Modifier.size(11.dp))
            }
            Spacer(Modifier.width(6.dp))
        }
    }
}

@Composable
fun TrackNumber(number: Int?, isCurrent: Boolean, isPlaying: Boolean) {
    Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) {
        if (isCurrent) {
            EqualizerBars(playing = isPlaying, color = MaterialTheme.colorScheme.primary)
        } else {
            Text(number?.toString() ?: "•", style = MaterialTheme.typography.bodyMedium, color = MistFaint, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun OverflowMenu(
    actions: List<MenuAction>,
    expanded: Boolean? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    visible: Boolean = true,
    tint: Color = MistDim,
) {
    var ownOpen by remember { mutableStateOf(false) }
    val open = expanded ?: ownOpen
    val setOpen: (Boolean) -> Unit = onExpandedChange ?: { ownOpen = it }
    Box {
        IconButton(onClick = { setOpen(true) }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.MoreHoriz, S.more, tint = if (visible) tint else Color.Transparent)
        }
        DropdownMenu(expanded = open, onDismissRequest = { setOpen(false) }, containerColor = Ink3, shape = MaterialTheme.shapes.medium) {
            actions.forEach { a ->
                DropdownMenuItem(
                    text = { Text(a.label, style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = { Icon(a.icon, null, modifier = Modifier.size(18.dp)) },
                    onClick = {
                        setOpen(false)
                        a.onClick()
                    },
                )
            }
        }
    }
}

@Composable
fun EqualizerBars(playing: Boolean, color: Color, modifier: Modifier = Modifier, barWidth: Dp = 3.dp, height: Dp = 14.dp) {
    val transition = rememberInfiniteTransition(label = "eq")
    Row(modifier.height(height), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        listOf(520, 380, 640).forEachIndexed { i, ms ->
            val animated by transition.animateFloat(
                0.25f, 1f, infiniteRepeatable(tween(ms, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bar$i",
            )
            Box(
                Modifier.width(barWidth).fillMaxHeight(if (playing) animated else listOf(0.4f, 0.7f, 0.5f)[i])
                    .clip(RoundedCornerShape(50)).background(color),
            )
        }
    }
}

// ───────────────────────── Knoppen ─────────────────────────

@Composable
fun HeartButton(starred: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.primary, size: Dp = 22.dp) {
    val pop by animateFloatAsState(if (starred) 1f else 0.9f, spring(dampingRatio = 0.35f, stiffness = 500f), label = "heart")
    IconButton(onClick = onToggle, modifier = modifier) {
        AnimatedContent(starred, transitionSpec = { scaleIn(spring(dampingRatio = 0.4f)) togetherWith scaleOut(tween(90)) }, label = "heartIcon") { on ->
            Icon(
                if (on) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                if (on) S.removeFromFavorites else S.addToFavorites,
                tint = if (on) tint else Color.White.copy(alpha = 0.75f),
                modifier = Modifier.size(size).scale(pop),
            )
        }
    }
}

data class GroupDownload(val state: GroupDownloadState, val progress: Float?)

@Composable
fun rememberGroupDownload(songs: List<Song>): GroupDownload {
    val app = LocalApp.current
    val items = app.downloads.items.observe()
    val ids = remember(songs) { songs.map { it.id } }
    return remember(ids, items) {
        val state = app.downloads.groupState(ids, items)
        val progress = if (state == GroupDownloadState.Active && ids.isNotEmpty()) {
            ids.sumOf { id ->
                val it = items[id]
                when {
                    it == null -> 0.0
                    it.isDone -> 1.0
                    else -> (it.progress ?: 0f).toDouble()
                }
            }.toFloat() / ids.size
        } else {
            null
        }
        GroupDownload(state, progress)
    }
}

@Composable
fun GroupDownloadButton(songs: List<Song>) {
    val app = LocalApp.current
    val dl = rememberGroupDownload(songs)
    val accent = MaterialTheme.colorScheme.primary
    var confirm by remember { mutableStateOf(false) }
    IconButton(onClick = {
        when (dl.state) {
            GroupDownloadState.Done -> confirm = true
            GroupDownloadState.Active -> Unit
            else -> app.downloads.download(songs)
        }
    }) {
        when (dl.state) {
            GroupDownloadState.Done -> Icon(Icons.Rounded.DownloadDone, S.downloaded, tint = accent)
            GroupDownloadState.Active -> {
                val p = dl.progress
                if (p != null) {
                    CircularProgressIndicator(progress = { p }, color = accent, trackColor = Color.White.copy(alpha = 0.15f), strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp))
                } else {
                    CircularProgressIndicator(color = accent, strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp))
                }
            }
            GroupDownloadState.Partial -> Icon(Icons.Rounded.Download, S.downloadRest, tint = accent)
            GroupDownloadState.None -> Icon(Icons.Rounded.Download, S.download, tint = Color.White.copy(alpha = 0.75f))
        }
    }
    if (confirm) {
        ConfirmDialog(S.removeDownloadsTitle, S.removeDownloadsText, S.remove, onConfirm = {
            confirm = false
            app.downloads.remove(songs.map { it.id })
        }, onDismiss = { confirm = false })
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text, color = MistDim) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel, color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(S.cancel) } },
        containerColor = Ink3,
        shape = MaterialTheme.shapes.large,
    )
}

@Composable
fun SmallAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            contentColor = MaterialTheme.colorScheme.primary,
        ),
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

// ───────────────────────── Bouwstenen ─────────────────────────

@Composable
fun Modifier.pressable(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, spring(dampingRatio = 0.55f, stiffness = 600f), label = "press")
    return this.scale(scale).pointerHoverIcon(PointerIcon.Hand).clickable(interactionSource = interaction, indication = null, onClick = onClick)
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(title, style = MaterialTheme.typography.titleLarge, modifier = modifier.padding(horizontal = 32.dp, vertical = 12.dp))
}

@Composable
fun ScreenTitle(title: String, modifier: Modifier = Modifier) {
    Text(title, style = MaterialTheme.typography.headlineLarge, modifier = modifier.padding(start = 32.dp, end = 32.dp, top = 28.dp, bottom = 16.dp))
}

@Composable
fun <T> LoadContent(handle: LoadHandle<T>, modifier: Modifier = Modifier, errorAction: Pair<String, () -> Unit>? = null, content: @Composable (T) -> Unit) {
    AnimatedContent(handle.state, contentKey = { it::class }, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) }, label = "load", modifier = modifier) { s ->
        when (s) {
            is Load.Loading -> LoadingView()
            is Load.Failed -> ErrorView(s.message, handle::reload, errorAction)
            is Load.Ready -> content(s.data)
        }
    }
}

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit, action: Pair<String, () -> Unit>? = null) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.CloudOff, null, tint = MistFaint, modifier = Modifier.size(52.dp))
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        FilledTonalButton(onClick = onRetry) { Text(S.retry) }
        if (action != null) TextButton(onClick = action.second) { Text(action.first) }
    }
}

@Composable
fun EmptyView(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(40.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = MistFaint, modifier = Modifier.size(52.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MistDim, textAlign = TextAlign.Center, modifier = Modifier.width(420.dp))
    }
}
