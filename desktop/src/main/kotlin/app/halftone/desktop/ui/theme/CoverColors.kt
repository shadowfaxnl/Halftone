package app.halftone.desktop.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Immutable
data class CoverColors(
    val accent: Color,
    val backdrop: Color,
    val backdropDeep: Color,
)

val DefaultCoverColors = CoverColors(
    accent = DefaultAccent,
    backdrop = Color(0xFF3A1A2C),
    backdropDeep = Color(0xFF140A12),
)

private val cache = ConcurrentHashMap<String, CoverColors>()

/**
 * De kleuren van een hoesje. Haalt een kleine versie op (64 px) en kiest
 * een levendige accentkleur en een donkere basistint.
 */
@Composable
fun rememberCoverColors(http: OkHttpClient, smallCoverUrl: String?): CoverColors {
    var colors by remember { mutableStateOf(smallCoverUrl?.let { cache[it] } ?: DefaultCoverColors) }
    LaunchedEffect(smallCoverUrl) {
        if (smallCoverUrl == null) {
            colors = DefaultCoverColors
            return@LaunchedEffect
        }
        cache[smallCoverUrl]?.let {
            colors = it
            return@LaunchedEffect
        }
        val extracted = withContext(Dispatchers.IO) {
            runCatching {
                http.newCall(Request.Builder().url(smallCoverUrl).build()).execute().use { r ->
                    val bytes = r.body?.bytes() ?: return@use null
                    ImageIO.read(ByteArrayInputStream(bytes))?.let(::extract)
                }
            }.getOrNull()
        }
        if (extracted != null) {
            cache[smallCoverUrl] = extracted
            colors = extracted
        }
    }
    return colors
}

private data class Hsl(val h: Float, val s: Float, val l: Float)

private fun toHsl(rgb: Int): Hsl {
    val r = (rgb shr 16 and 0xFF) / 255f
    val g = (rgb shr 8 and 0xFF) / 255f
    val b = (rgb and 0xFF) / 255f
    val mx = max(r, max(g, b))
    val mn = min(r, min(g, b))
    val l = (mx + mn) / 2f
    val d = mx - mn
    if (d == 0f) return Hsl(0f, 0f, l)
    val s = d / (1f - abs(2f * l - 1f))
    val h = when (mx) {
        r -> 60f * (((g - b) / d) % 6f)
        g -> 60f * (((b - r) / d) + 2f)
        else -> 60f * (((r - g) / d) + 4f)
    }.let { if (it < 0) it + 360f else it }
    return Hsl(h, s.coerceIn(0f, 1f), l)
}

private fun fromHsl(h: Float, s: Float, l: Float): Color {
    val c = (1f - abs(2f * l - 1f)) * s
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = l - c / 2f
    val (r, g, b) = when {
        h < 60 -> Triple(c, x, 0f)
        h < 120 -> Triple(x, c, 0f)
        h < 180 -> Triple(0f, c, x)
        h < 240 -> Triple(0f, x, c)
        h < 300 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color((r + m).coerceIn(0f, 1f), (g + m).coerceIn(0f, 1f), (b + m).coerceIn(0f, 1f))
}

/** Groepeert pixels per tint en kiest de levendigste groep als accent, de grootste als basis. */
private fun extract(img: BufferedImage): CoverColors {
    val bins = 24
    val weight = FloatArray(bins)
    val count = IntArray(bins)
    val sumS = FloatArray(bins)
    val sumL = FloatArray(bins)
    val sumX = FloatArray(bins)
    val sumY = FloatArray(bins)
    var greyCount = 0
    var total = 0
    val step = max(1, max(img.width, img.height) / 48)
    var y = 0
    while (y < img.height) {
        var x = 0
        while (x < img.width) {
            val p = toHsl(img.getRGB(x, y))
            total++
            if (p.s < 0.12f || p.l < 0.08f || p.l > 0.94f) {
                greyCount++
            } else {
                val i = ((p.h / 360f) * bins).toInt().coerceIn(0, bins - 1)
                count[i]++
                // Levendig = verzadigd en niet te donker of te licht.
                weight[i] += p.s * (1f - abs(p.l - 0.55f) * 1.4f).coerceAtLeast(0.05f)
                sumS[i] += p.s
                sumL[i] += p.l
                val rad = Math.toRadians(p.h.toDouble())
                sumX[i] += Math.cos(rad).toFloat()
                sumY[i] += Math.sin(rad).toFloat()
            }
            x += step
        }
        y += step
    }
    if (total == 0 || greyCount > total * 0.92f) {
        // Grijs hoesje: neutrale tinten in plaats van verzonnen kleur.
        return CoverColors(Color(0xFFD9D6E0), Color(0xFF26242C), Color(0xFF0E0D12))
    }
    fun hueOf(i: Int): Float {
        val deg = Math.toDegrees(Math.atan2(sumY[i].toDouble(), sumX[i].toDouble())).toFloat()
        return if (deg < 0) deg + 360f else deg
    }
    val vivid = weight.indices.maxBy { weight[it] }
    val dominant = count.indices.maxBy { count[it] }
    val accentS = (sumS[vivid] / count[vivid].coerceAtLeast(1)).coerceIn(0.45f, 0.95f)
    val baseS = (sumS[dominant] / count[dominant].coerceAtLeast(1)).coerceIn(0.25f, 0.6f)
    return CoverColors(
        accent = fromHsl(hueOf(vivid), accentS, 0.66f),
        backdrop = fromHsl(hueOf(dominant), baseS, 0.2f),
        backdropDeep = fromHsl(hueOf(dominant), (baseS * 0.8f).coerceIn(0.15f, 0.45f), 0.07f),
    )
}
