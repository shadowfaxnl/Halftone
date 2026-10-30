package app.halftone.desktop

import java.awt.Toolkit
import java.io.File

/**
 * Zorgt dat GNOME, KDE en andere Linux-bureaubladen Halftone herkennen:
 * de juiste naam en het juiste icoon in de taakbalk, het dock en Alt+Tab.
 */
object DesktopIntegration {

    /** Moet overeenkomen met StartupWMClass in halftone.desktop. */
    const val WM_CLASS = "halftone"

    /** Herkent de snelkoppeling die Halftone zelf heeft aangemaakt. */
    private const val MARKER = "X-Halftone-SelfInstalled=true"

    /**
     * Java geeft vensters standaard een WM_CLASS als "java-lang-Thread", waardoor het bureaublad
     * het venster niet aan Halftone koppelt (grijs tandwiel, verkeerde naam). Dit zet de naam goed.
     * Moet gebeuren voordat het eerste venster opent. Vereist
     * --add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED (staat in build.gradle.kts).
     */
    fun setWindowClass() {
        runCatching {
            val toolkit = Toolkit.getDefaultToolkit()
            val field = toolkit.javaClass.getDeclaredField("awtAppClassName")
            field.isAccessible = true
            field.set(toolkit, WM_CLASS)
        }
    }

    /**
     * Een AppImage installeert zichzelf niet. Draait Halftone als AppImage, dan zetten we een
     * snelkoppeling en icoon in de gebruikersmap, zodat Halftone in het app-menu staat en het
     * bureaublad het juiste icoon toont. Bij elke start bijgewerkt, voor als de AppImage
     * verplaatst of vernieuwd is. Verwijderen: de twee bestanden hieronder weggooien.
     *
     * Heeft Gear Lever of AppImageLauncher de AppImage al gekoppeld, dan laten we het aan hen over
     * en halen we een eventuele eigen snelkoppeling weg, zodat Halftone niet dubbel in het menu staat.
     */
    fun installLauncher() {
        val appImage = System.getenv("APPIMAGE")?.takeIf { it.isNotBlank() } ?: return
        runCatching {
            val home = System.getProperty("user.home")
            val dataHome = System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() } ?: "$home/.local/share"
            val applications = File(dataHome, "applications")
            val ownFile = File(applications, "halftone.desktop")

            val integratedElsewhere = applications.listFiles { f -> f.isFile && f.name.endsWith(".desktop") && f != ownFile }
                .orEmpty()
                .any { f -> runCatching { f.readText() }.getOrDefault("").let { it.contains(appImage) && !it.contains(MARKER) } }
            if (integratedElsewhere) {
                if (ownFile.exists() && ownFile.readText().contains(MARKER)) ownFile.delete()
                return
            }

            val icon = File(dataHome, "icons/hicolor/512x512/apps/halftone.png")
            if (!icon.exists()) {
                icon.parentFile.mkdirs()
                DesktopIntegration::class.java.getResourceAsStream("/halftone.png")?.use { input ->
                    icon.outputStream().use { input.copyTo(it) }
                }
            }

            val exec = "\"" + appImage.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
            val entry = """
                [Desktop Entry]
                Type=Application
                Name=Halftone
                GenericName=Music Player
                GenericName[nl]=Muziekspeler
                Comment=A beautiful player for Subsonic and Navidrome servers
                Comment[nl]=Een prachtige speler voor Subsonic- en Navidrome-servers
                Exec=$exec %U
                TryExec=$appImage
                Icon=halftone
                Terminal=false
                Categories=AudioVideo;Audio;Player;
                Keywords=music;subsonic;navidrome;player;audio;
                StartupWMClass=$WM_CLASS
                $MARKER
            """.trimIndent() + "\n"

            val desktopFile = ownFile
            if (!desktopFile.exists() || desktopFile.readText() != entry) {
                desktopFile.parentFile.mkdirs()
                desktopFile.writeText(entry)
                // Bureaubladen opnieuw laten kijken; bestaat het hulpprogramma niet, dan pikken ze het later op.
                runCatching {
                    ProcessBuilder("update-desktop-database", desktopFile.parentFile.absolutePath)
                        .redirectErrorStream(true).start()
                }
            }
        }
    }
}
