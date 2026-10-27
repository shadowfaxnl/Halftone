package app.halftone.desktop.i18n

import java.util.Locale

/**
 * Alle teksten van de desktop-app. Engels is de standaard; Nederlands wordt gekozen
 * als de systeemtaal Nederlands is, of als dat in de instellingen is gekozen.
 */
interface Strings {
    // Algemeen
    val unknownArtist: String
    val unknownAlbum: String
    val unknownSong: String
    val untitledPlaylist: String
    val noSongsFound: String
    val play: String
    val pause: String
    val shuffle: String
    val next: String
    val previous: String
    val repeat: String
    val close: String
    val back: String
    val more: String
    val cancel: String
    val remove: String
    val save: String
    val create: String
    val retry: String
    val clear: String
    val albums: String
    val artists: String
    val songs: String
    val playlists: String
    val favorites: String
    val downloads: String
    val queue: String
    val settings: String
    val home: String
    val search: String
    val library: String
    val volume: String
    val mute: String
    val kindAlbum: String
    val kindArtist: String
    val kindPlaylist: String
    fun disc(n: Int): String
    fun durationHoursMinutes(h: Int, m: Int): String
    fun durationMinutes(m: Int): String
    fun songsCount(n: Int): String
    fun albumsCount(n: Int): String
    fun songsOffline(n: Int): String

    // Fouten
    val errorGeneric: String
    val errorUnknownHost: String
    val errorConnect: String
    val errorTimeout: String
    val errorSsl: String
    val errorNotSubsonic: String
    val errorBadAddress: String
    val errorNotLoggedIn: String
    fun errorHttp(code: Int): String
    val errorEmptyResponse: String
    val errorWrongCredentials: String
    val errorNoTokenAuth: String
    val errorNoAccess: String
    val errorNotFound: String
    val errorServerUnknown: String
    val errorArtistNotFound: String
    val errorAlbumNotFound: String
    val errorPlaylistNotFound: String

    // Menu bij een nummer
    val playNext: String
    val playingNext: String
    val addToQueue: String
    val addedToQueue: String
    val addToPlaylist: String
    val addToFavorites: String
    val removeFromFavorites: String
    val favorite: String
    val download: String
    val downloadRest: String
    val downloaded: String
    val removeDownload: String
    val goToAlbum: String
    val goToArtist: String
    val addAlbumToQueue: String
    val albumAddedToQueue: String
    val removeDownloadsTitle: String
    val removeDownloadsText: String

    // Inloggen
    val tagline: String
    val serverAddress: String
    val serverPlaceholder: String
    val username: String
    val password: String
    val showPassword: String
    val hidePassword: String
    val connect: String
    val loginFillAll: String
    val worksWith: String

    // Home
    val goodMorning: String
    val goodAfternoon: String
    val goodEvening: String
    val goodNight: String
    val homeSubtitle: String
    val shuffleMix: String
    val surpriseMe: String
    val featured: String
    val yourPlaylists: String
    val recentlyPlayed: String
    val recentlyAdded: String
    val mostPlayed: String
    val discover: String
    val listenDownloads: String

    // Bibliotheek
    val sortAz: String
    val sortArtist: String
    val sortYear: String
    val favoritesEmptyTitle: String
    val favoritesEmptyText: String
    val downloadsEmptyTitle: String
    val downloadsEmptyText: String
    fun downloadsInProgress(n: Int): String

    // Zoeken
    val searchHint: String
    val searchIdle: String
    fun searchNone(query: String): String

    // Speler
    val playingFrom: String
    val nowPlaying: String
    val queueEmpty: String
    val upNext: String
    val openPlayer: String

    // Afspeellijsten
    val newPlaylist: String
    val startCollection: String
    val playlistNameHint: String
    fun addedToPlaylist(what: String, playlist: String): String
    fun playlistCreated(name: String): String
    fun byOwner(owner: String): String
    val rename: String
    val deletePlaylist: String
    fun deletePlaylistTitle(name: String): String
    val deletePlaylistText: String
    val playlistEmptyTitle: String
    val playlistEmptyText: String
    val removeFromPlaylist: String

    // Instellingen
    val account: String
    val signOut: String
    val signOutKeepsDownloads: String
    val signOutTitle: String
    val signOutText: String
    val playback: String
    val streamingQuality: String
    val qualityOriginal: String
    val qualityOriginalText: String
    fun qualityLimitedText(kbps: Int): String
    val scrobbling: String
    val scrobblingText: String
    val removeAllDownloads: String
    val removeAllDownloadsTitle: String
    fun removeAllDownloadsText(size: String): String
    val language: String
    val languageSystem: String
    val languageRestart: String
    fun versionLine(version: String): String
    val shortcuts: String
    val shortcutsText: String

    // libmpv ontbreekt
    val mpvMissingTitle: String
    val mpvMissingText: String
}

object En : Strings {
    override val unknownArtist = "Unknown artist"
    override val unknownAlbum = "Unknown album"
    override val unknownSong = "Unknown song"
    override val untitledPlaylist = "Untitled playlist"
    override val noSongsFound = "No songs found"
    override val play = "Play"
    override val pause = "Pause"
    override val shuffle = "Shuffle"
    override val next = "Next"
    override val previous = "Previous"
    override val repeat = "Repeat"
    override val close = "Close"
    override val back = "Back"
    override val more = "More"
    override val cancel = "Cancel"
    override val remove = "Remove"
    override val save = "Save"
    override val create = "Create"
    override val retry = "Try again"
    override val clear = "Clear"
    override val albums = "Albums"
    override val artists = "Artists"
    override val songs = "Songs"
    override val playlists = "Playlists"
    override val favorites = "Favourites"
    override val downloads = "Downloads"
    override val queue = "Queue"
    override val settings = "Settings"
    override val home = "Home"
    override val search = "Search"
    override val library = "Library"
    override val volume = "Volume"
    override val mute = "Mute"
    override val kindAlbum = "Album"
    override val kindArtist = "Artist"
    override val kindPlaylist = "Playlist"
    override fun disc(n: Int) = "Disc $n"
    override fun durationHoursMinutes(h: Int, m: Int) = "$h h $m min"
    override fun durationMinutes(m: Int) = "$m min"
    override fun songsCount(n: Int) = if (n == 1) "1 song" else "$n songs"
    override fun albumsCount(n: Int) = if (n == 1) "1 album" else "$n albums"
    override fun songsOffline(n: Int) = if (n == 1) "1 song offline" else "$n songs offline"

    override val errorGeneric = "Something went wrong"
    override val errorUnknownHost = "Server not found. Is the address right?"
    override val errorConnect = "Can't connect to the server"
    override val errorTimeout = "The server isn't responding in time"
    override val errorSsl = "Secure connection failed. Try http:// if the server is on your home network"
    override val errorNotSubsonic = "This doesn't look like a Subsonic or Navidrome server"
    override val errorBadAddress = "That server address isn't valid"
    override val errorNotLoggedIn = "You're not signed in"
    override fun errorHttp(code: Int) = "The server responded with HTTP $code"
    override val errorEmptyResponse = "Empty response from the server"
    override val errorWrongCredentials = "Wrong username or password"
    override val errorNoTokenAuth = "This server doesn't support token sign-in"
    override val errorNoAccess = "This user doesn't have access"
    override val errorNotFound = "Not found on the server"
    override val errorServerUnknown = "Unknown server error"
    override val errorArtistNotFound = "Artist not found"
    override val errorAlbumNotFound = "Album not found"
    override val errorPlaylistNotFound = "Playlist not found"

    override val playNext = "Play next"
    override val playingNext = "Playing next"
    override val addToQueue = "Add to queue"
    override val addedToQueue = "Added to queue"
    override val addToPlaylist = "Add to playlist"
    override val addToFavorites = "Add to favourites"
    override val removeFromFavorites = "Remove from favourites"
    override val favorite = "Favourite"
    override val download = "Download"
    override val downloadRest = "Download the rest"
    override val downloaded = "Downloaded"
    override val removeDownload = "Remove download"
    override val goToAlbum = "Go to album"
    override val goToArtist = "Go to artist"
    override val addAlbumToQueue = "Add album to queue"
    override val albumAddedToQueue = "Album added to queue"
    override val removeDownloadsTitle = "Remove downloads?"
    override val removeDownloadsText = "The songs stay on your server, but you can no longer play them offline."

    override val tagline = "Your own music, in every shade."
    override val serverAddress = "Server address"
    override val serverPlaceholder = "music.yourdomain.com"
    override val username = "Username"
    override val password = "Password"
    override val showPassword = "Show password"
    override val hidePassword = "Hide password"
    override val connect = "Connect"
    override val loginFillAll = "Fill in all three fields"
    override val worksWith = "Works with Navidrome, Airsonic, Gonic and any other Subsonic server."

    override val goodMorning = "Good morning"
    override val goodAfternoon = "Good afternoon"
    override val goodEvening = "Good evening"
    override val goodNight = "Good night"
    override val homeSubtitle = "What are we listening to?"
    override val shuffleMix = "Shuffle mix"
    override val surpriseMe = "Surprise me"
    override val featured = "FEATURED"
    override val yourPlaylists = "Your playlists"
    override val recentlyPlayed = "Recently played"
    override val recentlyAdded = "Recently added"
    override val mostPlayed = "Most played"
    override val discover = "Discover"
    override val listenDownloads = "Listen to your downloads"

    override val sortAz = "A–Z"
    override val sortArtist = "Artist"
    override val sortYear = "Year"
    override val favoritesEmptyTitle = "No favourites yet"
    override val favoritesEmptyText = "Click the heart on a song, album or artist to keep it here."
    override val downloadsEmptyTitle = "Nothing offline yet"
    override val downloadsEmptyText = "Click the download icon on an album or playlist, or choose Download in a song's menu."
    override fun downloadsInProgress(n: Int) = "$n still downloading"

    override val searchHint = "Artists, albums, songs"
    override val searchIdle = "Search your whole library"
    override fun searchNone(query: String) = "Nothing found for “$query”"

    override val playingFrom = "PLAYING FROM"
    override val nowPlaying = "Now playing"
    override val queueEmpty = "The queue is empty. Play an album, playlist or song to fill it."
    override val upNext = "Up next"
    override val openPlayer = "Open player"

    override val newPlaylist = "New playlist"
    override val startCollection = "Start a collection"
    override val playlistNameHint = "Playlist name"
    override fun addedToPlaylist(what: String, playlist: String) = "$what added to $playlist"
    override fun playlistCreated(name: String) = "Playlist “$name” created"
    override fun byOwner(owner: String) = "by $owner"
    override val rename = "Rename"
    override val deletePlaylist = "Delete playlist"
    override fun deletePlaylistTitle(name: String) = "Delete “$name”?"
    override val deletePlaylistText = "The playlist is removed from your server. The songs themselves stay."
    override val playlistEmptyTitle = "Still empty"
    override val playlistEmptyText = "Add songs from the menu on any song or album."
    override val removeFromPlaylist = "Remove from this playlist"

    override val account = "Account"
    override val signOut = "Sign out"
    override val signOutKeepsDownloads = "Your downloads are kept"
    override val signOutTitle = "Sign out?"
    override val signOutText = "You can then sign in to the same server or a different one."
    override val playback = "Playback"
    override val streamingQuality = "Streaming quality"
    override val qualityOriginal = "Original"
    override val qualityOriginalText = "The original file. Best sound and seamless transitions."
    override fun qualityLimitedText(kbps: Int) = "Up to $kbps kbps. Lighter on your connection."
    override val scrobbling = "Scrobbling"
    override val scrobblingText = "Report what you play, for Recently played and Most played. Navidrome can pass it on to Last.fm or ListenBrainz."
    override val removeAllDownloads = "Remove all downloads"
    override val removeAllDownloadsTitle = "Remove all downloads?"
    override fun removeAllDownloadsText(size: String) = "This frees up $size. Your music stays on your server."
    override val language = "Language"
    override val languageSystem = "System"
    override val languageRestart = "Restart Halftone to apply the new language."
    override fun versionLine(version: String) = "Version $version · for Navidrome and Subsonic"
    override val shortcuts = "Keyboard shortcuts"
    override val shortcutsText = "Space play/pause · Ctrl+→ / Ctrl+← next / previous · Ctrl+F search · Ctrl+L like · Alt+← back"

    override val mpvMissingTitle = "Halftone needs libmpv to play music"
    override val mpvMissingText = "Install it with your package manager, then restart Halftone:\n\n" +
        "Ubuntu, Debian, Mint:  sudo apt install libmpv2\n" +
        "Fedora:  sudo dnf install mpv-libs\n" +
        "Arch, Manjaro:  sudo pacman -S mpv\n" +
        "openSUSE:  sudo zypper install libmpv2"
}

object Nl : Strings {
    override val unknownArtist = "Onbekende artiest"
    override val unknownAlbum = "Onbekend album"
    override val unknownSong = "Onbekend nummer"
    override val untitledPlaylist = "Naamloze afspeellijst"
    override val noSongsFound = "Geen nummers gevonden"
    override val play = "Afspelen"
    override val pause = "Pauzeren"
    override val shuffle = "Shuffle"
    override val next = "Volgende"
    override val previous = "Vorige"
    override val repeat = "Herhalen"
    override val close = "Sluiten"
    override val back = "Terug"
    override val more = "Meer"
    override val cancel = "Annuleren"
    override val remove = "Verwijderen"
    override val save = "Opslaan"
    override val create = "Maken"
    override val retry = "Opnieuw proberen"
    override val clear = "Wissen"
    override val albums = "Albums"
    override val artists = "Artiesten"
    override val songs = "Nummers"
    override val playlists = "Afspeellijsten"
    override val favorites = "Favorieten"
    override val downloads = "Downloads"
    override val queue = "Wachtrij"
    override val settings = "Instellingen"
    override val home = "Home"
    override val search = "Zoeken"
    override val library = "Bibliotheek"
    override val volume = "Volume"
    override val mute = "Dempen"
    override val kindAlbum = "Album"
    override val kindArtist = "Artiest"
    override val kindPlaylist = "Afspeellijst"
    override fun disc(n: Int) = "CD $n"
    override fun durationHoursMinutes(h: Int, m: Int) = "$h u $m min"
    override fun durationMinutes(m: Int) = "$m min"
    override fun songsCount(n: Int) = if (n == 1) "1 nummer" else "$n nummers"
    override fun albumsCount(n: Int) = if (n == 1) "1 album" else "$n albums"
    override fun songsOffline(n: Int) = if (n == 1) "1 nummer offline" else "$n nummers offline"

    override val errorGeneric = "Er ging iets mis"
    override val errorUnknownHost = "Server niet gevonden. Klopt het adres?"
    override val errorConnect = "Kan geen verbinding maken met de server"
    override val errorTimeout = "De server reageert niet op tijd"
    override val errorSsl = "Beveiligde verbinding mislukt. Probeer http:// als het een lokale server is"
    override val errorNotSubsonic = "Dit lijkt geen Subsonic- of Navidrome-server"
    override val errorBadAddress = "Dat serveradres is ongeldig"
    override val errorNotLoggedIn = "Je bent niet ingelogd"
    override fun errorHttp(code: Int) = "De server antwoordde met HTTP $code"
    override val errorEmptyResponse = "Leeg antwoord van de server"
    override val errorWrongCredentials = "Gebruikersnaam of wachtwoord klopt niet"
    override val errorNoTokenAuth = "Deze server ondersteunt geen token-login"
    override val errorNoAccess = "Deze gebruiker heeft geen toegang"
    override val errorNotFound = "Niet gevonden op de server"
    override val errorServerUnknown = "Onbekende fout van de server"
    override val errorArtistNotFound = "Artiest niet gevonden"
    override val errorAlbumNotFound = "Album niet gevonden"
    override val errorPlaylistNotFound = "Afspeellijst niet gevonden"

    override val playNext = "Speel hierna"
    override val playingNext = "Speelt hierna"
    override val addToQueue = "Aan wachtrij toevoegen"
    override val addedToQueue = "Toegevoegd aan wachtrij"
    override val addToPlaylist = "Aan afspeellijst toevoegen"
    override val addToFavorites = "Aan favorieten toevoegen"
    override val removeFromFavorites = "Uit favorieten"
    override val favorite = "Favoriet"
    override val download = "Downloaden"
    override val downloadRest = "Rest downloaden"
    override val downloaded = "Gedownload"
    override val removeDownload = "Download verwijderen"
    override val goToAlbum = "Ga naar album"
    override val goToArtist = "Ga naar artiest"
    override val addAlbumToQueue = "Album aan wachtrij toevoegen"
    override val albumAddedToQueue = "Album toegevoegd aan wachtrij"
    override val removeDownloadsTitle = "Downloads verwijderen?"
    override val removeDownloadsText = "De nummers blijven op je server staan, maar zijn dan niet meer offline te beluisteren."

    override val tagline = "Jouw eigen muziek, in elke tint."
    override val serverAddress = "Serveradres"
    override val serverPlaceholder = "muziek.jouwdomein.nl"
    override val username = "Gebruikersnaam"
    override val password = "Wachtwoord"
    override val showPassword = "Toon wachtwoord"
    override val hidePassword = "Verberg wachtwoord"
    override val connect = "Verbinden"
    override val loginFillAll = "Vul alle drie de velden in"
    override val worksWith = "Werkt met Navidrome, Airsonic, Gonic en elke andere Subsonic-server."

    override val goodMorning = "Goedemorgen"
    override val goodAfternoon = "Goedemiddag"
    override val goodEvening = "Goedenavond"
    override val goodNight = "Goedenacht"
    override val homeSubtitle = "Wat gaan we luisteren?"
    override val shuffleMix = "Shuffle mix"
    override val surpriseMe = "Verras me"
    override val featured = "UITGELICHT"
    override val yourPlaylists = "Jouw afspeellijsten"
    override val recentlyPlayed = "Recent gespeeld"
    override val recentlyAdded = "Nieuw toegevoegd"
    override val mostPlayed = "Vaak gedraaid"
    override val discover = "Ontdek"
    override val listenDownloads = "Luister je downloads"

    override val sortAz = "A–Z"
    override val sortArtist = "Artiest"
    override val sortYear = "Jaar"
    override val favoritesEmptyTitle = "Nog geen favorieten"
    override val favoritesEmptyText = "Klik op het hartje bij een nummer, album of artiest om het hier te bewaren."
    override val downloadsEmptyTitle = "Nog niets offline"
    override val downloadsEmptyText = "Klik op het downloadicoon bij een album of afspeellijst, of kies Downloaden in het menu van een nummer."
    override fun downloadsInProgress(n: Int) = "nog $n bezig"

    override val searchHint = "Artiesten, albums, nummers"
    override val searchIdle = "Zoek in je hele bibliotheek"
    override fun searchNone(query: String) = "Niets gevonden voor “$query”"

    override val playingFrom = "SPEELT UIT"
    override val nowPlaying = "Speelt nu"
    override val queueEmpty = "De wachtrij is leeg. Speel een album, afspeellijst of nummer af om hem te vullen."
    override val upNext = "Hierna"
    override val openPlayer = "Speler openen"

    override val newPlaylist = "Nieuwe afspeellijst"
    override val startCollection = "Begin een verzameling"
    override val playlistNameHint = "Naam van de afspeellijst"
    override fun addedToPlaylist(what: String, playlist: String) = "$what toegevoegd aan $playlist"
    override fun playlistCreated(name: String) = "Afspeellijst “$name” gemaakt"
    override fun byOwner(owner: String) = "door $owner"
    override val rename = "Naam wijzigen"
    override val deletePlaylist = "Afspeellijst verwijderen"
    override fun deletePlaylistTitle(name: String) = "“$name” verwijderen?"
    override val deletePlaylistText = "De afspeellijst verdwijnt van je server. De nummers zelf blijven staan."
    override val playlistEmptyTitle = "Nog leeg"
    override val playlistEmptyText = "Voeg nummers toe via het menu bij een nummer of album."
    override val removeFromPlaylist = "Uit deze afspeellijst"

    override val account = "Account"
    override val signOut = "Uitloggen"
    override val signOutKeepsDownloads = "Je downloads blijven bewaard"
    override val signOutTitle = "Uitloggen?"
    override val signOutText = "Je kunt daarna inloggen op dezelfde of een andere server."
    override val playback = "Afspelen"
    override val streamingQuality = "Streamkwaliteit"
    override val qualityOriginal = "Origineel"
    override val qualityOriginalText = "Het originele bestand. Beste geluid en naadloze overgangen."
    override fun qualityLimitedText(kbps: Int) = "Maximaal $kbps kbps. Lichter voor je verbinding."
    override val scrobbling = "Scrobbelen"
    override val scrobblingText = "Geef door wat je luistert, voor Recent gespeeld en Vaak gedraaid. Navidrome stuurt het desgewenst door naar Last.fm of ListenBrainz."
    override val removeAllDownloads = "Alle downloads verwijderen"
    override val removeAllDownloadsTitle = "Alle downloads verwijderen?"
    override fun removeAllDownloadsText(size: String) = "Dit maakt $size vrij. Je muziek blijft gewoon op je server staan."
    override val language = "Taal"
    override val languageSystem = "Systeem"
    override val languageRestart = "Start Halftone opnieuw om de nieuwe taal te gebruiken."
    override fun versionLine(version: String) = "Versie $version · voor Navidrome en Subsonic"
    override val shortcuts = "Sneltoetsen"
    override val shortcutsText = "Spatie afspelen/pauzeren · Ctrl+→ / Ctrl+← volgende / vorige · Ctrl+F zoeken · Ctrl+L favoriet · Alt+← terug"

    override val mpvMissingTitle = "Halftone heeft libmpv nodig om muziek af te spelen"
    override val mpvMissingText = "Installeer het met je pakketbeheerder en start Halftone opnieuw:\n\n" +
        "Ubuntu, Debian, Mint:  sudo apt install libmpv2\n" +
        "Fedora:  sudo dnf install mpv-libs\n" +
        "Arch, Manjaro:  sudo pacman -S mpv\n" +
        "openSUSE:  sudo zypper install libmpv2"
}

/** De actieve taal. Wordt bij het opstarten gezet op basis van de instelling of het systeem. */
var S: Strings = En
    private set

fun selectLanguage(preference: String) {
    S = when (preference) {
        "nl" -> Nl
        "en" -> En
        else -> if (Locale.getDefault().language == "nl") Nl else En
    }
}
