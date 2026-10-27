package app.halftone.desktop.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.halftone.desktop.ui.theme.Coral
import app.halftone.desktop.ui.theme.IconInk
import app.halftone.desktop.ui.theme.Rose
import app.halftone.desktop.ui.theme.Violet
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/** Kleurverloop van het logo: koraal → roze → violet. */
fun halftoneRamp(t: Float): Color =
    if (t < 0.55f) lerp(Coral, Rose, t / 0.55f) else lerp(Rose, Violet, (t - 0.55f) / 0.45f)

private data class Dot(val x: Float, val y: Float, val r: Float, val t: Float)

/** De punten van het logo, in eenheden van een 108×108-vlak (zoals het app-icoon). */
private val logoDots: List<Dot> by lazy {
    val out = mutableListOf<Dot>()
    val c = sqrt(0.5f)
    val step = 6.6f
    val radius = 32f
    for (i in -8..8) for (j in -8..8) {
        val u = i * step
        val v = j * step
        val x = 54f + (u - v) * c
        val y = 54f + (u + v) * c
        if (hypot(x - 54f, y - 54f) > radius - 1.2f) continue
        val t = (((x - 54f) - (y - 54f)) / (2 * radius * 0.95f) + 0.5f).coerceIn(0f, 1f)
        out += Dot(x, y, 3.3f + (0.85f - 3.3f) * t, t)
    }
    out
}

/**
 * Het Halftone-logo: een cirkel van rasterpunten.
 * [tile] tekent het donkere vlak eromheen, zoals op het app-icoon.
 * [tint] maakt het éénkleurig.
 */
@Composable
fun HalftoneMark(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tile: Boolean = false,
    tint: Color? = null,
) {
    Canvas(
        modifier
            .size(size)
            .semantics { contentDescription = "Halftone" },
    ) {
        val scale = this.size.minDimension / 108f
        if (tile) {
            drawRoundRect(
                color = IconInk,
                size = Size(108f * scale, 108f * scale),
                cornerRadius = CornerRadius(26f * scale),
            )
        }
        // Zonder tegel mag het logo de hele ruimte vullen.
        val zoom = if (tile) 1f else 108f / 66f
        val offset = if (tile) 0f else -21f
        logoDots.forEach { d ->
            drawCircle(
                color = tint ?: halftoneRamp(d.t),
                radius = d.r * scale * zoom,
                center = Offset((d.x + offset) * scale * zoom, (d.y + offset) * scale * zoom),
            )
        }
    }
}

/** Logo plus woordmerk, zoals op de website. */
@Composable
fun HalftoneLockup(
    modifier: Modifier = Modifier,
    markSize: Dp = 44.dp,
    textSize: TextUnit = 40.sp,
    style: TextStyle = MaterialTheme.typography.displaySmall,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        HalftoneMark(size = markSize)
        Spacer(Modifier.width(14.dp))
        Text("halftone", style = style.copy(fontSize = textSize, letterSpacing = (-0.045f * textSize.value).sp))
    }
}

/**
 * Een groot, langzaam ademend rasterveld in de merkkleuren.
 * Punten worden groter naar linksonder en kleiner naar rechtsboven, net als in het logo.
 */
@Composable
fun HalftoneField(
    modifier: Modifier = Modifier,
    step: Dp = 22.dp,
    maxRadius: Dp = 7.dp,
    alpha: Float = 0.55f,
    animated: Boolean = true,
) {
    val transition = rememberInfiniteTransition(label = "field")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val c = remember { sqrt(0.5f) }
    Canvas(modifier) {
        val s = step.toPx()
        val rMax = maxRadius.toPx()
        val rMin = rMax * 0.12f
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val n = (hypot(w, h) / s).toInt() / 2 + 3
        val p = if (animated) phase else 0f
        for (i in -n..n) for (j in -n..n) {
            val u = i * s
            val v = j * s
            val x = cx + (u - v) * c
            val y = cy + (u + v) * c
            if (x < -rMax || y < -rMax || x > w + rMax || y > h + rMax) continue
            val t = (((x - cx) - (y - cy)) / (w + h) + 0.5f).coerceIn(0f, 1f)
            // Een zachte golf die over het raster trekt.
            val wave = 0.82f + 0.18f * sin(p + (x + y) / (s * 9f))
            val r = (rMax + (rMin - rMax) * t) * wave
            drawCircle(halftoneRamp(t).copy(alpha = alpha), r, Offset(x, y))
        }
    }
}
