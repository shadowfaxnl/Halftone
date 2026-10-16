package app.halftone.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
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
// Inkt: bijna zwart met een vleugje violet, zoals drukinkt op donker papier.
val Ink = Color(0xFF0B0B10)
val Ink2 = Color(0xFF13121A)
val Ink3 = Color(0xFF1C1A25)
val Ink4 = Color(0xFF26232E)
val IconInk = Color(0xFF17121F)
val Mist = Color(0xFFF1EFF6)
val MistDim = Color(0xFFB4B0C1)
val MistFaint = Color(0xFF858196)

// De drie merkkleuren uit het logo: van groot en warm naar klein en koel.
val Coral = Color(0xFFFF9A5C)
val Rose = Color(0xFFE8457A)
val Violet = Color(0xFF7A5CFF)

/** Accent zolang er niets speelt; daarna neemt het hoesje het over. */
val DefaultAccent = Coral

private fun TextStyle.display(weight: FontWeight, tracking: Float) = copy(
    fontFamily = BrandFonts.display(weight),
    fontWeight = weight,
    letterSpacing = tracking.sp,
)

private fun TextStyle.body(weight: FontWeight) = copy(
    fontFamily = BrandFonts.body,
    fontWeight = weight,
)

/** Bricolage Grotesque voor koppen, Manrope voor al het andere. */
fun halftoneTypography(): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.display(FontWeight.ExtraBold, -2.2f),
        displayMedium = base.displayMedium.display(FontWeight.ExtraBold, -1.8f),
        displaySmall = base.displaySmall.display(FontWeight.ExtraBold, -1.2f),
        headlineLarge = base.headlineLarge.display(FontWeight.ExtraBold, -1f),
        headlineMedium = base.headlineMedium.display(FontWeight.ExtraBold, -0.8f),
        headlineSmall = base.headlineSmall.display(FontWeight.Bold, -0.5f),
        titleLarge = base.titleLarge.display(FontWeight.Bold, -0.3f),
        titleMedium = base.titleMedium.body(FontWeight.Bold),
        titleSmall = base.titleSmall.body(FontWeight.Bold),
        bodyLarge = base.bodyLarge.body(FontWeight.Medium),
        bodyMedium = base.bodyMedium.body(FontWeight.Medium),
        bodySmall = base.bodySmall.body(FontWeight.Medium),
        labelLarge = base.labelLarge.body(FontWeight.Bold),
        labelMedium = base.labelMedium.body(FontWeight.Bold),
        labelSmall = base.labelSmall.body(FontWeight.ExtraBold).copy(letterSpacing = 1.6.sp),
    )
}

val HalftoneShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

/**
 * Altijd donker. De accentkleur komt uit het hoesje dat op dat moment speelt
 * en vloeit langzaam over wanneer het nummer wisselt.
 */
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
        outlineVariant = Color(0xFF2A2734),
        error = Color(0xFFFF6B6B),
    )
    MaterialTheme(
        colorScheme = scheme,
        typography = typography,
        shapes = HalftoneShapes,
        content = content,
    )
}
