package com.hifnawy.alquran.shared.domain

import android.app.Notification
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
import androidx.media3.exoplayer.scheduler.Scheduler
import com.hifnawy.alquran.shared.R
import com.hifnawy.alquran.shared.domain.QuranCacheDataSource.CacheKey.Companion.asCacheKey
import com.hifnawy.alquran.shared.domain.QuranCacheDataSource.cacheDataSourceFactory
import com.hifnawy.alquran.shared.domain.QuranCacheDataSource.moveContentsTo
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.DownloadRequestId
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.DownloadState.State.COMPLETED
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.DownloadState.State.Companion.asDownloadState
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.DownloadState.State.DOWNLOADING
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.DownloadState.State.FAILED
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.DownloadState.State.QUEUED
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.UPDATE_PROGRESS_INTERVAL
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.completedDownloads
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.downloadManagerInstance
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.downloadMonitorJob
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.downloadPath
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.ioCoroutineScope
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.serializedData
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.updateDownloadState
import com.hifnawy.alquran.shared.domain.QuranDownloadService.Companion.DOWNLOAD_NOTIFICATION_CHANNEL_ID
import com.hifnawy.alquran.shared.domain.QuranDownloadService.Companion.FOREGROUND_NOTIFICATION_ID
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.debug
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.error
import com.hifnawy.alquran.shared.utils.LongEx.asHumanReadableSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * A [DownloadService] responsible for managing the downloading of Quran recitations.
 *
 * This service runs in the foreground to handle long-running download tasks, ensuring they are not
 * terminated by the system. It configures and provides a singleton instance of [DownloadManager]
 * for handling the download queue, processing requests, and managing download state.
 *
 * Key Responsibilities:
 * - **Initialization**: Sets up the [DownloadManager], notification helpers, and listeners upon creation.
 * - **Download Management**: Provides a configured [DownloadManager] with a custom [DownloaderFactory]
 *   that uses a `ProgressiveDownloader` and a custom cache data source.
 * - **Foreground Service**: Manages its lifecycle as a foreground service, displaying notifications
 *   to the user about the download progress (e.g., `downloading`, `queued`, `preparing`).
 * - **State Monitoring**: Actively monitors download progress and state changes through a [DownloadManager.Listener]
 *   and a dedicated coroutine job ([downloadMonitorJob]).
 * - **Post-Download Processing**: Once a download is completed, it moves the downloaded file from the
 *   ExoPlayer cache to its final destination in the application's storage. It then removes the
 *   completed download entry from the [DownloadManager].
 * - **Cleanup**: Cleans up resources, such as listeners and coroutine jobs, when the service is destroyed.
 *
 * This class is marked with [UnstableApi] as it relies on ExoPlayer's download components, which
 * are subject to change.
 *
 * @see DownloadService
 * @see DownloadManager
 * @see QuranDownloadManager
 */
@UnstableApi
class QuranDownloadService : DownloadService(
        FOREGROUND_NOTIFICATION_ID,
        UPDATE_PROGRESS_INTERVAL,
        DOWNLOAD_NOTIFICATION_CHANNEL_ID,
        R.string.download_channel_name,
        R.string.download_channel_description
), DownloadManager.Listener {

    /**
     * Companion object for [QuranDownloadService], holding constants used by the service.
     *
     * @property FOREGROUND_NOTIFICATION_ID [const Int][Int] The unique identifier for the foreground service notification.
     * @property DOWNLOAD_NOTIFICATION_CHANNEL_ID [const String][String] The ID for the notification channel used for download notifications.
     */
    private companion object {

        /**
         * Unique identifier for the foreground service notification.
         * This ID is used when starting the service in the foreground, ensuring that the download
         * process is not killed by the system.
         */
        private const val FOREGROUND_NOTIFICATION_ID = 1

        /**
         * The unique identifier for the notification channel used for displaying download progress
         * and status. This ID is used to create and manage the notification channel through which
         * all download-related notifications are sent.
         */
        private const val DOWNLOAD_NOTIFICATION_CHANNEL_ID = "Quran Downloads"
    }

    /**
     * A helper class for creating and updating notifications for downloads.
     * This helper is initialized in [onCreate] and is used by [getForegroundNotification] to
     * build the progress notifications that are displayed while the service is in the foreground.
     *
     * @see DownloadNotificationHelper
     * @see getForegroundNotification
     */
    private lateinit var notificationHelper: DownloadNotificationHelper

    /**
     * Called by the system when the service is first created.
     *
     * This method initializes essential components for the download service:
     * - It creates a [DownloadNotificationHelper] to manage and display download notifications.
     * - It registers this service as a [DownloadManager.Listener] to receive updates on download state changes.
     * - It starts a coroutine-based job ([downloadMonitorJob]) to periodically monitor and broadcast download progress.
     *
     * This setup ensures that the service is ready to handle download requests and provide real-time feedback to the user.
     *
     * @see DownloadService.onCreate
     * @see DownloadManager.addListener
     */
    override fun onCreate() {
        notificationHelper = DownloadNotificationHelper(this@QuranDownloadService, DOWNLOAD_NOTIFICATION_CHANNEL_ID)

        downloadManager.addListener(this@QuranDownloadService)
        startDownloadMonitor()

        super.onCreate()
    }

    /**
     * Called by the system when the service is no longer used and is being destroyed.
     *
     * This method performs essential cleanup tasks to release resources and prevent memory leaks:
     * - It removes the service as a [DownloadManager.Listener] to stop receiving download state updates.
     * - It cancels the [downloadMonitorJob] coroutine, which was responsible for periodically monitoring download progress.
     *
     * This ensures a graceful shutdown of the service's components.
     *
     * @see DownloadService.onDestroy
     * @see DownloadManager.removeListener
     */
    override fun onDestroy() {
        downloadManager.removeListener(this@QuranDownloadService)
        downloadMonitorJob?.cancel()

        super.onDestroy()
    }

    /**
     * Provides a singleton instance of [DownloadManager].
     *
     * This method is responsible for creating and configuring the [DownloadManager] used by the
     * service. It follows a singleton pattern, creating the manager only once and reusing the
     * instance for subsequent calls.
     *
     * The [DownloadManager] is configured with:
     * - A [DefaultDownloadIndex] for persisting download metadata.
     * - A custom [DownloaderFactory] that creates [ProgressiveDownloader] instances. These
     *   downloaders use a custom [cacheDataSourceFactory] to handle the download requests.
     * - A limit of 1 parallel download to ensure sequential processing.
     * - A minimum retry count of 3 for failed downloads.
     *
     * The created instance is stored in [downloadManagerInstance] for reuse.
     *
     * @return [DownloadManager] The singleton [DownloadManager] instance.
     */
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

    /**
     * Returns a scheduler for managing when downloads can run, or `null` if downloads are not scheduled.
     *
     * By returning `null`, this implementation indicates that downloads should be processed as soon
     * as they are added to the [DownloadManager] and are not subject to any specific scheduling
     * constraints (like requiring a network connection or the device to be charging). The downloads
     * will run whenever the service is active.
     *
     * @return [Scheduler?][Scheduler] Always `null` to disable download scheduling.
     */
    override fun getScheduler() = null

    /**
     * Builds and returns a notification to be displayed while the service is in the foreground.
     *
     * This method is called by the [DownloadService] to create a notification that reflects the
     * current state of the download queue. The notification's content is dynamically updated based
     * on the status of the downloads.
     *
     * The logic for the notification message is as follows:
     * - If there is an active download ([Download.STATE_DOWNLOADING]), the message will show detailed
     *   progress for that specific download, including Surah, reciter, moshaf, and percentage.
     * - If there are queued downloads ([Download.STATE_QUEUED]) but none are actively downloading,
     *   the message will indicate the number of queued items and details about the next item in the queue.
     * - If there are no active or queued downloads (e.g., downloads are in a preparing state),
     *   a generic "Preparing downloads" message is shown.
     *
     * The final notification is constructed using [DownloadNotificationHelper.buildProgressNotification],
     * which aggregates the status of all downloads into a single, comprehensive notification.
     *
     * @param downloads [MutableList< Download >][MutableList] A list of all current [Download]s being managed by the service.
     * @param notMetRequirements [Int] A bitmask of requirements (e.g., network connectivity) that are
     *   not currently met for the downloads to proceed.
     * @return [Notification] The [Notification] to be displayed for the foreground service.
     */
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

    /**
     * Starts a coroutine job to periodically monitor and broadcast the progress of active downloads.
     *
     * This function ensures that any existing monitoring job ([downloadMonitorJob]) is cancelled
     * before launching a new one to prevent multiple concurrent monitoring loops.
     *
     * The job runs in a `do-while(true)` loop on the [ioCoroutineScope] (backed by [Dispatchers.IO]).
     * Inside the loop, it performs the following actions:
     * - Fetches the list of [currentDownloads][DownloadManager.currentDownloads] from the
     *   [downloadManager][DownloadService.downloadManager].
     * - Filters the list to include only downloads that are in the [DOWNLOADING] state.
     * - Switches to the [Dispatchers.Main] context to safely update the UI-related state by calling
     *   [updateDownloadState] for each active download.
     *
     * The launched job is stored in the [downloadMonitorJob] property, allowing it to be
     * managed.
     */
    private fun startDownloadMonitor() = downloadMonitorJob.let { job ->
        job?.cancel()

        ioCoroutineScope.launch {
            do {
                downloadManager.currentDownloads.filter { it.state == DOWNLOADING.code }.run {
                    withContext(Dispatchers.Main) { forEach { updateDownloadState(it) } }
                }
            } while (true)
        }.also { downloadMonitorJob = it }
    }

    /**
     * Called when the state of a download changes.
     *
     * This callback is triggered by the [DownloadManager.Listener] whenever a download's status
     * is updated (e.g., from [QUEUED] to [DOWNLOADING], or from [DOWNLOADING] to [COMPLETED] or [FAILED]).
     *
     * The implementation focuses on handling two primary scenarios:
     * - **Successful Completion**: If a download completes successfully (`download.state` is
     *     [COMPLETED] and [finalException] is `null`), it performs the following actions:
     *     - Moves the downloaded file from the ExoPlayer cache to its final destination using [moveCompletedDownload].
     *     - Adds the completed [Download] object to the [completedDownloads] map for tracking.
     *     - Updates the download's state via [updateDownloadState] to notify observers.
     *
     * 2.  **Failure**: If [finalException] is not `null`, it logs an error with details about
     *     the failure.
     *
     * All other state changes are logged for debugging purposes but do not trigger specific actions
     * in this method. The processing is performed within the [ioCoroutineScope].
     *
     * @param downloadManager [DownloadManager] The [DownloadManager] that manages the download.
     * @param download [Download] The [Download] instance whose state has changed.
     * @param finalException [Exception?][Exception] The exception that caused the download to fail,
     *   or `null` if the state change was not caused by an error.
     */
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

    /**
     * Called when a download is permanently removed from the [DownloadManager].
     *
     * This callback is triggered after a download has been successfully processed
     * ([COMPLETED] / [FAILED]) and its entry is removed from the download manager's
     * index via [DownloadManager.removeDownload].
     *
     * This implementation performs cleanup by:
     * - Identifying the [DownloadRequestId] associated with the removed download.
     * - Removing the corresponding [Download] object from the [completedDownloads] tracking map.
     * - Logging the removal for debugging purposes.
     *
     * @param downloadManager [DownloadManager] The manager from which the download was removed.
     * @param download [Download] The [Download] instance that has been removed.
     */
    override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) = download.run {
        val serializedData = request.serializedData
        val downloadRequestId = DownloadRequestId(serializedData.reciter, serializedData.moshaf)

        completedDownloads[downloadRequestId]?.remove(this@run)

        Timber.debug("Download '${request.id}' removed!")
    }

    /**
     * Moves a completed download from the ExoPlayer cache to its final designated storage path
     * and then removes it from the [DownloadManager].
     *
     * This function is called once a download has successfully completed. It performs the following steps:
     * - It runs on the [Dispatchers.IO] context to avoid blocking the main thread.
     * - It retrieves the cache key from the download request ID.
     * - It calls [moveContentsTo] to move the files from the temporary cache location to the
     *   final path specified in the download request.
     * - It verifies that the number of bytes moved matches the expected content length of the download.
     *   If there is a mismatch, it logs an error. Otherwise, it logs a success message.
     * - Finally, it calls [DownloadManager.removeDownload] to clean up the completed download
     *   entry from the download manager's index, as it is no longer needed.
     *
     * @param download [Download] The completed download object containing the request details and metadata.
     */
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
