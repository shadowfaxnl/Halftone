package app.halftone.ui.theme

import android.content.Context
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** De drie kleuren die we uit een hoesje halen. */
@Immutable
data class CoverColors(
    /** Helder genoeg om op zwart te lezen: knoppen, actieve items. */
    val accent: Color,
    /** Donkere, verzadigde tint voor achtergrondverlopen. */
    val backdrop: Color,
    /** Bijna zwart met een zweem van het hoesje. */
    val backdropDeep: Color,
)

val DefaultCoverColors = CoverColors(
    accent = DefaultAccent,
    backdrop = Color(0xFF3A1A2C),
    backdropDeep = Color(0xFF140A12),
)

private object CoverColorCache {
    private val cache = LruCache<String, CoverColors>(96)
    operator fun get(key: String): CoverColors? = cache.get(key)
    operator fun set(key: String, value: CoverColors) {
        cache.put(key, value)
    }
}

/**
 * Geeft de kleuren van een hoesje terug. Tijdens het laden blijft de vorige
 * waarde staan, zodat er nooit een flits naar de standaardkleur is.
 */
@Composable
fun rememberCoverColors(url: String?): CoverColors {
    val context = LocalContext.current
    var colors by remember { mutableStateOf(url?.let { CoverColorCache[it] } ?: DefaultCoverColors) }
    LaunchedEffect(url) {
        if (url == null) {
            colors = DefaultCoverColors
            return@LaunchedEffect
        }
        val cached = CoverColorCache[url]
        if (cached != null) {
            colors = cached
            return@LaunchedEffect
        }
        extractCoverColors(context, url)?.let {
            CoverColorCache[url] = it
            colors = it
        }
    }
    return colors
}

private suspend fun extractCoverColors(context: Context, url: String): CoverColors? {
    val request = ImageRequest.Builder(context)
        .data(url)
        .size(160)
        .allowHardware(false)
        .build()
    val result = context.imageLoader.execute(request) as? SuccessResult ?: return null
    val bitmap = result.image.toBitmap()
    return withContext(Dispatchers.Default) {
        val palette = Palette.from(bitmap).maximumColorCount(24).generate()
        fromPalette(palette)
    }
}

private fun fromPalette(p: Palette): CoverColors {
    val accentRaw = p.vibrantSwatch?.rgb
        ?: p.lightVibrantSwatch?.rgb
        ?: p.mutedSwatch?.rgb
        ?: p.dominantSwatch?.rgb
        ?: return DefaultCoverColors
    val baseRaw = p.darkVibrantSwatch?.rgb
        ?: p.darkMutedSwatch?.rgb
        ?: p.dominantSwatch?.rgb
        ?: accentRaw

    return CoverColors(
        accent = tone(accentRaw, minL = 0.60f, maxL = 0.74f, minS = 0.45f, maxS = 0.95f),
        backdrop = tone(baseRaw, minL = 0.17f, maxL = 0.24f, minS = 0.25f, maxS = 0.60f),
        backdropDeep = tone(baseRaw, minL = 0.06f, maxL = 0.08f, minS = 0.15f, maxS = 0.45f),
    )
}

/** Brengt een kleur binnen een vaste helderheid en verzadiging, zodat elk hoesje goed oogt. */
private fun tone(rgb: Int, minL: Float, maxL: Float, minS: Float, maxS: Float): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(rgb, hsl)
    // Grijze hoesjes blijven grijs: geen roze tint verzinnen.
    hsl[1] = if (hsl[1] < 0.08f) hsl[1] else hsl[1].coerceIn(minS, maxS)
    hsl[2] = hsl[2].coerceIn(minL, maxL)
    return Color(ColorUtils.HSLToColor(hsl))
}
