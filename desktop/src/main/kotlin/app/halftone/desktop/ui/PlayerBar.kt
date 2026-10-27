@file:OptIn(ExperimentalComposeUiApi::class)

package app.halftone.desktop.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.halftone.desktop.data.StarKind
import app.halftone.desktop.i18n.S
import app.halftone.desktop.player.RepeatMode
import app.halftone.desktop.ui.common.CoverArt
import app.halftone.desktop.ui.common.HeartButton
import app.halftone.desktop.ui.common.LocalApp
import app.halftone.desktop.ui.common.formatDuration
import app.halftone.desktop.ui.common.observe
import app.halftone.desktop.ui.theme.Ink2
import app.halftone.desktop.ui.theme.Line
import app.halftone.desktop.ui.theme.MistDim
import app.halftone.desktop.ui.theme.MistFaint

/** De vaste spelerbalk onderaan het venster. */
@Composable
fun PlayerBar() {
    val app = LocalApp.current
    val ps = app.player.state.observe()
    val starred = app.favorites.ids.observe()
    val song = ps.current
    val accent = MaterialTheme.colorScheme.primary

    Column(Modifier.fillMaxWidth().background(Ink2)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
        Row(Modifier.fillMaxWidth().height(88.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Links: hoesje en titel
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                if (song != null) {
                    CoverArt(
                        song.coverArt,
                        Modifier.size(56.dp).pointerHoverIcon(PointerIcon.Hand).clickable { app.nowPlayingOpen = true },
                        shape = MaterialTheme.shapes.small,
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f, fill = false)) {
                        Text(song.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand).clickable { song.albumId?.let(app.nav::album) })
                        Text(song.artist.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MistDim, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand).clickable { song.artistId?.let(app.nav::artist) })
                    }
                    HeartButton(song.id in starred, { app.favorites.toggle(song.id, StarKind.Song) }, size = 20.dp)
                }
            }

            // Midden: knoppen en voortgang
            Column(Modifier.width(520.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ToggleIcon(Icons.Rounded.Shuffle, S.shuffle, ps.shuffle, accent, app.player::toggleShuffle)
                    BarIcon(Icons.Rounded.SkipPrevious, S.previous, app.player::previous)
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            Modifier.size(42.dp).clip(CircleShape).background(Color.White)
                                .pointerHoverIcon(PointerIcon.Hand).clickable(enabled = song != null, onClick = app.player::togglePlay),
                            contentAlignment = Alignment.Center,
                        ) {
                            AnimatedContent(ps.isPlaying, transitionSpec = { scaleIn() togetherWith scaleOut() }, label = "barPlay") { playing ->
                                Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (playing) S.pause else S.play, tint = Color.Black, modifier = Modifier.size(26.dp))
                            }
                        }
                        if (ps.isBuffering) CircularProgressIndicator(color = accent, strokeWidth = 2.dp, modifier = Modifier.size(50.dp))
                    }
                    BarIcon(Icons.Rounded.SkipNext, S.next, app.player::next)
                    ToggleIcon(
                        if (ps.repeat == RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        S.repeat, ps.repeat != RepeatMode.Off, accent, app.player::cycleRepeat,
                    )
                }
                SeekBar(ps.durationSec)
            }

            // Rechts: afspeellijst, wachtrij, volume
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (song != null) {
                    BarIcon(Icons.AutoMirrored.Rounded.PlaylistAdd, S.addToPlaylist, { app.addToPlaylist = listOf(song) }, size = 20.dp)
                }
                ToggleIcon(Icons.AutoMirrored.Rounded.QueueMusic, S.queue, app.queueOpen, accent, { app.queueOpen = !app.queueOpen })
                VolumeControl(ps.volume, app.player::setVolume)
            }
        }
    }
}

@Composable
private fun SeekBar(durationSec: Double) {
    val app = LocalApp.current
    val pos = app.player.position.observe()
    var dragging by remember { mutableStateOf<Float?>(null) }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val duration = durationSec.coerceAtLeast(1.0)
    val fraction = dragging ?: (pos / duration).toFloat().coerceIn(0f, 1f)
    val trackHeight by animateDpAsState(if (hovered || dragging != null) 6.dp else 4.dp, label = "track")
    val currentDuration by rememberUpdatedState(duration)
    val accent = MaterialTheme.colorScheme.primary

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(formatDuration((fraction * duration).toInt()), style = MaterialTheme.typography.labelMedium, color = MistFaint, modifier = Modifier.width(44.dp))
        Box(
            Modifier.weight(1f).height(20.dp)
                .hoverable(interaction)
                .pointerHoverIcon(PointerIcon.Hand)
                .pointerInput(Unit) {
                    detectTapGestures { o -> app.player.seekTo((o.x / size.width).coerceIn(0f, 1f) * currentDuration) }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { o -> dragging = (o.x / size.width).coerceIn(0f, 1f) },
                        onDragEnd = {
                            dragging?.let { app.player.seekTo(it * currentDuration) }
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
                    drawLine(Color.White.copy(alpha = 0.16f), Offset(0f, y), Offset(size.width, y), stroke, StrokeCap.Round)
                    drawLine(if (hovered || dragging != null) accent else Color.White, Offset(0f, y), Offset(size.width * fraction, y), stroke, StrokeCap.Round)
                    if (hovered || dragging != null) drawCircle(Color.White, 6.dp.toPx(), Offset(size.width * fraction, y))
                },
        )
        Text(formatDuration(duration.toInt()), style = MaterialTheme.typography.labelMedium, color = MistFaint, modifier = Modifier.width(44.dp).padding(start = 8.dp))
    }
}

@Composable
private fun VolumeControl(volume: Int, onChange: (Int) -> Unit) {
    var beforeMute by remember { mutableStateOf(80) }
    val icon = when {
        volume == 0 -> Icons.AutoMirrored.Rounded.VolumeOff
        volume < 50 -> Icons.AutoMirrored.Rounded.VolumeDown
        else -> Icons.AutoMirrored.Rounded.VolumeUp
    }
    BarIcon(icon, S.mute, {
        if (volume > 0) {
            beforeMute = volume
            onChange(0)
        } else {
            onChange(beforeMute.coerceAtLeast(10))
        }
    }, size = 20.dp)
    val fraction = volume / 100f
    val currentOnChange by rememberUpdatedState(onChange)
    Box(
        Modifier.width(110.dp).height(20.dp)
            .pointerHoverIcon(PointerIcon.Hand)
            .onPointerEvent(PointerEventType.Scroll) { e ->
                val dy = e.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                currentOnChange((volume - (dy * 5).toInt()).coerceIn(0, 100))
            }
            .pointerInput(Unit) {
                detectTapGestures { o -> currentOnChange(((o.x / size.width) * 100).toInt().coerceIn(0, 100)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    currentOnChange(((change.position.x / size.width) * 100).toInt().coerceIn(0, 100))
                }
            }
            .drawBehind {
                val y = size.height / 2
                drawLine(Color.White.copy(alpha = 0.16f), Offset(0f, y), Offset(size.width, y), 4.dp.toPx(), StrokeCap.Round)
                drawLine(Color.White, Offset(0f, y), Offset(size.width * fraction, y), 4.dp.toPx(), StrokeCap.Round)
            },
    )
    Spacer(Modifier.width(4.dp))
}

@Composable
private fun BarIcon(icon: ImageVector, description: String, onClick: () -> Unit, size: Dp = 26.dp) {
    IconButton(onClick = onClick, modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)) {
        Icon(icon, description, tint = Color.White, modifier = Modifier.size(size))
    }
}

@Composable
private fun ToggleIcon(icon: ImageVector, description: String, active: Boolean, accent: Color, onClick: () -> Unit) {
    Box(contentAlignment = Alignment.BottomCenter) {
        IconButton(onClick = onClick, modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)) {
            Icon(icon, description, tint = if (active) accent else MistDim, modifier = Modifier.size(20.dp))
        }
        if (active) Box(Modifier.padding(bottom = 4.dp).size(4.dp).background(accent, CircleShape))
    }
}
