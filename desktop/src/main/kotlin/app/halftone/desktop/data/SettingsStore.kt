package app.halftone.desktop.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions

/** Mappen volgens de XDG-standaard, zoals elke Linux-app ze hoort te gebruiken. */
object Paths {
    private val home = System.getProperty("user.home")

    val config: File = File(System.getenv("XDG_CONFIG_HOME")?.takeIf { it.isNotBlank() } ?: "$home/.config", "halftone")
    val data: File = File(System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() } ?: "$home/.local/share", "halftone")
    val cache: File = File(System.getenv("XDG_CACHE_HOME")?.takeIf { it.isNotBlank() } ?: "$home/.cache", "halftone")

    init {
        listOf(config, data, cache).forEach { it.mkdirs() }
    }
}

@Serializable
data class StoredSettings(
    val server: String? = null,
    val username: String? = null,
    val password: String? = null,
    val salt: String? = null,
    val scrobble: Boolean = true,
    val maxBitRate: Int = 0,
    val volume: Int = 80,
    /** "system", "en" of "nl". */
    val language: String = "system",
    val windowWidth: Int = 1280,
    val windowHeight: Int = 820,
) {
    val credentials: Credentials?
        get() = if (server != null && username != null && password != null && salt != null) {
            Credentials(server, username, password, salt)
        } else {
            null
        }
}

/**
 * Instellingen in ~/.config/halftone/settings.json.
 * Het bestand is alleen leesbaar voor jouw gebruiker (rechten 600), omdat het je wachtwoord bevat.
 */
class SettingsStore(private val json: Json) {
    private val file = File(Paths.config, "settings.json")
    private val _state = MutableStateFlow(load())
    val state: StateFlow<StoredSettings> = _state.asStateFlow()

    private fun load(): StoredSettings = runCatching {
        if (file.exists()) json.decodeFromString(StoredSettings.serializer(), file.readText()) else StoredSettings()
    }.getOrDefault(StoredSettings())

    @Synchronized
    fun update(transform: (StoredSettings) -> StoredSettings) {
        val next = transform(_state.value)
        if (next == _state.value) return
        _state.value = next
        runCatching {
            val tmp = File(file.parentFile, "settings.json.tmp")
            tmp.writeText(json.encodeToString(StoredSettings.serializer(), next))
            runCatching {
                Files.setPosixFilePermissions(tmp.toPath(), PosixFilePermissions.fromString("rw-------"))
            }
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
        }
    }

    fun saveLogin(c: Credentials) = update {
        it.copy(server = c.serverUrl, username = c.username, password = c.password, salt = c.salt)
    }

    fun clearLogin() = update { it.copy(server = null, username = null, password = null, salt = null) }
}
