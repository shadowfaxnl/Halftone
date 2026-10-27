package app.halftone.desktop.data

import kotlinx.serialization.SerializationException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import app.halftone.desktop.i18n.S

/** Vertaalt technische fouten naar iets wat een mens begrijpt. */
fun Throwable.userMessage(): String = when (this) {
    is SubsonicException -> message ?: S.errorGeneric
    is UnknownHostException -> S.errorUnknownHost
    is ConnectException -> S.errorConnect
    is SocketTimeoutException -> S.errorTimeout
    is SSLException -> S.errorSsl
    is SerializationException -> S.errorNotSubsonic
    is IllegalArgumentException -> S.errorBadAddress
    else -> message ?: S.errorGeneric
}
