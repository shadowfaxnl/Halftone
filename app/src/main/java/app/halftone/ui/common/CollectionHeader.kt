package app.halftone.ui.common

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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.halftone.data.COVER_LARGE
import app.halftone.ui.theme.CoverColors
import app.halftone.ui.theme.MistDim

/** De zachte kleurgloed achter de kop van een album- of afspeellijstpagina. */
@Composable
fun CoverGlow(colors: CoverColors, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(560.dp)
            .background(
                Brush.verticalGradient(
                    listOf(colors.backdrop, colors.backdrop.copy(alpha = 0.5f), Color.Transparent),
                ),
            ),
    )
}

/**
 * Kop voor albums en afspeellijsten: groot hoesje met gekleurde schaduw,
 * titel, ondertitel, details, afspeel- en shuffleknop en een rij kleine acties.
 */
@Composable
fun CollectionHeader(
    coverArt: String?,
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
    Column(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 56.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CoverArt(
            coverArt,
            Modifier
                .size(264.dp)
                .shadow(
                    elevation = 32.dp,
                    shape = MaterialTheme.shapes.large,
                    ambientColor = colors.accent,
                    spotColor = colors.accent,
                ),
            size = COVER_LARGE,
            shape = MaterialTheme.shapes.large,
            placeholderIcon = placeholderIcon,
        )
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        if (!subtitle.isNullOrEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.titleMedium,
                color = colors.accent,
                textAlign = TextAlign.Center,
                modifier = Modifier.clickable(enabled = onSubtitleClick != null) { onSubtitleClick?.invoke() },
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(meta, style = MaterialTheme.typography.bodySmall, color = MistDim, textAlign = TextAlign.Center)
        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onPlay,
                enabled = playEnabled,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.Black),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
            ) {
                Icon(Icons.Rounded.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("Afspelen", style = MaterialTheme.typography.titleSmall)
            }
            FilledTonalButton(
                onClick = onShuffle,
                enabled = playEnabled,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color.White.copy(alpha = 0.1f),
                    contentColor = Color.White,
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
            ) {
                Icon(Icons.Rounded.Shuffle, null)
                Spacer(Modifier.width(8.dp))
                Text("Shuffle", style = MaterialTheme.typography.titleSmall)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
        Spacer(Modifier.height(8.dp))
    }
}
