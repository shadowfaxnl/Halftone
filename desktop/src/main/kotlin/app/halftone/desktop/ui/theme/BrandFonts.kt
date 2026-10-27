package app.halftone.desktop.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font

/**
 * Bricolage Grotesque (koppen) en Manrope (tekst), als losse gewichten in de app meegeleverd.
 * Ontbreken ze, dan valt de app terug op het systeemlettertype.
 */
object BrandFonts {
    private fun load(prefix: String, weights: List<Int>): FontFamily {
        val fonts: List<Font> = weights.mapNotNull { w ->
            val path = "/fonts/$prefix-$w.ttf"
            val bytes = BrandFonts::class.java.getResourceAsStream(path)?.use { it.readBytes() } ?: return@mapNotNull null
            Font(identity = "$prefix-$w", data = bytes, weight = FontWeight(w), style = FontStyle.Normal)
        }
        return if (fonts.isEmpty()) FontFamily.Default else FontFamily(fonts)
    }

    val display: FontFamily by lazy { load("bricolage_grotesque", listOf(500, 700, 800)) }
    val body: FontFamily by lazy { load("manrope", listOf(400, 500, 600, 700, 800)) }
}
