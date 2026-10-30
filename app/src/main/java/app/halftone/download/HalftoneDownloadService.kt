package app.halftone.download

import android.app.Notification
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
import app.halftone.HalftoneApp
import app.halftone.R

private const val CHANNEL_ID = "downloads"
private const val NOTIFICATION_ID = 7

/** Houdt downloads gaande als de app op de achtergrond staat, met een voortgangsmelding. */
@OptIn(UnstableApi::class)
class HalftoneDownloadService : DownloadService(
    NOTIFICATION_ID,
    DownloadService.DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    CHANNEL_ID,
    R.string.download_channel_name,
    R.string.download_channel_description,
) {
    private val notifications by lazy { DownloadNotificationHelper(this, CHANNEL_ID) }

    override fun getDownloadManager(): DownloadManager =
        (application as HalftoneApp).container.downloads.manager

    override fun getScheduler(): Scheduler? = null

    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int,
    ): Notification {
        val active = downloads.count { it.state == Download.STATE_DOWNLOADING || it.state == Download.STATE_QUEUED }
        val message = resources.getQuantityString(R.plurals.downloading_songs, active, active)
        return notifications.buildProgressNotification(
            this,
            R.drawable.ic_stat_download,
            null,
            message,
            downloads,
            notMetRequirements,
        )
    }
}
