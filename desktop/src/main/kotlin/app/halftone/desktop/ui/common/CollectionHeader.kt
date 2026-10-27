package app.halftone.desktop.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.halftone.desktop.data.COVER_LARGE
import app.halftone.desktop.i18n.S
import app.halftone.desktop.ui.theme.CoverColors
import app.halftone.desktop.ui.theme.MistDim

/** Kleurgloed achter de kop van een album-, artiest- of afspeellijstpagina. */
@Composable
fun CoverGlow(colors: CoverColors, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().height(420.dp).background(
            Brush.verticalGradient(listOf(colors.backdrop, colors.backdrop.copy(alpha = 0.45f), Color.Transparent)),
        ),
    )
}

/** Kop op bureaubladformaat: hoesje links, titel en acties rechts. */
@Composable
fun CollectionHeader(
    coverArt: String?,
    kind: String,
    title: String,
    subtitle: String?,
    meta: String,
    colors: CoverColors,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
    onSubtitleClick: (() -> Unit)? = null,
    placeholderIcon: ImageVector = Icons.Rounded.Album,
    playEnabled: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, top = 40.dp, bottom = 24.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        CoverArt(
            coverArt,
            Modifier.size(232.dp).shadow(28.dp, MaterialTheme.shapes.large, ambientColor = colors.accent, spotColor = colors.accent),
            size = COVER_LARGE,
            shape = MaterialTheme.shapes.large,
            placeholderIcon = placeholderIcon,
        )
        Spacer(Modifier.width(32.dp))
        Column(Modifier.weight(1f)) {
            Text(kind.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.displaySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.accent,
                    modifier = if (onSubtitleClick != null) {
                        Modifier.pointerHoverIcon(PointerIcon.Hand).clickable(onClick = onSubtitleClick)
                    } else {
                        Modifier
                    },
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(meta, style = MaterialTheme.typography.bodySmall, color = MistDim)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onPlay,
                    enabled = playEnabled,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.Black),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                ) {
                    Icon(Icons.Rounded.PlayArrow, null)
                    Spacer(Modifier.width(6.dp))
                    Text(S.play, style = MaterialTheme.typography.titleSmall)
                }
                FilledTonalButton(
                    onClick = onShuffle,
                    enabled = playEnabled,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color.White.copy(alpha = 0.1f), contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Icon(Icons.Rounded.Shuffle, null)
                    Spacer(Modifier.width(6.dp))
                    Text(S.shuffle, style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.width(4.dp))
                actions()
            }
        }
    }
}
