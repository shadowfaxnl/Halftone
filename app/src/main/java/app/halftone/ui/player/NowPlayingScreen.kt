@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package app.halftone.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import app.halftone.data.COVER_LARGE
import app.halftone.data.StarKind
import app.halftone.playback.toSong
import app.halftone.ui.common.CoverArt
import app.halftone.ui.common.HeartButton
import app.halftone.ui.common.LocalAppContainer
import app.halftone.ui.common.formatMs
import app.halftone.ui.theme.CoverColors
import app.halftone.ui.theme.Ink
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.ui.res.stringResource
import app.halftone.R

@Composable
fun NowPlayingScreen(
    colors: CoverColors,
    onClose: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
) {
    val container = LocalAppContainer.current
    val player = container.player
    val state by player.state.collectAsStateWithLifecycle()
    val starred by container.favorites.ids.collectAsStateWithLifecycle()
    val item = state.current ?: return
    var showQueue by rememberSaveable { mutableStateOf(false) }

    BackHandler(onBack = onClose)

    Box(
        Modifier
            .fillMaxSize()
            .background(Ink)
            // Vegen naar beneden sluit het scherm.
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { if (total > 180f) onClose() },
                ) { _, dy -> total += dy }
            },
    ) {
        // Het hoesje, groot en wazig, als sfeer.
        AnimatedContent(
            targetState = item.coverArt,
            transitionSpec = { fadeIn(tween(700)) togetherWith fadeOut(tween(700)) },
            label = "bg",
        ) { cover ->
            AsyncImage(
                model = container.client.coverArtUrl(cover, COVER_LARGE),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.6f,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.4f
                        scaleY = 1.4f
                    }
                    .blur(90.dp),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to colors.backdrop.copy(alpha = 0.55f),
                        0.55f to colors.backdropDeep.copy(alpha = 0.85f),
                        1f to Ink,
                    ),
                ),
        )

        // Breder dan hoog (tablet of telefoon in landschap): hoesje links, bediening rechts.
        // Anders de vertrouwde kolom. Het hoesje past zich in beide gevallen aan de ruimte aan.
        val topBar: @Composable () -> Unit = {
            // Bovenbalk
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.close), tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Column(
                    Modifier
                        .weight(1f)
                        .clickable(enabled = item.albumId != null) { item.albumId?.let(onOpenAlbum) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.playing_from),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                    Text(
                        item.album,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.basicMarquee(),
                    )
                }
                IconButton(onClick = { container.openAddToPlaylist(listOf(item.toSong())) }) {
                    Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, stringResource(R.string.add_to_playlist), tint = Color.White)
                }
                IconButton(onClick = { showQueue = true }) {
                    Icon(Icons.AutoMirrored.Rounded.QueueMusic, stringResource(R.string.queue), tint = Color.White)
                }
            }
        }
        val cover: @Composable () -> Unit = {
            SwipeableCover(
                coverArt = item.coverArt,
                isPlaying = state.isPlaying,
                accent = colors.accent,
                onNext = player::next,
                onPrevious = player::previous,
            )
        }
        val details: @Composable () -> Unit = {
            Column {
                // Titel en artiest, met het hartje ernaast
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedContent(
                        targetState = item,
                        contentKey = { it.mediaId },
                        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                        label = "title",
                        modifier = Modifier.weight(1f),
                    ) { current ->
                        Column(Modifier.fillMaxWidth()) {
                            Text(
                                current.title,
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee(),
                            )
                            Text(
                                current.artist,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                modifier = Modifier
                                    .basicMarquee()
                                    .clickable(enabled = current.artistId != null) {
                                        current.artistId?.let(onOpenArtist)
                                    },
                            )
                        }
                    }
                    HeartButton(
                        starred = item.mediaId in starred,
                        onToggle = { container.favorites.toggle(item.mediaId, StarKind.Song) },
                        tint = colors.accent,
                        size = 28.dp,
                    )
                }

                Spacer(Modifier.height(20.dp))
                SeekBar(player.position, state.durationMs, onSeek = player::seekTo)
                Spacer(Modifier.height(12.dp))

                Controls(
                    isPlaying = state.isPlaying,
                    isBuffering = state.isBuffering,
                    shuffle = state.shuffle,
                    repeatMode = state.repeatMode,
                    accent = colors.accent,
                    onToggle = player::togglePlay,
                    onNext = player::next,
                    onPrevious = player::previous,
                    onShuffle = player::toggleShuffle,
                    onRepeat = player::cycleRepeat,
                )
            }
        }

        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            if (maxWidth > maxHeight) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                ) {
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) { cover() }
                    Column(
                        Modifier
                            .weight(1f)
                            .widthIn(max = 520.dp),
                    ) {
                        topBar()
                        Spacer(Modifier.height(24.dp))
                        details()
                    }
                }
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    topBar()
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center,
                    ) { cover() }
                    Column(Modifier.widthIn(max = 560.dp)) { details() }
                    Spacer(Modifier.height(40.dp))
                }
            }
        }

        if (showQueue) {
            QueueSheet(
                state = state,
                onDismiss = { showQueue = false },
                onSelect = player::skipTo,
                onRemove = player::removeAt,
            )
        }
    }
}

/** Het hoesje: ademt mee met afspelen en pauzeren, en je kunt het opzij vegen. */
@Composable
private fun SwipeableCover(
    coverArt: String?,
    isPlaying: Boolean,
    accent: Color,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    val scale by animateFloatAsState(
        if (isPlaying) 1f else 0.86f,
        spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "coverScale",
    )
    val shape = RoundedCornerShape(26.dp)

    Box(
        Modifier
            .widthIn(max = 560.dp)
            .aspectRatio(1f, matchHeightConstraintsFirst = true)
            .scale(scale)
            .graphicsLayer {
                translationX = offset.value
                rotationZ = offset.value / 60f
                alpha = 1f - (abs(offset.value) / (size.width * 1.4f)).coerceIn(0f, 0.6f)
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val threshold = size.width * 0.25f
                        when {
                            offset.value < -threshold -> onNext()
                            offset.value > threshold -> onPrevious()
                        }
                        scope.launch { offset.animateTo(0f, spring(dampingRatio = 0.7f)) }
                    },
                    onDragCancel = { scope.launch { offset.animateTo(0f) } },
                ) { change, dx ->
                    change.consume()
                    scope.launch { offset.snapTo(offset.value + dx) }
                }
            }
            .shadow(40.dp, shape, ambientColor = accent, spotColor = accent),
    ) {
        AnimatedContent(
            targetState = coverArt,
            transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(250)) },
            label = "cover",
        ) { cover ->
            CoverArt(cover, Modifier.fillMaxSize(), size = COVER_LARGE, shape = shape)
        }
    }
}

/** Een slanke voortgangsbalk die dikker wordt zodra je hem vastpakt. */
@Composable
private fun SeekBar(position: StateFlow<Long>, durationMs: Long, onSeek: (Long) -> Unit) {
    val pos by position.collectAsStateWithLifecycle()
    var dragging by remember { mutableStateOf<Float?>(null) }
    val currentOnSeek by rememberUpdatedState(onSeek)
    val duration = durationMs.coerceAtLeast(1L)
    val fraction = dragging ?: (pos.toFloat() / duration).coerceIn(0f, 1f)
    val trackHeight by animateDpAsState(if (dragging != null) 8.dp else 4.dp, label = "track")
    val thumbRadius by animateDpAsState(if (dragging != null) 9.dp else 6.dp, label = "thumb")

    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .height(30.dp)
                .pointerInput(duration) {
                    detectTapGestures { o ->
                        currentOnSeek(((o.x / size.width).coerceIn(0f, 1f) * duration).toLong())
                    }
                }
                .pointerInput(duration) {
                    detectHorizontalDragGestures(
                        onDragStart = { o -> dragging = (o.x / size.width).coerceIn(0f, 1f) },
                        onDragEnd = {
                            dragging?.let { currentOnSeek((it * duration).toLong()) }
                            dragging = null
                        },
                        onDragCancel = { dragging = null },
                    ) { change, _ ->
                        change.consume()
                        dragging = (change.position.x / size.width).coerceIn(0f, 1f)
                    }
                }
                .drawBehind {
                    val y = size.height / 2
                    val stroke = trackHeight.toPx()
                    drawLine(Color.White.copy(alpha = 0.18f), Offset(0f, y), Offset(size.width, y), stroke, StrokeCap.Round)
                    drawLine(Color.White, Offset(0f, y), Offset(size.width * fraction, y), stroke, StrokeCap.Round)
                    drawCircle(Color.White, thumbRadius.toPx(), Offset(size.width * fraction, y))
                },
        )
        Row(Modifier.fillMaxWidth()) {
            val shownMs = (fraction * duration).toLong()
            Text(formatMs(shownMs), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.6f))
            Spacer(Modifier.weight(1f))
            Text(
                "-" + formatMs((duration - shownMs).coerceAtLeast(0)),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun Controls(
    isPlaying: Boolean,
    isBuffering: Boolean,
    shuffle: Boolean,
    repeatMode: Int,
    accent: Color,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToggleIcon(Icons.Rounded.Shuffle, stringResource(R.string.shuffle), active = shuffle, accent = accent, onClick = onShuffle)
        BigIcon(Icons.Rounded.SkipPrevious, stringResource(R.string.previous), onPrevious)

        // De grote afspeelknop, in de kleur van het hoesje.
        val interaction = remember { MutableInteractionSource() }
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(78.dp)
                    .shadow(20.dp, CircleShape, ambientColor = accent, spotColor = accent)
                    .background(accent, CircleShape)
                    .clickable(interactionSource = interaction, indication = null, onClick = onToggle),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = isPlaying,
                    transitionSpec = { scaleIn(tween(180)) togetherWith scaleOut(tween(120)) },
                    label = "playPause",
                ) { playing ->
                    Icon(
                        if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(if (playing) R.string.pause else R.string.play),
                        tint = Color.Black,
                        modifier = Modifier.size(42.dp),
                    )
                }
            }
            if (isBuffering) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(90.dp),
                )
            }
        }

        BigIcon(Icons.Rounded.SkipNext, stringResource(R.string.next), onNext)
        ToggleIcon(
            if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
            stringResource(R.string.repeat),
            active = repeatMode != Player.REPEAT_MODE_OFF,
            accent = accent,
            onClick = onRepeat,
        )
    }
}

@Composable
private fun BigIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(60.dp)) {
        Icon(icon, description, tint = Color.White, modifier = Modifier.size(40.dp))
    }
}

@Composable
private fun ToggleIcon(
    icon: ImageVector,
    description: String,
    active: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Box(contentAlignment = Alignment.BottomCenter) {
        IconButton(onClick = onClick) {
            Icon(
                icon,
                description,
                tint = if (active) accent else Color.White.copy(alpha = 0.55f),
                modifier = Modifier.size(24.dp),
            )
        }
        if (active) {
            Box(
                Modifier
                    .padding(bottom = 2.dp)
                    .size(4.dp)
                    .background(accent, CircleShape),
            )
        }
    }
}
