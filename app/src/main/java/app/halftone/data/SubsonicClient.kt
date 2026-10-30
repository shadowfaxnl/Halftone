package app.halftone.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.security.SecureRandom
import app.halftone.R
import app.halftone.Str

const val API_VERSION = "1.16.1"
const val CLIENT_NAME = "Halftone"

/** Groottes voor hoesjes. Vaste waarden houden de beeldcache effectief. */
const val COVER_SMALL = 300
const val COVER_LARGE = 900

/**
 * Inloggegevens. Het zout wordt eenmalig bij het inloggen gemaakt en bewaard,
 * zodat URL's voor hoesjes en streams stabiel zijn en goed gecachet worden.
 */
data class Credentials(
    val serverUrl: String,
    val username: String,
    val password: String,
    val salt: String,
) {
    val token: String by lazy { md5(password + salt) }
}

class SubsonicException(val code: Int, message: String) : Exception(message)

class SubsonicClient(
    private val http: OkHttpClient,
    private val json: Json,
) {
    @Volatile
    var credentials: Credentials? = null

    /** Maximale bitrate voor streamen in kbps; 0 betekent het originele bestand. */
    @Volatile
    var maxBitRate: Int = 0

    private fun requireCredentials(): Credentials =
        credentials ?: throw SubsonicException(10, Str.get(R.string.error_not_logged_in))

    fun url(
        endpoint: String,
        params: Map<String, Any?> = emptyMap(),
        creds: Credentials = requireCredentials(),
    ): HttpUrl {
        val builder = creds.serverUrl.toHttpUrl().newBuilder()
            .addPathSegment("rest")
            .addPathSegment(endpoint)
            .addQueryParameter("u", creds.username)
            .addQueryParameter("t", creds.token)
            .addQueryParameter("s", creds.salt)
            .addQueryParameter("v", API_VERSION)
            .addQueryParameter("c", CLIENT_NAME)
        params.forEach { (key, value) ->
            when (value) {
                null -> Unit
                // Subsonic herhaalt een parameter voor lijsten: songId=1&songId=2
                is Iterable<*> -> value.filterNotNull().forEach { builder.addQueryParameter(key, it.toString()) }
                else -> builder.addQueryParameter(key, value.toString())
            }
        }
        return builder.build()
    }

    suspend fun call(
        endpoint: String,
        params: Map<String, Any?> = emptyMap(),
        creds: Credentials = requireCredentials(),
    ): SubsonicResponse = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url(endpoint, params + ("f" to "json"), creds))
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw SubsonicException(response.code, Str.get(R.string.error_http, response.code))
            }
            val body = response.body?.string()
                ?: throw SubsonicException(0, Str.get(R.string.error_empty_response))
            val result = json.decodeFromString<Envelope>(body).response
            if (result.status != "ok") {
                throw SubsonicException(result.error?.code ?: 0, friendlyError(result.error))
            }
            result
        }
    }

    suspend fun ping(creds: Credentials) {
        call("ping", creds = creds)
    }

    fun coverArtUrl(coverArt: String?, size: Int = COVER_SMALL): String? {
        if (coverArt.isNullOrBlank()) return null
        val creds = credentials ?: return null
        return url("getCoverArt", mapOf("id" to coverArt, "size" to size), creds).toString()
    }

    fun streamUrl(songId: String): String =
        url("stream", mapOf("id" to songId, "maxBitRate" to maxBitRate.takeIf { it > 0 })).toString()

    /** Het originele bestand, onbewerkt. Gebruikt voor offline downloads. */
    fun downloadUrl(songId: String): String =
        url("download", mapOf("id" to songId)).toString()

    private fun friendlyError(error: SubsonicError?): String = when (error?.code) {
        40 -> Str.get(R.string.error_wrong_credentials)
        41 -> Str.get(R.string.error_no_token_auth)
        50 -> Str.get(R.string.error_no_access)
        70 -> Str.get(R.string.error_not_found)
        else -> error?.message ?: Str.get(R.string.error_server_unknown)
    }
}

/**
 * Maakt van wat iemand intypt een bruikbaar adres.
 * Zonder schema: http voor lokale adressen en adressen met poort, anders https.
 */
fun normalizeServerUrl(input: String): String {
    var url = input.trim().trimEnd('/')
    if (url.endsWith("/rest")) url = url.removeSuffix("/rest")
    if (!url.startsWith("http://") && !url.startsWith("https://")) {
        val host = url.substringBefore('/')
        val looksLocal = host.contains(':') ||
            host.endsWith(".local") ||
            host.endsWith(".lan") ||
            host == "localhost" ||
            Regex("""^\d{1,3}(\.\d{1,3}){3}$""").matches(host)
        url = (if (looksLocal) "http://" else "https://") + url
    }
    return url
}

fun randomSalt(): String {
    val bytes = ByteArray(8)
    SecureRandom().nextBytes(bytes)
    return bytes.joinToString("") { "%02x".format(it) }
}

private fun md5(input: String): String =
    MessageDigest.getInstance("MD5")
        .digest(input.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
