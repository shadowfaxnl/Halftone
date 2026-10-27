package app.halftone.desktop.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Huisstijl Halftone ─────────────────────────────────────────────
val Ink = Color(0xFF0B0B10)
val Ink2 = Color(0xFF13121A)
val Ink3 = Color(0xFF1C1A25)
val Ink4 = Color(0xFF26232E)
val IconInk = Color(0xFF17121F)
val Line = Color(0xFF2A2734)
val Mist = Color(0xFFF1EFF6)
val MistDim = Color(0xFFB4B0C1)
val MistFaint = Color(0xFF858196)
val Coral = Color(0xFFFF9A5C)
val Rose = Color(0xFFE8457A)
val Violet = Color(0xFF7A5CFF)
val DefaultAccent = Coral

private fun TextStyle.display(weight: FontWeight, tracking: Float) = copy(
    fontFamily = BrandFonts.display,
    fontWeight = weight,
    letterSpacing = tracking.sp,
)

private fun TextStyle.body(weight: FontWeight) = copy(fontFamily = BrandFonts.body, fontWeight = weight)

/** Iets kleiner dan op de telefoon: op een bureaublad zit je dichter bij het scherm. */
fun halftoneTypography(): Typography {
    val b = Typography()
    return Typography(
        displayLarge = b.displayLarge.display(FontWeight.ExtraBold, -2.2f),
        displayMedium = b.displayMedium.display(FontWeight.ExtraBold, -1.8f),
        displaySmall = b.displaySmall.display(FontWeight.ExtraBold, -1.2f).copy(fontSize = 34.sp),
        headlineLarge = b.headlineLarge.display(FontWeight.ExtraBold, -1f).copy(fontSize = 30.sp),
        headlineMedium = b.headlineMedium.display(FontWeight.ExtraBold, -0.8f).copy(fontSize = 26.sp),
        headlineSmall = b.headlineSmall.display(FontWeight.Bold, -0.5f).copy(fontSize = 22.sp),
        titleLarge = b.titleLarge.display(FontWeight.Bold, -0.3f).copy(fontSize = 19.sp),
        titleMedium = b.titleMedium.body(FontWeight.Bold).copy(fontSize = 15.sp),
        titleSmall = b.titleSmall.body(FontWeight.Bold).copy(fontSize = 14.sp),
        bodyLarge = b.bodyLarge.body(FontWeight.Medium).copy(fontSize = 14.sp),
        bodyMedium = b.bodyMedium.body(FontWeight.Medium).copy(fontSize = 13.sp),
        bodySmall = b.bodySmall.body(FontWeight.Medium).copy(fontSize = 12.sp),
        labelLarge = b.labelLarge.body(FontWeight.Bold).copy(fontSize = 13.sp),
        labelMedium = b.labelMedium.body(FontWeight.Bold),
        labelSmall = b.labelSmall.body(FontWeight.ExtraBold).copy(letterSpacing = 1.6.sp),
    )
}

val HalftoneShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun HalftoneTheme(
    accent: Color = DefaultAccent,
    content: @Composable () -> Unit,
) {
    val animatedAccent by animateColorAsState(accent, tween(900), label = "accent")
    val onAccent = if (animatedAccent.luminance() > 0.45f) Ink else Color.White
    val typography = remember { halftoneTypography() }
    val scheme = darkColorScheme(
        primary = animatedAccent,
        onPrimary = onAccent,
        primaryContainer = animatedAccent.copy(alpha = 0.22f).compositeOver(Ink2),
        onPrimaryContainer = Mist,
        secondary = animatedAccent,
        onSecondary = onAccent,
        secondaryContainer = animatedAccent.copy(alpha = 0.16f).compositeOver(Ink3),
        onSecondaryContainer = Mist,
        tertiary = Violet,
        background = Ink,
        onBackground = Mist,
        surface = Ink,
        onSurface = Mist,
        surfaceVariant = Ink3,
        onSurfaceVariant = MistDim,
        surfaceContainerLowest = Ink,
        surfaceContainerLow = Ink2,
        surfaceContainer = Ink2,
        surfaceContainerHigh = Ink3,
        surfaceContainerHighest = Ink4,
        outline = Color(0xFF3A3744),
        outlineVariant = Line,
        error = Color(0xFFFF6B6B),
    )
    MaterialTheme(colorScheme = scheme, typography = typography, shapes = HalftoneShapes) {
        CompositionLocalProvider(LocalContentColor provides Mist, content = content)
    }
}
