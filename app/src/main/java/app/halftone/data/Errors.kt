package app.halftone.data

import kotlinx.serialization.SerializationException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/** Vertaalt technische fouten naar iets wat een mens begrijpt. */
fun Throwable.userMessage(): String = when (this) {
    is SubsonicException -> message ?: "Er ging iets mis"
    is UnknownHostException -> "Server niet gevonden. Klopt het adres?"
    is ConnectException -> "Kan geen verbinding maken met de server"
    is SocketTimeoutException -> "De server reageert niet op tijd"
    is SSLException -> "Beveiligde verbinding mislukt. Probeer http:// als het een lokale server is"
    is SerializationException -> "Dit lijkt geen Subsonic- of Navidrome-server"
    is IllegalArgumentException -> "Dat serveradres is ongeldig"
    else -> message ?: "Er ging iets mis"
}
