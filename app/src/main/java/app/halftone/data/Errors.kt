package app.halftone.data

import kotlinx.serialization.SerializationException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import app.halftone.R
import app.halftone.Str

/** Vertaalt technische fouten naar iets wat een mens begrijpt. */
fun Throwable.userMessage(): String = when (this) {
    is SubsonicException -> message ?: Str.get(R.string.error_generic)
    is UnknownHostException -> Str.get(R.string.error_unknown_host)
    is ConnectException -> Str.get(R.string.error_connect)
    is SocketTimeoutException -> Str.get(R.string.error_timeout)
    is SSLException -> Str.get(R.string.error_ssl)
    is SerializationException -> Str.get(R.string.error_not_subsonic)
    is IllegalArgumentException -> Str.get(R.string.error_bad_address)
    else -> message ?: Str.get(R.string.error_generic)
}
