# Halftone

*[English](README.md)*

Een Android-muziekspeler voor je eigen Subsonic-server (Navidrome, Airsonic, Gonic, …), gebouwd met Kotlin, Jetpack Compose en Media3.

De hele app kleurt mee met het hoesje dat speelt: accentkleuren, achtergrondverlopen en de speler halen hun tinten uit de albumhoes en vloeien zacht over als het nummer wisselt.

## Wat zit erin

**Bladeren en afspelen**
- Inloggen op elke Subsonic-server met token-authenticatie
- Home met begroeting, *Shuffle mix*, *Verras me*, een uitgelicht album, je afspeellijsten en rijen met recent gespeeld, nieuw, vaak gedraaid en ontdekken
- Bibliotheek met tabbladen: Afspeellijsten, Favorieten, Artiesten, Albums en Downloads
- Artiest-, album- en afspeellijstpagina's met grote hoesjes en kleurgloed
- Zoeken terwijl je typt
- Afspelen op de achtergrond met melding, vergrendelscherm en Bluetooth-knoppen
- Miniplayer en een volledig spelerscherm met vegen voor volgende/vorige en een wachtrij

**Nieuw in 0.4: Engels en Nederlands**
- De hele interface staat in Android-taalbestanden: Engels is de standaard, Nederlands is ingebouwd (`res/values/strings.xml` en `res/values-nl/strings.xml`).
- De app volgt de taal van je telefoon. Op Android 13+ kies je via Instellingen › Taal een taal alleen voor Halftone.
- Een taal toevoegen? Kopieer `values/strings.xml` naar bijvoorbeeld `values-de/`, vertaal de teksten en voeg de taal toe aan `res/xml/locales_config.xml`.

**Sinds 0.3: de huisstijl**
- Nieuwe naam: **Halftone**, met package `app.halftone`.
- App-icoon met het rasterlogo, plus een éénkleurige versie voor de themed icons van Android 13+ en een eigen icoon in de mediamelding.
- Merkkleuren koraal, roze en violet op donkere inkt. Zolang er niets speelt is koraal het accent, daarna nemen de hoesjes het over.
- Lettertypes Bricolage Grotesque (koppen) en Manrope (tekst).
- Inlogscherm met het logo en een langzaam ademend rasterveld; het logo staat ook op Home en in Instellingen.

**Sinds 0.2**
- **Afspeellijsten**: maken, hernoemen, verwijderen; nummers of hele albums toevoegen via het menu, nummers eruit halen. Wijzigingen staan meteen op je server, dus ook in de webinterface van Navidrome.
- **Favorieten**: hartje bij nummers (ook in de speler), albums en artiesten. Eigen tabblad met alles bij elkaar; kleine hartjes in elke nummerlijst.
- **Scrobbelen**: Halftone meldt "speelt nu" en telt een nummer als beluisterd na de helft of na vier minuten. Daardoor kloppen *Recent gespeeld* en *Vaak gedraaid*, en Navidrome kan het doorsturen naar Last.fm of ListenBrainz. Offline luisteren wordt later alsnog doorgegeven. Uit te zetten in Instellingen.
- **Offline downloaden**: per nummer, album of afspeellijst. Downloads zijn het originele bestand, met voortgang in de lijst en in een melding. Gedownloade nummers spelen altijd lokaal, ook als je wél verbinding hebt. Ook de hoesjes worden bewaard. Kun je de server niet bereiken, dan biedt het startscherm *Luister je downloads* aan.
- **Gapless afspelen**: het volgende nummer staat al klaar voordat het huidige afloopt, en ExoPlayer haalt de stiltes van de encoder weg. Live-albums en klassieke werken lopen naadloos door.
- **Instellingen**: streamkwaliteit (origineel, 320, 192 of 128 kbps), scrobbelen aan/uit, opslagruimte van downloads en alles in één keer wissen.

## Bouwen

Je hebt nodig: **Android Studio** (Narwhal 2025.1 of nieuwer) en een telefoon of emulator met Android 8.0+.

1. Pak de zip uit en kies in Android Studio **File → Open** en dan de map `Halftone`.
2. Laat Gradle synchroniseren.
3. Druk op **Run ▶**.

Of vanaf de command line: `./gradlew assembleDebug`. De APK staat dan in `app/build/outputs/apk/debug/`.

> De code is geschreven en grondig nagelopen, maar kon niet gecompileerd worden in de omgeving waarin hij is gemaakt (geen toegang tot de Android-buildservers). Geeft de eerste build een fout, plak de melding dan terug in het gesprek.

## Lettertypes

De lettertypes van de huisstijl (Bricolage Grotesque en Manrope, beide onder de SIL Open Font License) haalt Gradle bij de eerste build op van de Google Fonts-repository op GitHub en zet ze in `app/src/main/assets/fonts/`. Lukt dat niet, bijvoorbeeld zonder internet, dan bouwt de app gewoon en gebruikt hij het systeemlettertype. Je kunt de bestanden ook zelf neerzetten als `manrope.ttf` en `bricolage_grotesque.ttf`.

## Linux

In `desktop/` staat een Linux-versie, gebouwd met Compose Multiplatform. Hij gebruikt dezelfde Kotlin-datalaag als de Android-app en kan hetzelfde: start, zoeken, bibliotheek, afspeellijsten, favorieten, scrobbelen, downloads, naadloos afspelen, kleuren uit hoesjes, Engels en Nederlands. De indeling past bij een bureaublad, met een zijbalk, een spelerbalk onderin en een wachtrijpaneel.

**Installeren.** Installeer eerst **libmpv**, dat Halftone gebruikt om af te spelen:

- Debian, Ubuntu, Mint: `sudo apt install libmpv2` (oudere versies: `libmpv1`)
- Fedora: `sudo dnf install mpv-libs` (via RPM Fusion)
- Arch, Manjaro: `sudo pacman -S mpv`
- openSUSE: `sudo zypper install libmpv2`

Download daarna `Halftone-x.y.z-x86_64.AppImage` bij [Releases](https://github.com/shadowfaxnl/Halftone/releases), maak hem uitvoerbaar met `chmod +x` en start hem. Java hoef je niet te installeren, dat zit in de AppImage.

**Integratie.** Mediatoetsen, de mediabediening van GNOME en KDE en `playerctl` werken via MPRIS. Sneltoetsen: spatie speelt of pauzeert, Ctrl+→ en Ctrl+← slaan over, Ctrl+F zoekt, Ctrl+L markeert het huidige nummer als favoriet, Alt+← gaat terug en Esc sluit de speler. Instellingen staan in `~/.config/halftone/settings.json`, downloads in `~/.local/share/halftone/downloads` en hoesjes in `~/.cache/halftone`.

**Zelf bouwen** (JDK 17 of nieuwer):

```bash
cd desktop
./gradlew run                     # app starten
packaging/build-appimage.sh       # AppImage in build/appimage
```

De workflow in `.github/workflows/linux-appimage.yml` bouwt de AppImage bij elke push naar `desktop/` en hangt hem aan de release als je een tag als `v0.4.0` pusht.

## Serveradres

Typ het adres zoals je Navidrome in de browser opent, bijvoorbeeld `muziek.jouwdomein.nl` of `192.168.1.20:4533`. Zonder `http://` of `https://` kiest Halftone zelf: http voor lokale adressen en adressen met een poort, anders https.

## Goed om te weten

- **Gapless werkt het best met streamkwaliteit "Origineel"** (en met downloads, die altijd origineel zijn). Bij een lagere bitrate zet de server het bestand om, en niet elke omzetting bewaart de informatie die nodig is voor naadloze overgangen.
- Downloads staan in de privé-opslag van de app en worden nooit automatisch opgeruimd. Uitloggen laat ze staan; verwijderen kan per album/afspeellijst of via Instellingen.
- Afspeellijsten van anderen kun je afspelen, maar alleen je eigen lijsten aanpassen (dat bepaalt de server).
- Op Android 13+ vraagt Halftone bij de eerste download of hij meldingen mag tonen, voor de voortgang. Weigeren mag; downloaden werkt dan nog steeds.
- Inloggegevens staan lokaal in DataStore. `usesCleartextTraffic` staat aan zodat lokale servers via http werken.
- De release-build is voor het gemak ondertekend met de debug-sleutel. Vervang dat in `app/build.gradle.kts` voordat je de app deelt.

## Opbouw

```
app/src/main/java/app/halftone/
├── HalftoneApp.kt, MainActivity.kt
├── AppContainer.kt        alle onderdelen op één plek
├── data/                  Subsonic-client, modellen, repository, favorieten, voorkeuren
├── download/              Media3 DownloadManager + DownloadService
├── playback/              PlaybackService (cache, gapless-buffer), Scrobbler, PlayerConnection
└── ui/
    ├── theme/             donker thema, kleuren uit hoesjes
    ├── common/            hoesjes, kaarten, rijen, kop voor album/afspeellijst, knoppen
    ├── home/ library/ artist/ album/ search/
    ├── playlist/          afspeellijstpagina, overzicht, "toevoegen aan"-onderblad
    ├── favorites/ downloads/ settings/
    └── player/            miniplayer, spelerscherm, wachtrij
```

## Ideeën voor later

Android Auto, een widget op het startscherm, songteksten, ReplayGain, de wachtrij bewaren na het sluiten van de app, en een Flatpak voor de Linux-versie.

## Licentie

Halftone is vrije software onder de [GNU General Public License v3.0](LICENSE). Je mag de code gebruiken, aanpassen en verspreiden, zolang verspreide versies (ook aangepaste) ook onder de GPL-3.0 vallen en met hun broncode worden gedeeld.

Copyright © 2026 [shadowfaxnl](https://github.com/shadowfaxnl) en bijdragers. De lettertypes Bricolage Grotesque en Manrope vallen onder de SIL Open Font License 1.1.
