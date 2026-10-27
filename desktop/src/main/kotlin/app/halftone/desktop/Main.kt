package app.halftone.desktop

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.halftone.desktop.data.Paths
import app.halftone.desktop.data.StarKind
import app.halftone.desktop.i18n.selectLanguage
import app.halftone.desktop.ui.Shell
import app.halftone.desktop.ui.common.LocalApp
import app.halftone.desktop.ui.common.observe
import app.halftone.desktop.ui.screens.LoginScreen
import app.halftone.desktop.ui.theme.HalftoneTheme
import app.halftone.desktop.ui.theme.Ink
import app.halftone.desktop.ui.theme.rememberCoverColors
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import okio.Path.Companion.toOkioPath
import java.io.File
import javax.imageio.ImageIO
import kotlin.system.exitProcess

fun main() {
    // Taal kiezen voordat er iets getoond wordt.
    val app = AppState()
    selectLanguage(app.settings.state.value.language)

    application {
        val start = app.settings.state.value
        val windowState = rememberWindowState(size = DpSize(start.windowWidth.dp, start.windowHeight.dp))
        val icon = remember {
            Thread.currentThread().contextClassLoader.getResourceAsStream("halftone.png")?.use {
                BitmapPainter(ImageIO.read(it).toComposeImageBitmap())
            }
        }

        fun quit() {
            app.settings.update {
                it.copy(windowWidth = windowState.size.width.value.toInt(), windowHeight = windowState.size.height.value.toInt())
            }
            app.shutdown()
            exitApplication()
            exitProcess(0)
        }

        Window(
            onCloseRequest = ::quit,
            state = windowState,
            title = "Halftone",
            icon = icon,
            onPreviewKeyEvent = handler@{ e ->
                if (e.type != KeyEventType.KeyDown) return@handler false
                when {
                    e.key == Key.Spacebar && !app.typing && app.loggedIn -> { app.player.togglePlay(); true }
                    e.isCtrlPressed && e.key == Key.DirectionRight -> { app.player.next(); true }
                    e.isCtrlPressed && e.key == Key.DirectionLeft -> { app.player.previous(); true }
                    e.isCtrlPressed && e.key == Key.F && app.loggedIn -> { app.nowPlayingOpen = false; app.nav.root(Screen.Search); true }
                    e.isCtrlPressed && e.key == Key.L -> {
                        app.player.state.value.current?.let { app.favorites.toggle(it.id, StarKind.Song) }
                        true
                    }
                    e.isAltPressed && e.key == Key.DirectionLeft -> { app.nav.back(); true }
                    e.key == Key.Escape && app.nowPlayingOpen -> { app.nowPlayingOpen = false; true }
                    else -> false
                }
            },
        ) {
            window.minimumSize = java.awt.Dimension(980, 640)
            app.raiseWindow = {
                java.awt.EventQueue.invokeLater {
                    window.isVisible = true
                    window.toFront()
                    window.requestFocus()
                }
            }
            app.quitApp = { java.awt.EventQueue.invokeLater { quit() } }

            setSingletonImageLoaderFactory { context ->
                ImageLoader.Builder(context)
                    .components { add(OkHttpNetworkFetcherFactory(callFactory = { app.okHttp })) }
                    .diskCache {
                        DiskCache.Builder()
                            .directory(File(Paths.cache, "images").toOkioPath())
                            .maxSizeBytes(512L * 1024 * 1024)
                            .build()
                    }
                    .crossfade(true)
                    .build()
            }

            CompositionLocalProvider(LocalApp provides app) {
                val ps = app.player.state.observe()
                val colors = rememberCoverColors(app.okHttp, app.client.coverArtUrl(ps.current?.coverArt, 64))
                HalftoneTheme(accent = colors.accent) {
                    Box(Modifier.fillMaxSize().background(Ink)) {
                        Crossfade(app.loggedIn, animationSpec = tween(350), label = "session") { loggedIn ->
                            if (loggedIn) Shell(colors) else LoginScreen()
                        }
                    }
                }
            }
        }
    }
}
