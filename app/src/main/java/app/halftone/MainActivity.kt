package app.halftone

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.compose.runtime.CompositionLocalProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadService
import app.halftone.download.HalftoneDownloadService
import app.halftone.ui.HalftoneRoot
import app.halftone.ui.common.LocalAppContainer

class MainActivity : ComponentActivity() {
    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        // Onafgemaakte downloads van een vorige keer weer oppakken.
        runCatching { DownloadService.start(this, HalftoneDownloadService::class.java) }

        val container = (application as HalftoneApp).container
        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                HalftoneRoot()
            }
        }
    }
}
