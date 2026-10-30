import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.net.URI

plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.0"
    id("org.jetbrains.compose") version "1.8.2"
}

group = "app.halftone"
version = "0.5.0"

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt.coil3:coil-compose:3.2.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.2.0")

    // Afspelen via libmpv (van het systeem) en mediatoetsen via MPRIS/D-Bus.
    implementation("net.java.dev.jna:jna:5.17.0")
    implementation("com.github.hypfvieh:dbus-java-core:5.1.1")
    implementation("com.github.hypfvieh:dbus-java-transport-native-unixsocket:5.1.1")
    implementation("org.slf4j:slf4j-nop:2.0.17")
}

compose.desktop {
    application {
        mainClass = "app.halftone.desktop.MainKt"
        jvmArgs += listOf(
            "-Dsun.java2d.uiScale.enabled=true",
            "-Dawt.useSystemAAFontSettings=on",
            // Nodig om de vensternaam (WM_CLASS) te zetten, zie DesktopIntegration.
            "--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED",
        )

        buildTypes.release.proguard {
            // JNA, D-Bus en kotlinx.serialization werken met reflectie; niet verkleinen.
            isEnabled.set(false)
        }

        nativeDistributions {
            targetFormats(TargetFormat.AppImage)
            packageName = "halftone"
            packageVersion = "0.5.0"
            description = "Music player for Navidrome and Subsonic servers"
            vendor = "shadowfaxnl"
            licenseFile.set(rootProject.file("../LICENSE"))
            includeAllModules = true
            linux {
                iconFile.set(project.file("packaging/halftone.png"))
                appCategory = "AudioVideo"
                menuGroup = "AudioVideo;Audio;Player"
            }
        }
    }
}

// ── Merklettertypes ────────────────────────────────────────────────
// Haalt Bricolage Grotesque en Manrope (SIL Open Font License) als losse gewichten op.
// Google Fonts levert aan een eenvoudige client gewone .ttf-bestanden per gewicht.
// Lukt het niet, dan gebruikt de app het systeemlettertype.
val fetchBrandFonts by tasks.registering {
    val fontDir = layout.projectDirectory.dir("src/main/resources/fonts").asFile
    outputs.dir(fontDir)
    doLast {
        fontDir.mkdirs()
        if ((fontDir.listFiles()?.count { it.name.endsWith(".ttf") } ?: 0) >= 8) return@doLast
        val css = runCatching {
            val url = URI(
                "https://fonts.googleapis.com/css2?family=Bricolage+Grotesque:wght@500;700;800" +
                    "&family=Manrope:wght@400;500;600;700;800",
            ).toURL()
            val conn = url.openConnection()
            conn.setRequestProperty("User-Agent", "curl/8.5.0")
            conn.getInputStream().bufferedReader().readText()
        }.getOrNull()
        if (css == null) {
            logger.warn("Halftone: lettertypes niet opgehaald; de app gebruikt het systeemlettertype.")
            return@doLast
        }
        val block = Regex("""font-family:\s*'([^']+)';.*?font-weight:\s*(\d+);.*?src:\s*url\(([^)]+)\)""", RegexOption.DOT_MATCHES_ALL)
        block.findAll(css).forEach { m ->
            val (family, weight, src) = m.destructured
            val name = family.lowercase().replace(" ", "_") + "-" + weight + ".ttf"
            val target = File(fontDir, name)
            if (target.exists() && target.length() > 10_000) return@forEach
            runCatching {
                target.writeBytes(URI(src).toURL().readBytes())
                logger.lifecycle("Halftone: $name opgehaald")
            }.onFailure { target.delete() }
        }
    }
}

tasks.named("processResources") { dependsOn(fetchBrandFonts) }
