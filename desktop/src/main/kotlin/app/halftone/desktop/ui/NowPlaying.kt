package app.halftone.desktop.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import app.halftone.desktop.data.COVER_LARGE
import app.halftone.desktop.i18n.S
import app.halftone.desktop.ui.common.CoverArt
import app.halftone.desktop.ui.common.EqualizerBars
import app.halftone.desktop.ui.common.LocalApp
import app.halftone.desktop.ui.common.observe
import app.halftone.desktop.ui.theme.CoverColors
import app.halftone.desktop.ui.theme.Ink
import app.halftone.desktop.ui.theme.Ink2
import app.halftone.desktop.ui.theme.MistDim
import app.halftone.desktop.ui.theme.MistFaint
import coil3.compose.AsyncImage

/** Groot speelt-nu-scherm boven de rest van het venster, met de spelerbalk eronder. */
@Composable
fun NowPlaying(colors: CoverColors) {
    val app = LocalApp.current
    val ps = app.player.state.observe()
    val song = ps.current ?: return
    val scale by animateFloatAsState(if (ps.isPlaying) 1f else 0.92f, spring(dampingRatio = 0.6f, stiffness = 300f), label = "cover")

    Box(Modifier.fillMaxSize().background(Ink)) {
        AnimatedContent(song.coverArt, transitionSpec = { fadeIn(tween(700)) togetherWith fadeOut(tween(700)) }, label = "npBg") { cover ->
            AsyncImage(
                app.client.coverArtUrl(cover, COVER_LARGE), null,
                contentScale = ContentScale.Crop, alpha = 0.55f,
                modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = 1.3f; scaleY = 1.3f }.blur(90.dp),
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to colors.backdrop.copy(alpha = 0.5f), 0.6f to colors.backdropDeep.copy(alpha = 0.85f), 1f to Ink),
            ),
        )

        IconButton(onClick = { app.nowPlayingOpen = false }, modifier = Modifier.padding(16.dp).align(Alignment.TopStart).pointerHoverIcon(PointerIcon.Hand)) {
            Icon(Icons.Rounded.KeyboardArrowDown, S.close, tint = Color.White, modifier = Modifier.size(30.dp))
        }

        BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 64.dp, vertical = 48.dp)) {
            val coverSize = min(maxHeight - 120.dp, maxWidth * 0.45f)
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(56.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedContent(song.coverArt, transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(250)) }, label = "npCover") { cover ->
                        CoverArt(
                            cover,
                            Modifier.size(coverSize).scale(scale).shadow(48.dp, RoundedCornerShape(24.dp), ambientColor = colors.accent, spotColor = colors.accent),
                            size = COVER_LARGE,
                            shape = RoundedCornerShape(24.dp),
                        )
                    }
                    Spacer(Modifier.height(28.dp))
                    Text(song.title, style = MaterialTheme.typography.headlineMedium, color = Color.White, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(coverSize))
                    Text(
                        song.artist.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.accent,
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand).clickable {
                            song.artistId?.let { app.nowPlayingOpen = false; app.nav.artist(it) }
                        },
                    )
                    Text(
                        song.album.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand).clickable {
                            song.albumId?.let { app.nowPlayingOpen = false; app.nav.album(it) }
                        },
                    )
                }
                // Wat er hierna komt
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Text(S.upNext, style = MaterialTheme.typography.titleLarge, color = Color.White, modifier = Modifier.padding(bottom = 12.dp))
                    QueueList(Modifier.weight(1f), dark = true)
                }
            }
        }
    }
}

/** De wachtrij als zijpaneel. */
@Composable
fun QueuePanel() {
    val app = LocalApp.current
    Column(Modifier.width(340.dp).fillMaxHeight().background(Ink2)) {
        Box(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(S.queue, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = { app.queueOpen = false }) { Icon(Icons.Rounded.Close, S.close, tint = MistDim) }
            }
        }
        QueueList(Modifier.weight(1f).padding(horizontal = 8.dp), dark = false)
    }
}

@Composable
private fun QueueList(modifier: Modifier, dark: Boolean) {
    val app = LocalApp.current
    val ps = app.player.state.observe()
    val accent = MaterialTheme.colorScheme.primary
    if (ps.queue.isEmpty()) {
        Text(S.queueEmpty, style = MaterialTheme.typography.bodyMedium, color = MistFaint, modifier = modifier.padding(12.dp))
        return
    }
    LazyColumn(modifier) {
        itemsIndexed(ps.queue, key = { i, s -> "$i-${s.id}" }) { index, song ->
            val isCurrent = index == ps.index
            val isPast = index < ps.index
            Row(
                Modifier.fillMaxWidth()
                    .background(if (isCurrent) accent.copy(alpha = 0.14f) else Color.Transparent, RoundedCornerShape(10.dp))
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable { app.player.skipTo(index) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoverArt(song.coverArt, Modifier.size(40.dp), shape = MaterialTheme.shapes.small)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        song.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                        color = when {
                            isCurrent -> accent
                            isPast -> Color.White.copy(alpha = 0.45f)
                            dark -> Color.White
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(song.artist.orEmpty(), style = MaterialTheme.typography.bodySmall, color = if (isPast) MistFaint else MistDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (isCurrent) {
                    EqualizerBars(ps.isPlaying, accent, Modifier.padding(horizontal = 10.dp))
                } else {
                    IconButton(onClick = { app.player.removeAt(index) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Close, S.remove, tint = MistFaint, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
