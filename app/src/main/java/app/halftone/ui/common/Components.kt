package app.halftone.ui.common

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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.halftone.data.Album
import app.halftone.data.COVER_SMALL
import app.halftone.data.Playlist
import app.halftone.data.Song
import app.halftone.data.StarKind
import app.halftone.download.GroupDownloadState
import app.halftone.ui.theme.Ink3
import app.halftone.ui.theme.Ink4
import app.halftone.ui.theme.MistDim
import app.halftone.ui.theme.MistFaint
import coil3.compose.AsyncImage

// ───────────────────────── Hoesjes ─────────────────────────

@Composable
fun CoverArt(
    coverArt: String?,
    modifier: Modifier = Modifier,
    size: Int = COVER_SMALL,
    shape: Shape = MaterialTheme.shapes.medium,
    placeholderIcon: ImageVector = Icons.Rounded.Album,
) {
    val client = LocalAppContainer.current.client
    val url = remember(coverArt, size) { client.coverArtUrl(coverArt, size) }
    Box(
        modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(Ink4, Ink3))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            placeholderIcon,
            contentDescription = null,
            tint = MistFaint,
            modifier = Modifier.fillMaxSize(0.38f),
        )
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Vierkant hoesje met titel en ondertitel; basis voor album- en afspeellijstkaarten. */
@Composable
fun MediaCard(
    coverArt: String?,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholderIcon: ImageVector = Icons.Rounded.Album,
) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .pressable(onClick)
            .padding(bottom = 6.dp),
    ) {
        CoverArt(
            coverArt,
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(12.dp, MaterialTheme.shapes.medium, clip = false),
            placeholderIcon = placeholderIcon,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!subtitle.isNullOrEmpty()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun AlbumCard(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showArtist: Boolean = true,
) {
    MediaCard(
        coverArt = album.coverArt,
        title = album.name,
        subtitle = listOfNotNull(album.artist.takeIf { showArtist }, album.year?.toString()).joinToString(" · "),
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
fun PlaylistCard(playlist: Playlist, onClick: () -> Unit, modifier: Modifier = Modifier) {
    MediaCard(
        coverArt = playlist.coverArt,
        title = playlist.name,
        subtitle = if (playlist.songCount == 1) "1 nummer" else "${playlist.songCount} nummers",
        onClick = onClick,
        modifier = modifier,
        placeholderIcon = Icons.AutoMirrored.Rounded.QueueMusic,
    )
}

@Composable
fun ArtistAvatar(coverArt: String?, modifier: Modifier = Modifier) {
    CoverArt(
        coverArt,
        modifier.aspectRatio(1f),
        shape = CircleShape,
        placeholderIcon = Icons.Rounded.Person,
    )
}

// ───────────────────────── Nummers ─────────────────────────

data class MenuAction(val label: String, val icon: ImageVector, val onClick: () -> Unit)

/** Het standaardmenu voor een nummer. Extra acties komen onderaan. */
@Composable
fun songMenu(
    song: Song,
    onOpenAlbum: ((String) -> Unit)? = null,
    onOpenArtist: ((String) -> Unit)? = null,
    extra: List<MenuAction> = emptyList(),
): List<MenuAction> {
    val container = LocalAppContainer.current
    val starred by container.favorites.ids.collectAsStateWithLifecycle()
    val downloads by container.downloads.items.collectAsStateWithLifecycle()
    val isStarred = song.id in starred
    val download = downloads[song.id]

    return buildList {
        add(MenuAction("Speel hierna", Icons.AutoMirrored.Rounded.QueueMusic) {
            container.player.playNext(song)
            container.toast("Speelt hierna")
        })
        add(MenuAction("Aan wachtrij toevoegen", Icons.AutoMirrored.Rounded.QueueMusic) {
            container.player.addToQueue(song)
            container.toast("Toegevoegd aan wachtrij")
        })
        add(MenuAction("Aan afspeellijst toevoegen", Icons.AutoMirrored.Rounded.PlaylistAdd) {
            container.openAddToPlaylist(listOf(song))
        })
        add(
            MenuAction(
                if (isStarred) "Uit favorieten" else "Aan favorieten toevoegen",
                if (isStarred) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            ) { container.favorites.toggle(song.id, StarKind.Song) },
        )
        if (download == null || download.isFailed) {
            add(MenuAction("Downloaden", Icons.Rounded.Download) { container.downloads.download(listOf(song)) })
        } else {
            add(MenuAction("Download verwijderen", Icons.Rounded.Delete) { container.downloads.remove(listOf(song.id)) })
        }
        val albumId = song.albumId
        if (onOpenAlbum != null && albumId != null) {
            add(MenuAction("Ga naar album", Icons.Rounded.Album) { onOpenAlbum(albumId) })
        }
        val artistId = song.artistId
        if (onOpenArtist != null && artistId != null) {
            add(MenuAction("Ga naar artiest", Icons.Rounded.Person) { onOpenArtist(artistId) })
        }
        addAll(extra)
    }
}

@Composable
fun SongRow(
    title: String,
    subtitle: String?,
    durationSec: Int,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    songId: String? = null,
    menu: List<MenuAction> = emptyList(),
    leading: @Composable () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(if (isCurrent) accent.copy(alpha = 0.10f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                color = if (isCurrent) accent else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (songId != null) SongBadges(songId)
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Text(
            formatDuration(durationSec),
            style = MaterialTheme.typography.bodySmall,
            color = MistFaint,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        if (menu.isNotEmpty()) {
            OverflowMenu(menu)
        }
    }
}

/** Kleine tekens onder de titel: een hartje als favoriet, een vinkje of voortgang als gedownload. */
@Composable
private fun SongBadges(songId: String) {
    val container = LocalAppContainer.current
    val starred by container.favorites.ids.collectAsStateWithLifecycle()
    val downloads by container.downloads.items.collectAsStateWithLifecycle()
    val accent = MaterialTheme.colorScheme.primary
    val download = downloads[songId]

    if (songId in starred) {
        Icon(Icons.Rounded.Favorite, "Favoriet", tint = accent, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(5.dp))
    }
    when {
        download?.isDone == true -> {
            Icon(Icons.Rounded.DownloadDone, "Gedownload", tint = accent, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
        }
        download?.isActive == true -> {
            val progress = download.progress
            if (progress != null) {
                CircularProgressIndicator(
                    progress = { progress },
                    color = accent,
                    trackColor = Color.White.copy(alpha = 0.15f),
                    strokeWidth = 1.8.dp,
                    modifier = Modifier.size(12.dp),
                )
            } else {
                CircularProgressIndicator(color = accent, strokeWidth = 1.8.dp, modifier = Modifier.size(12.dp))
            }
            Spacer(Modifier.width(6.dp))
        }
    }
}

/** Tracknummer, of dansende staafjes als dit nummer speelt. */
@Composable
fun TrackNumber(number: Int?, isCurrent: Boolean, isPlaying: Boolean) {
    Box(Modifier.width(26.dp), contentAlignment = Alignment.Center) {
        if (isCurrent) {
            EqualizerBars(playing = isPlaying, color = MaterialTheme.colorScheme.primary)
        } else {
            Text(
                number?.toString() ?: "•",
                style = MaterialTheme.typography.bodyMedium,
                color = MistFaint,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun OverflowMenu(actions: List<MenuAction>, tint: Color = MistDim) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "Meer", tint = tint)
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = MaterialTheme.shapes.medium,
            containerColor = Ink3,
        ) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.label) },
                    leadingIcon = { Icon(action.icon, contentDescription = null) },
                    onClick = {
                        open = false
                        action.onClick()
                    },
                )
            }
        }
    }
}

@Composable
fun EqualizerBars(
    playing: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    barWidth: Dp = 3.dp,
    height: Dp = 14.dp,
) {
    val transition = rememberInfiniteTransition(label = "eq")
    val durations = listOf(520, 380, 640)
    Row(
        modifier.height(height),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        durations.forEachIndexed { i, ms ->
            val animated by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(ms, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "bar$i",
            )
            val fraction = if (playing) animated else listOf(0.4f, 0.7f, 0.5f)[i]
            Box(
                Modifier
                    .width(barWidth)
                    .fillMaxHeight(fraction)
                    .clip(RoundedCornerShape(50))
                    .background(color),
            )
        }
    }
}

// ───────────────────────── Actieknoppen ─────────────────────────

/** Ronde knop met hartje dat even opveert als je het aanzet. */
@Composable
fun HeartButton(
    starred: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 26.dp,
) {
    val pop by animateFloatAsState(
        if (starred) 1f else 0.9f,
        spring(dampingRatio = 0.35f, stiffness = 500f),
        label = "heart",
    )
    IconButton(onClick = onToggle, modifier = modifier) {
        AnimatedContent(
            targetState = starred,
            transitionSpec = { scaleIn(spring(dampingRatio = 0.4f)) togetherWith scaleOut(tween(90)) },
            label = "heartIcon",
        ) { on ->
            Icon(
                if (on) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = if (on) "Uit favorieten" else "Aan favorieten toevoegen",
                tint = if (on) tint else Color.White.copy(alpha = 0.75f),
                modifier = Modifier
                    .size(size)
                    .scale(pop),
            )
        }
    }
}

/** Downloadknop voor een album of afspeellijst, met de toestand van alle nummers erin. */
@Composable
fun GroupDownloadButton(
    state: GroupDownloadState,
    progress: Float?,
    onDownload: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    var confirmRemove by remember { mutableStateOf(false) }
    IconButton(
        onClick = {
            when (state) {
                GroupDownloadState.Done -> confirmRemove = true
                GroupDownloadState.Active -> Unit
                else -> onDownload()
            }
        },
        modifier = modifier,
    ) {
        when (state) {
            GroupDownloadState.Done ->
                Icon(Icons.Rounded.DownloadDone, "Gedownload", tint = accent, modifier = Modifier.size(26.dp))
            GroupDownloadState.Active ->
                Box(contentAlignment = Alignment.Center) {
                    if (progress != null) {
                        CircularProgressIndicator(
                            progress = { progress },
                            color = accent,
                            trackColor = Color.White.copy(alpha = 0.15f),
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(26.dp),
                        )
                    } else {
                        CircularProgressIndicator(color = accent, strokeWidth = 2.5.dp, modifier = Modifier.size(26.dp))
                    }
                }
            GroupDownloadState.Partial ->
                Icon(Icons.Rounded.Download, "Rest downloaden", tint = accent, modifier = Modifier.size(26.dp))
            GroupDownloadState.None ->
                Icon(Icons.Rounded.Download, "Downloaden", tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(26.dp))
        }
    }
    if (confirmRemove) {
        ConfirmDialog(
            title = "Downloads verwijderen?",
            text = "De nummers blijven op je server staan, maar zijn dan niet meer offline te beluisteren.",
            confirmLabel = "Verwijderen",
            onConfirm = {
                confirmRemove = false
                onRemove()
            },
            onDismiss = { confirmRemove = false },
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text, color = MistDim) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuleren") } },
        containerColor = Ink3,
        shape = MaterialTheme.shapes.large,
    )
}

// ───────────────────────── Bouwstenen ─────────────────────────

/** Laat een element buiten de padding van zijn lijst doorlopen, tot aan de schermrand. */
fun Modifier.bleed(horizontal: Dp): Modifier = layout { measurable, constraints ->
    val extra = horizontal.roundToPx() * 2
    val width = constraints.maxWidth + extra
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) {
        placeable.place(-extra / 2, 0)
    }
}

/** Klikbaar met een zachte indruk-animatie. */
@Composable
fun Modifier.pressable(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.96f else 1f,
        spring(dampingRatio = 0.55f, stiffness = 600f),
        label = "press",
    )
    return this
        .scale(scale)
        .clickable(interactionSource = interaction, indication = null, onClick = onClick)
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 12.dp),
    )
}

/** Ronde, doorschijnende knop die op elk hoesje leesbaar blijft. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Terug", onBack, modifier)
}

/** Een extra knop op het foutscherm, bijvoorbeeld "Bekijk downloads" als je offline bent. */
data class ErrorAction(val label: String, val onClick: () -> Unit)

@Composable
fun <T> LoadContent(
    state: Load<T>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    errorAction: ErrorAction? = null,
    content: @Composable (T) -> Unit,
) {
    AnimatedContent(
        targetState = state,
        contentKey = { it::class },
        transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(180)) },
        label = "load",
        modifier = modifier,
    ) { current ->
        when (current) {
            is Load.Loading -> LoadingView()
            is Load.Failed -> ErrorView(current.message, onRetry, action = errorAction)
            is Load.Ready -> content(current.data)
        }
    }
}

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp,
            modifier = Modifier.size(36.dp),
        )
    }
}

@Composable
fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    action: ErrorAction? = null,
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = MistFaint, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        FilledTonalButton(onClick = onRetry) { Text("Opnieuw proberen") }
        if (action != null) {
            TextButton(onClick = action.onClick) { Text(action.label) }
        }
    }
}

/** Leeg-scherm met een icoon en een korte uitleg. */
@Composable
fun EmptyView(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .padding(40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = MistFaint, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MistDim, textAlign = TextAlign.Center)
    }
}
