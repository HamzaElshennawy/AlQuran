package com.hifnawy.alquran.shared.domain

import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.exoplayer.offline.DefaultDownloadIndex
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.offline.DownloaderFactory
import androidx.media3.exoplayer.offline.ProgressiveDownloader
import com.hifnawy.alquran.shared.R
import com.hifnawy.alquran.shared.domain.QuranCacheDataSource.CacheKey.Companion.asCacheKey
import com.hifnawy.alquran.shared.domain.QuranCacheDataSource.cacheDataSourceFactory
import com.hifnawy.alquran.shared.domain.QuranCacheDataSource.moveContentsTo
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.DownloadRequestId
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.DownloadState
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.UPDATE_PROGRESS_INTERVAL
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.asDownloadState
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.completedDownloads
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.downloadManagerInstance
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.downloadMonitorJob
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.downloadPath
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.ioCoroutineScope
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.serializedData
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.updateDownloadState
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.debug
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.error
import com.hifnawy.alquran.shared.utils.LongEx.asHumanReadableSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@UnstableApi
class QuranDownloadService : DownloadService(
        FOREGROUND_NOTIFICATION_ID,
        UPDATE_PROGRESS_INTERVAL,
        DOWNLOAD_NOTIFICATION_CHANNEL_ID,
        R.string.download_channel_name,
        R.string.download_channel_description
), DownloadManager.Listener {

    private companion object {

        private const val FOREGROUND_NOTIFICATION_ID = 1
        private const val DOWNLOAD_NOTIFICATION_CHANNEL_ID = "Quran Downloads"
    }

    private lateinit var notificationHelper: DownloadNotificationHelper

    override fun onCreate() {
        notificationHelper = DownloadNotificationHelper(this@QuranDownloadService, DOWNLOAD_NOTIFICATION_CHANNEL_ID)

        downloadManager.addListener(this@QuranDownloadService)
        startDownloadMonitor()

        super.onCreate()
    }

    override fun onDestroy() {
        downloadManager.removeListener(this@QuranDownloadService)
        downloadMonitorJob?.cancel()

        super.onDestroy()
    }

    override fun getDownloadManager() = downloadManagerInstance ?: run {
        val downloadIndex = DefaultDownloadIndex(StandaloneDatabaseProvider(this@QuranDownloadService))

        val downloaderFactory = DownloaderFactory { request ->
            val mediaItem = MediaItem.Builder().run {
                setUri(request.uri)
                setCustomCacheKey(request.customCacheKey)
                build()
            }
            val cacheDataSourceFactory = request.id.asCacheKey.cacheDataSourceFactory
            val executor = Runnable::run
            val position = request.byteRange?.offset ?: 0L
            val length = request.byteRange?.length ?: -1L

            ProgressiveDownloader(mediaItem, cacheDataSourceFactory, executor, position, length)
        }

        DownloadManager(this@QuranDownloadService, downloadIndex, downloaderFactory).apply {
            maxParallelDownloads = 1
            minRetryCount = 3

            downloadManagerInstance = this@apply
        }
    }

    override fun getScheduler() = null

    override fun getForegroundNotification(downloads: MutableList<Download>, notMetRequirements: Int) = notificationHelper.run {
        val isActive = downloads.firstOrNull { it.state == Download.STATE_DOWNLOADING }
        val isQueued = downloads.any { it.state == Download.STATE_QUEUED }

        val message = when {
            isActive != null -> {
                val data = isActive.request.serializedData
                val progress = isActive.percentDownloaded

                getString(R.string.downloading_surah_detailed, data.surah.id, data.surah.name, data.reciter.name, data.moshaf.name, progress)
            }

            isQueued         -> {
                val queuedCount = downloads.count { it.state == Download.STATE_QUEUED }
                val nextDownload = downloads.firstOrNull { it.state == Download.STATE_QUEUED }

                when (nextDownload) {
                    null -> getString(R.string.preparing_downloads_count, queuedCount)

                    else -> {
                        val data = nextDownload.request.serializedData
                        getString(R.string.preparing_surah, data.surah.id, data.surah.name, queuedCount)
                    }
                }
            }

            else             -> getString(R.string.preparing_downloads)
        }

        buildProgressNotification(
                this@QuranDownloadService,
                R.drawable.quran_icon_monochrome_white_64,
                null,
                message,
                downloads,
                notMetRequirements
        )
    }

    private fun startDownloadMonitor() = downloadMonitorJob.let { job ->
        job?.cancel()

        ioCoroutineScope.launch {
            do {
                downloadManager.currentDownloads.filter { it.state == DownloadState.State.DOWNLOADING.code }.run {
                    withContext(Dispatchers.Main) { forEach { updateDownloadState(it) } }
                }
            } while (true)
        }.also { downloadMonitorJob = it }
    }

    override fun onDownloadChanged(downloadManager: DownloadManager, download: Download, finalException: Exception?) = ioCoroutineScope.launch {
        when (finalException) {
            null if (download.state == Download.STATE_COMPLETED) -> download.request.serializedData.let { serializedData ->
                val downloadRequestId = DownloadRequestId(serializedData.reciter, serializedData.moshaf)

                moveCompletedDownload(download)

                completedDownloads[downloadRequestId]?.run {
                    add(size, download)
                } ?: run {
                    completedDownloads[downloadRequestId] = mutableListOf(download)
                }

                withContext(Dispatchers.Main) { updateDownloadState(download) }.run { Timber.debug("Download '${download.request.id}' is ${download.state.asDownloadState}!") }
            }

            null                                                 -> Unit
            else                                                 -> Timber.error("Download '${download.request.id}' is ${download.state.asDownloadState}: ${finalException.message}!")
        }
    }.run { Timber.debug("Download '${download.request.id}' is ${download.state.asDownloadState}!") }

    override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) = download.run {
        val serializedData = request.serializedData
        val downloadRequestId = DownloadRequestId(serializedData.reciter, serializedData.moshaf)

        completedDownloads[downloadRequestId]?.remove(this@run)

        Timber.debug("Download '${request.id}' removed!")
    }

    private suspend fun moveCompletedDownload(download: Download) = withContext(Dispatchers.IO) {
        download.run {
            val cacheKey = request.id.asCacheKey
            val bytesWritten = cacheKey.moveContentsTo(request.downloadPath)

            when {
                bytesWritten != contentLength -> Timber.error("Failed to move download '${request.id}' to '${request.downloadPath}'!")
                else                          -> Timber.debug("Download '${request.id}' (${bytesWritten.asHumanReadableSize}) moved to '${request.downloadPath}'!")
            }

            downloadManager.removeDownload(request.id)
        }
    }
}
