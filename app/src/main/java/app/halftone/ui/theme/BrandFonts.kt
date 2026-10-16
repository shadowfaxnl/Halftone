@file:OptIn(ExperimentalTextApi::class)

package app.halftone.ui.theme

import android.content.Context
import android.content.res.AssetManager
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight

/**
 * De merklettertypes: Bricolage Grotesque (koppen) en Manrope (tekst).
 *
 * Gradle haalt ze bij de eerste build op naar `assets/fonts/` (zie app/build.gradle.kts).
 * Ontbreken ze, bijvoorbeeld omdat er bij het bouwen geen internet was, dan valt de app
 * zonder fouten terug op het systeemlettertype.
 */
object BrandFonts {
    private const val DISPLAY = "fonts/bricolage_grotesque.ttf"
    private const val BODY = "fonts/manrope.ttf"

    private var assets: AssetManager? = null
    private var hasDisplay = false
    private var hasBody = false

    fun init(context: Context) {
        val am = context.assets
        val files = runCatching { am.list("fonts")?.toSet() }.getOrNull().orEmpty()
        assets = am
        hasDisplay = "bricolage_grotesque.ttf" in files
        hasBody = "manrope.ttf" in files
    }

    /** Koppen: per gewicht één variabele instantie, met optische grootte voor display-formaat. */
    fun display(weight: FontWeight): FontFamily {
        val am = assets
        if (!hasDisplay || am == null) return FontFamily.Default
        return FontFamily(
            Font(
                path = DISPLAY,
                assetManager = am,
                weight = weight,
                variationSettings = FontVariation.Settings(
                    FontVariation.weight(weight.weight),
                    FontVariation.Setting("opsz", 72f),
                ),
            ),
        )
    }

    val body: FontFamily by lazy {
        val am = assets
        if (!hasBody || am == null) {
            FontFamily.Default
        } else {
            FontFamily(
                listOf(400, 500, 600, 700, 800).map { w ->
                    Font(path = BODY, assetManager = am, weight = FontWeight(w))
                },
            )
        }
    }
}
