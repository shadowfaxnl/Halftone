package app.halftone.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.halftone.playback.PlayerState
import app.halftone.ui.common.CoverArt
import app.halftone.ui.theme.Mist
import app.halftone.ui.theme.Ink3
import app.halftone.ui.theme.MistDim
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.ui.res.stringResource
import app.halftone.R

@Composable
fun MiniPlayer(
    state: PlayerState,
    position: StateFlow<Long>,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
) {
    val item = state.current ?: return
    val accent = MaterialTheme.colorScheme.primary

    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(20.dp),
        color = accent.copy(alpha = 0.18f).compositeOver(Ink3),
        contentColor = Mist,
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .fillMaxWidth(),
    ) {
        Column {
            Row(
                Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedContent(
                    targetState = item.coverArt,
                    transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.85f)) togetherWith fadeOut() },
                    label = "miniCover",
                ) { cover ->
                    CoverArt(cover, Modifier.size(46.dp), shape = RoundedCornerShape(12.dp))
                }
                Spacer(Modifier.width(12.dp))
                AnimatedContent(
                    targetState = item,
                    contentKey = { it.mediaId },
                    transitionSpec = {
                        (slideInVertically { it / 2 } + fadeIn()) togetherWith
                            (slideOutVertically { -it / 2 } + fadeOut())
                    },
                    label = "miniTitle",
                    modifier = Modifier.weight(1f),
                ) { current ->
                    Column {
                        Text(
                            current.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            current.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MistDim,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                IconButton(onClick = onToggle) {
                    AnimatedContent(
                        targetState = state.isPlaying,
                        transitionSpec = { scaleIn() togetherWith scaleOut() },
                        label = "miniPlay",
                    ) { playing ->
                        Icon(
                            if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(if (playing) R.string.pause else R.string.play),
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = stringResource(R.string.next), modifier = Modifier.size(28.dp))
                }
            }
            MiniProgress(position, state.durationMs)
        }
    }
}

/** Dunne voortgangslijn. Apart, zodat alleen dit stukje elke tik opnieuw tekent. */
@Composable
private fun MiniProgress(position: StateFlow<Long>, durationMs: Long) {
    val pos by position.collectAsStateWithLifecycle()
    val fraction = if (durationMs > 0) (pos.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    Box(
        Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 6.dp)
            .fillMaxWidth()
            .height(2.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.12f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}
