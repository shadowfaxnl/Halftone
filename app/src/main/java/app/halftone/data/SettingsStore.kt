package app.halftone.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "halftone_settings")

/** Voorkeuren die de gebruiker zelf kan instellen. */
data class Prefs(
    /** Luistergedrag doorgeven aan de server (en via Navidrome eventueel aan Last.fm/ListenBrainz). */
    val scrobble: Boolean = true,
    /** Maximale bitrate bij streamen in kbps; 0 = origineel. */
    val maxBitRate: Int = 0,
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val login: Flow<Credentials?> = store.data.map { prefs ->
        val server = prefs[SERVER]
        val user = prefs[USER]
        val pass = prefs[PASS]
        val salt = prefs[SALT]
        if (server != null && user != null && pass != null && salt != null) {
            Credentials(server, user, pass, salt)
        } else {
            null
        }
    }

    val prefs: Flow<Prefs> = store.data.map { prefs ->
        Prefs(
            scrobble = prefs[SCROBBLE] ?: true,
            maxBitRate = prefs[MAX_BITRATE] ?: 0,
        )
    }

    suspend fun save(credentials: Credentials) {
        store.edit {
            it[SERVER] = credentials.serverUrl
            it[USER] = credentials.username
            it[PASS] = credentials.password
            it[SALT] = credentials.salt
        }
    }

    suspend fun setScrobble(enabled: Boolean) {
        store.edit { it[SCROBBLE] = enabled }
    }

    suspend fun setMaxBitRate(kbps: Int) {
        store.edit { it[MAX_BITRATE] = kbps }
    }

    /** Wist alleen de inloggegevens; voorkeuren blijven staan. */
    suspend fun clearLogin() {
        store.edit {
            it.remove(SERVER)
            it.remove(USER)
            it.remove(PASS)
            it.remove(SALT)
        }
    }

    private companion object {
        val SERVER = stringPreferencesKey("server")
        val USER = stringPreferencesKey("user")
        val PASS = stringPreferencesKey("pass")
        val SALT = stringPreferencesKey("salt")
        val SCROBBLE = booleanPreferencesKey("scrobble")
        val MAX_BITRATE = intPreferencesKey("max_bitrate")
    }
}
