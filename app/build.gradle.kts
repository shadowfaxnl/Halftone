import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.halftone"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.halftone"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Ondertekend met de debug-sleutel zodat je de release-build direct kunt installeren.
            // Vervang dit door je eigen keystore voordat je de app verspreidt.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    lint {
        // Media3 markeert de download- en cache-API's als "unstable"; we gebruiken ze bewust.
        disable += "UnsafeOptInUsageError"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.okhttp)
    implementation(libs.androidx.media3.database)

    implementation(libs.androidx.palette)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.coil.compose)
    implementation(libs.coil.okhttp)
}

// ── Merklettertypes ────────────────────────────────────────────────
// Bricolage Grotesque en Manrope (beide SIL Open Font License) worden bij de eerste
// build opgehaald naar src/main/assets/fonts. Lukt dat niet, dan bouwt de app gewoon
// verder en gebruikt hij het systeemlettertype. Je kunt de bestanden ook zelf neerzetten.
val brandFonts = mapOf(
    "manrope.ttf" to listOf(
        "https://github.com/google/fonts/raw/main/ofl/manrope/Manrope%5Bwght%5D.ttf",
    ),
    "bricolage_grotesque.ttf" to listOf(
        "https://github.com/google/fonts/raw/main/ofl/bricolagegrotesque/BricolageGrotesque%5Bopsz%2Cwdth%2Cwght%5D.ttf",
        "https://github.com/google/fonts/raw/main/ofl/bricolagegrotesque/BricolageGrotesque%5Bopsz%2Cwght%5D.ttf",
    ),
)

val fetchBrandFonts by tasks.registering {
    val fontDir = layout.projectDirectory.dir("src/main/assets/fonts").asFile
    outputs.dir(fontDir)
    doLast {
        fontDir.mkdirs()
        brandFonts.forEach { (name, urls) ->
            val target = File(fontDir, name)
            if (target.exists() && target.length() > 10_000) return@forEach
            val ok = urls.any { url ->
                runCatching {
                    URI(url).toURL().openStream().use { input ->
                        target.outputStream().use { input.copyTo(it) }
                    }
                    target.length() > 10_000
                }.getOrDefault(false)
            }
            if (ok) {
                logger.lifecycle("Halftone: lettertype $name opgehaald")
            } else {
                target.delete()
                logger.warn("Halftone: kon $name niet downloaden; de app gebruikt dan het systeemlettertype.")
            }
        }
    }
}

tasks.named("preBuild") { dependsOn(fetchBrandFonts) }
