package app.halftone.playback

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import app.halftone.HalftoneApp
import app.halftone.data.COVER_SMALL
import okhttp3.Request
import java.io.File
import java.io.FileNotFoundException

/**
 * Levert albumhoesjes aan Android Auto.
 *
 * Auto toont in zijn bladerlijsten alleen afbeeldingen via content://-adressen, niet via http.
 * Deze provider haalt het hoesje één keer van je server, bewaart het in de cache van de app
 * en geeft het bestand door. Hij is niet geëxporteerd: alleen apps die wij expliciet
 * leesrechten geven (Android Auto) kunnen hem gebruiken.
 */
class ArtworkProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String = "image/jpeg"

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val ctx = context ?: throw FileNotFoundException(uri.toString())
        val cover = uri.lastPathSegment?.takeIf { it.isNotBlank() } ?: throw FileNotFoundException(uri.toString())
        val dir = File(ctx.cacheDir, "auto-artwork").apply { mkdirs() }
        val file = File(dir, cover.replace(Regex("[^A-Za-z0-9._-]"), "_") + ".img")

        if (!file.exists() || file.length() == 0L) {
            val container = (ctx.applicationContext as HalftoneApp).container
            val url = container.client.coverArtUrl(cover, COVER_SMALL) ?: throw FileNotFoundException(cover)
            val tmp = File(dir, file.name + ".tmp")
            try {
                container.okHttp.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    val body = response.body
                    if (!response.isSuccessful || body == null) throw FileNotFoundException("HTTP ${response.code}")
                    tmp.outputStream().use { out -> body.byteStream().copyTo(out) }
                }
                if (!tmp.renameTo(file)) throw FileNotFoundException(cover)
            } catch (e: FileNotFoundException) {
                tmp.delete()
                throw e
            } catch (e: Exception) {
                tmp.delete()
                throw FileNotFoundException(e.message ?: cover)
            }
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    companion object {
        fun authority(context: Context) = "${context.packageName}.artwork"

        fun baseUri(context: Context): Uri =
            Uri.Builder().scheme("content").authority(authority(context)).build()

        fun coverUri(context: Context, coverArt: String?): Uri? =
            coverArt?.takeIf { it.isNotBlank() }?.let {
                Uri.Builder().scheme("content").authority(authority(context)).appendPath("cover").appendPath(it).build()
            }
    }
}
