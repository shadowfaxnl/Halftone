<p align="center">
  <img src="docs/logo.svg" width="120" height="120" alt="Halftone logo">
</p>

<h1 align="center">Halftone</h1>

<p align="center">
  <strong>Your own music, in every shade.</strong><br>
  A beautiful music player for Android and Linux, for Navidrome and any Subsonic server.
</p>

<p align="center">
  <a href="https://gethalftone.eu">Website</a> ·
  <a href="#building">Build</a> ·
  <a href="#linux-desktop">Linux</a> ·
  <a href="https://github.com/shadowfaxnl/Halftone/issues/new?labels=enhancement">Suggest a feature</a> ·
  <a href="https://github.com/shadowfaxnl/Halftone/issues/new?labels=bug">Report a bug</a> ·
  <a href="README.nl.md">Nederlands</a>
</p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-GPL--3.0-E8457A" alt="License: GPL-3.0"></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-FF9A5C" alt="Android 8.0+">
  <img src="https://img.shields.io/badge/Linux-AppImage-FF9A5C" alt="Linux AppImage">
  <img src="https://img.shields.io/badge/Subsonic%20API-1.16.1-7A5CFF" alt="Subsonic API 1.16.1">
</p>

---

Halftone plays the music library on your own server and takes its colours from whatever album is playing. Buttons, gradients and the player pick up the tones of the cover and fade over when the track changes, so every album gets its own app.

It is built with Kotlin, Jetpack Compose and Media3, and talks straight to your server. There is no Halftone account, no analytics and no ads.

## Features

**Browsing and playback**
- Sign in to any Subsonic server with token authentication. Your password never travels in plain text.
- Home with Shuffle mix, Surprise me, a featured album, your playlists and shelves for recently played, recently added, most played and discover. Pull down to refresh.
- Library tabs for playlists, favourites, artists, albums and downloads.
- Artist, album and playlist pages with large covers and a colour glow taken from the artwork.
- Search as you type, across artists, albums and songs.
- Background playback with controls in the notification, on the lock screen and on Bluetooth headsets. Audio focus is handled for calls and navigation prompts.
- Mini player and a full-screen player: swipe the cover for next or previous, swipe down to close. Includes a queue you can jump around in and edit.

**Your server stays in charge**
- **Playlists**: create, rename and delete them, add songs or whole albums, and remove tracks. Changes are saved on your server, so the Navidrome web UI and your other clients stay in sync.
- **Favourites**: heart songs (including the one that's playing), albums and artists. Favourites have their own tab and show up as a small heart in every list.
- **Scrobbling**: Halftone reports "now playing" and counts a play after half the track or four minutes, whichever comes first. This keeps *Recently played* and *Most played* accurate, and Navidrome can forward plays to Last.fm or ListenBrainz. Plays made offline are sent later. You can turn it off in Settings.

**Listening anywhere**
- **Offline downloads** for songs, albums and playlists. Downloads are the original files, with their covers, and always play locally, even when you're online. If the server can't be reached, the home screen offers your downloads.
- **Gapless playback**: the next track is buffered before the current one ends, and encoder padding is trimmed, so live albums and DJ mixes flow on without a gap.
- **Streaming quality**: original, 320, 192 or 128 kbps.

**Look and feel**
- Dynamic colours from every album cover, with checks that keep text readable.
- Dark theme with the Halftone palette (coral, rose and violet), Bricolage Grotesque for headings and Manrope for text.
- Adaptive app icon with a themed (monochrome) version for Android 13+.
- English and Dutch. The app follows your phone's language, and on Android 13+ you can pick a language just for Halftone.

## Requirements

| | |
|---|---|
| Android | 8.0 (API 26) or newer |
| Server | Navidrome, Gonic, Airsonic-Advanced, Ampache or any other Subsonic-compatible server |
| Protocol | Subsonic API 1.16.1 with token authentication |
| Connection | HTTPS, or HTTP on your home network |

## Building

You need [Android Studio](https://developer.android.com/studio) (Narwhal 2025.1 or newer) and a phone or emulator running Android 8.0+.

```bash
git clone https://github.com/shadowfaxnl/Halftone.git
cd Halftone
./gradlew assembleDebug
```

The APK ends up in `app/build/outputs/apk/debug/`. In Android Studio, open the project folder and press **Run**.

**Fonts.** On the first build, Gradle downloads the brand fonts (Bricolage Grotesque and Manrope, both under the SIL Open Font License) from the Google Fonts repository into `app/src/main/assets/fonts/`. If that fails, for example without an internet connection, the build continues and the app uses the system font. You can also place `manrope.ttf` and `bricolage_grotesque.ttf` there yourself.

**Release builds** are signed with the debug key for convenience. Set up your own signing config in `app/build.gradle.kts` before you distribute the app.

## Linux desktop

The `desktop/` folder holds a Linux client built with Compose Multiplatform. It shares the Kotlin data layer with the Android app and has the same features: home, search, library, playlists, favourites, scrobbling, downloads, gapless playback, colours from covers and English/Dutch. The layout is made for a desktop, with a sidebar, a player bar at the bottom and a queue panel.

### Install

1. Install **libmpv**, which Halftone uses for playback:

   | Distribution | Command |
   |---|---|
   | Debian, Ubuntu, Mint | `sudo apt install libmpv2` (older releases: `libmpv1`) |
   | Fedora | `sudo dnf install mpv-libs` (needs RPM Fusion) |
   | Arch, Manjaro | `sudo pacman -S mpv` |
   | openSUSE | `sudo zypper install libmpv2` |

2. Download `Halftone-x.y.z-x86_64.AppImage` from [Releases](https://github.com/shadowfaxnl/Halftone/releases), make it executable and run it:

   ```bash
   chmod +x Halftone-*-x86_64.AppImage
   ./Halftone-*-x86_64.AppImage
   ```

The AppImage contains its own Java runtime, so you don't need Java installed.

### Desktop integration

- **Media keys and panels.** Halftone registers over MPRIS (`org.mpris.MediaPlayer2.halftone`), so media keys, the GNOME and KDE media controls and `playerctl` work.
- **Keyboard shortcuts.** Space plays or pauses, Ctrl+→ and Ctrl+← skip, Ctrl+F searches, Ctrl+L likes the current song, Alt+← goes back and Esc closes the player.
- **Files.** Settings are in `~/.config/halftone/settings.json` (readable by your user only), downloads in `~/.local/share/halftone/downloads` and cached covers in `~/.cache/halftone`.

### Build from source

You need JDK 17 or newer.

```bash
cd desktop
./gradlew run                        # start the app
./gradlew createDistributable        # app folder in build/compose/binaries/main/app
packaging/build-appimage.sh          # AppImage in build/appimage
```

The [Linux AppImage](.github/workflows/linux-appimage.yml) workflow builds the AppImage on every push to `desktop/`, and attaches it to the release when you push a tag such as `v0.4.0`.

## Signing in

Enter the address you use to open Navidrome in your browser, for example `music.example.com` or `192.168.1.20:4533`. Without `http://` or `https://`, Halftone picks HTTP for local addresses and addresses with a port, and HTTPS for everything else. A path such as `/navidrome` is fine.

## Good to know

- **Gapless works best with streaming quality set to Original**, and with downloads, which are always original. At a lower bitrate the server transcodes the file, and not every transcoder keeps the information needed for seamless transitions.
- **Downloads** live in the app's private storage and are never cleaned up automatically. Signing out keeps them. You can remove them per album or playlist, or all at once in Settings.
- **Playlists owned by other users** can be played, but only your own can be edited. Your server enforces this.
- **Notifications.** On Android 13+, Halftone asks for permission to show notifications the first time you download something, to show progress. Downloading still works if you decline.
- **Credentials** are stored on the device only (DataStore). Cleartext traffic is allowed so that servers on your home network work over HTTP. Turn `usesCleartextTraffic` off in `AndroidManifest.xml` if you only use HTTPS.

## Project structure

```
app/src/main/java/app/halftone/
├── HalftoneApp.kt, MainActivity.kt
├── AppContainer.kt        wires everything together
├── Str.kt                 strings outside Compose
├── data/                  Subsonic client, models, repository, favourites, preferences
├── download/              Media3 DownloadManager and DownloadService
├── playback/              PlaybackService (cache, gapless buffer), scrobbler, player connection
└── ui/
    ├── theme/             dark theme, brand fonts, colours from covers
    ├── common/            covers, cards, rows, collection header, logo, buttons
    ├── home/ library/ artist/ album/ search/
    ├── playlist/          playlist page, overview, "add to playlist" sheet
    ├── favorites/ downloads/ settings/
    └── player/            mini player, now playing, queue
```

## Website

The project website, [gethalftone.eu](https://gethalftone.eu), lives in [`docs/`](docs/index.html), together with its icons, social preview image, sitemap and `robots.txt`. To publish it with GitHub Pages, go to **Settings › Pages**, choose **Deploy from a branch**, select the `main` branch with the `/docs` folder, and enter `gethalftone.eu` as the custom domain (the `CNAME` file is already there). The site uses Google Analytics only after a visitor agrees to it.

## Translations

All text lives in Android string resources: English in `app/src/main/res/values/strings.xml`, Dutch in `values-nl/strings.xml`. To add a language, copy the English file to a folder such as `values-de/`, translate it, and add the language to `app/src/main/res/xml/locales_config.xml`. Pull requests are welcome.

## Roadmap

Ideas we're considering:

- Android Auto
- Home screen widget
- Synced lyrics
- ReplayGain
- Remember the queue after the app closes
- Flatpak for the Linux client

Want one of these, or something else? [Open an issue](https://github.com/shadowfaxnl/Halftone/issues/new?labels=enhancement) and tell us how you'd use it.

## License

Halftone is free software: you can redistribute it and/or modify it under the terms of the [GNU General Public License v3.0](LICENSE) as published by the Free Software Foundation. It is distributed in the hope that it will be useful, but without any warranty.

Copyright © 2026 [shadowfaxnl](https://github.com/shadowfaxnl) and contributors. Any version you distribute, including modified ones, must also be released under the GPL-3.0 with its source code.

The brand fonts Bricolage Grotesque and Manrope are licensed separately under the SIL Open Font License 1.1.

## Acknowledgements

Halftone builds on [Media3](https://developer.android.com/media/media3), [Jetpack Compose](https://developer.android.com/compose), [OkHttp](https://square.github.io/okhttp/), [Coil](https://coil-kt.github.io/coil/) and [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization). Navidrome and Subsonic are independent projects; Halftone is not affiliated with them.
