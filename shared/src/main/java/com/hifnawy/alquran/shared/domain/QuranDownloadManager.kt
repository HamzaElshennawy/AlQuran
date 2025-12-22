package com.hifnawy.alquran.shared.domain

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadProgress
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.offline.DownloadService.sendAddDownload
import androidx.media3.exoplayer.offline.DownloadService.sendRemoveDownload
import androidx.media3.exoplayer.offline.DownloadService.sendSetStopReason
import com.google.gson.Gson
import com.hifnawy.alquran.shared.domain.QuranCacheDataSource.CacheKey.Companion.asCacheKey
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.DownloadState.FailureReason.Companion.asFailureReason
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.DownloadState.State.COMPLETED
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.DownloadState.State.Companion.asDownloadState
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.DownloadState.State.DOWNLOADING
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.DownloadState.State.FAILED
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.DownloadState.State.QUEUED
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.DownloadState.State.STOPPED
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.UPDATE_PROGRESS_INTERVAL
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.cacheKey
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.completedDownloads
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.downloadJob
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.downloadMonitorJob
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.downloadStatusObservers
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.ioCoroutineScope
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.mainCoroutineScope
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.notifyDownloadStatusObservers
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.pauseDownload
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.pauseDownloads
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.queueDownload
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.queueDownloads
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.removeDownload
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.removeDownloads
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.resumeDownload
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.resumeDownloads
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.updateDownloadState
import com.hifnawy.alquran.shared.model.Moshaf
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.shared.model.Surah
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.debug
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.warn
import com.hifnawy.alquran.shared.utils.LongEx.asHumanReadableSize
import com.hifnawy.alquran.shared.utils.SerializableExt.Companion.asJsonString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.Serializable
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

/**
 * A singleton object that manages the downloading of Quran audio files (surahs).
 * It leverages [Media3's DownloadManager](https://developer.android.com/guide/topics/media/exoplayer/downloading-media)
 * to handle the lifecycle of downloads, including `queuing`, `resuming`, `pausing`, and `removing`.
 *
 * This manager is designed to handle both individual surah downloads and bulk downloads for a specific
 * reciter and moshaf. It keeps track of download `progress` and `state`, `notifying` observers of any changes.
 *
 * It uses a [QuranDownloadService] to manage downloads in the background and ensures that already
 * downloaded files are not re-downloaded. The state of each download is encapsulated in the [DownloadState]
 * data class, which is broadcast to registered [DownloadStatusObserver]s.
 *
 * Key functionalities:
 * - [queueDownloads]: Adds a list of surahs to the download queue.
 * - [resumeDownloads]: Resumes paused or failed downloads.
 * - [pauseDownloads]: Pauses all ongoing downloads for a given request.
 * - [removeDownloads]: Removes downloads and their associated files.
 * - [DownloadStatusObserver]: An interface to subscribe to download status updates.
 *
 * This object is marked with [UnstableApi] as it uses experimental APIs from Media3 ExoPlayer.
 */
@UnstableApi
object QuranDownloadManager {

    /**
     * A list of observers that are subscribed to download status updates.
     *
     * This list holds references to objects implementing the [DownloadStatusObserver] interface.
     * When a download's state changes (e.g., `progress update`, `completion`, `failure`), the
     * [QuranDownloadManager] iterates through this list and notifies each observer
     * by calling its `onDownloadStateChanged` method.
     *
     * Observers can be added or removed from this list to dynamically manage who receives
     * download status notifications.
     *
     * @see DownloadStatusObserver
     * @see notifyDownloadStatusObservers
     */
    val downloadStatusObservers = mutableListOf<DownloadStatusObserver>()

    /**
     * The interval, in milliseconds, at which download progress updates are processed and sent to observers.
     * A smaller value provides more frequent updates but may increase processing overhead.
     */
    internal const val UPDATE_PROGRESS_INTERVAL = 100L

    /**
     * A [CoroutineScope] for I/O-bound operations, such as file access or network requests.
     * It uses the [Dispatchers.IO] dispatcher to run tasks on a background thread pool,
     * preventing them from blocking the main thread. A new [Job] is created to allow for
     * cancellation of all coroutines launched within this scope.
     */
    internal val ioCoroutineScope by lazy { CoroutineScope(Dispatchers.IO + Job()) }

    /**
     * A [CoroutineScope] bound to the main UI thread.
     *
     * This scope is used for launching coroutines that need to interact with the UI,
     * such as updating download progress or notifying observers about state changes.
     * The new [Job] ensures that this scope has its own lifecycle and can be cancelled
     * independently if needed, without affecting other scopes.
     */
    internal val mainCoroutineScope by lazy { CoroutineScope(Dispatchers.Main + Job()) }

    /**
     * A map to cache downloads that are already completed.
     *
     * This is used to immediately report the status of already-downloaded files when a download
     * request is queued, without waiting for the [DownloadManager] to report their state. This is
     * particularly useful for bulk operations where some files might already exist on disk.
     *
     * The key is a [DownloadRequestId], which uniquely identifies a download batch (by reciter and moshaf),
     * and the value is a list of [Download] objects representing the individual completed surahs.
     */
    internal val completedDownloads = mutableMapOf<DownloadRequestId, MutableList<Download>>()

    /**
     * An extension property for [ByteArray] that converts the byte array into a [String]
     * using the UTF-8 charset. This is useful for deserializing data stored in a [DownloadRequest].
     *
     * @receiver [ByteArray] The byte array to be converted.
     *
     * @return [String] The converted string.
     */
    internal val ByteArray.asUTF8String get() = String(this@asUTF8String, Charsets.UTF_8)

    /**
     * A computed property that generates a unique cache key for a given [DownloadRequestData].
     * The key is a string formatted as `reciter_#{reciter_id}_moshaf_#{moshaf_id}_surah_#{surah_id}`,
     * ensuring that each audio file for a specific reciter, moshaf, and surah has a distinct identifier
     * in the cache. The surah ID is padded with leading zeros to ensure consistent formatting.
     *
     * This key is crucial for the [DownloadManager] to identify and manage the cached content.
     *
     * @receiver [DownloadRequestData] The data for which the cache key is to be generated.
     *
     * @return [String] The generated cache key.
     */
    internal val DownloadRequestData.cacheKey
        get() = "reciter_#${reciter.id.value}_moshaf_#${moshaf.id}_surah_#${surah.id.toString().padStart(3, '0')}".asCacheKey

    /**
     * An extension property for [DownloadRequestData] that constructs a [DownloadRequest] object.
     * This object encapsulates all necessary information for the [DownloadManager] to process
     * a download, including the unique ID, the URI of the media to be downloaded, a custom cache key,
     * and the serialized [DownloadRequestData] itself as custom data.
     *
     * It uses the [cacheKey] property to generate a unique identifier for the download, ensuring
     * that the same surah for a specific reciter and moshaf is not downloaded multiple times.
     * The [DownloadRequestData] is serialized to a JSON string and stored as a byte array, allowing
     * to be retrieved later to identify the download's metadata.
     *
     * If the [surah.uri][Surah.uri] is `null`, this property will return `null`, indicating that a download
     * request cannot be created.
     *
     * @receiver [DownloadRequestData] The data object containing reciter, moshaf, and surah information.
     *
     * @return [DownloadRequest] A configured [DownloadRequest] instance, or `null` if the surah's URI is not available.
     */
    internal val DownloadRequestData.downloadRequest
        get() = surah.uri?.toUri()?.let { uri ->
            DownloadRequest.Builder(cacheKey.value, uri).run {
                setCustomCacheKey(cacheKey.value)
                setData(asJsonString.toByteArray(Charsets.UTF_8))
                build()
            }
        }

    /**
     * An extension property for [DownloadRequest] that deserializes its [DownloadRequest.data] field
     * from a JSON byte array into a [DownloadRequestData] object.
     *
     * This allows easy access to the custom metadata (reciter, moshaf, surah)
     * associated with a download request. The [DownloadRequest.data] is assumed to be a UTF-8 encoded
     * JSON string.
     *
     * @receiver [DownloadRequest] The [DownloadRequest] instance whose data needs to be deserialized.
     *
     * @return [DownloadRequestData] The deserialized [DownloadRequestData] object.
     */
    internal val DownloadRequest.serializedData get() = Gson().fromJson(data.asUTF8String, DownloadRequestData::class.java)

    /**
     * An extension property for [File] that creates a [Download] object representing a fully completed download.
     *
     * This is used to mock a [Download] object for a file that already exists on disk, allowing the manager to
     * treat it as a completed download without it having been processed by the [DownloadManager].
     *
     * It populates the [Download] object with metadata indicating completion:
     * - State is set to [Download.STATE_COMPLETED].
     * - Progress is set to `100%`.
     * - `bytesDownloaded` and `contentLength` are set to the file's size.
     *
     * The property requires a `downloadRequest: DownloadRequest` in its context to associate the created
     * [Download] object with the original request.
     *
     * @receiver [File] The [File] that represents the completed download.
     *
     * @return [Download] A new [Download] instance configured as a completed download.
     */
    context(downloadRequest: DownloadRequest)
    internal val File.completedDownload
        get() = Download(
                downloadRequest,
                COMPLETED.code,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                length(),
                STOPPED.code,
                Download.FAILURE_REASON_NONE,
                DownloadProgress().apply {
                    bytesDownloaded = length()
                    percentDownloaded = 100f
                }
        )

    /**
     * An extension property for [DownloadRequest] that constructs the local file path for a downloaded audio file.
     * The path is structured to ensure that each audio file has a unique location based on the reciter,
     * moshaf, and surah.
     *
     * The file path follows this pattern:
     * `<app_files_dir>/downloads/reciter_#<reciter_id>/moshaf_#<moshaf_id>/surah_#<surah_id>.mp3`
     *
     * This property requires a [Context] in its scope to access the application's file directory.
     * It deserializes the [DownloadRequest.data] to get the necessary metadata (reciter, moshaf, surah)
     * to build the path.
     *
     * @receiver [DownloadRequest] The [DownloadRequest] instance for which the path is being generated.
     *
     * @return [File] A [File] object representing the expected destination path of the downloaded audio file.
     */
    context(context: Context)
    internal val DownloadRequest.downloadPath
        get() = serializedData.let { (reciter, moshaf, surah) ->
            val surahNum = surah.id.toString().padStart(3, '0')
            val baseDestination = File(context.filesDir, "downloads")

            File(baseDestination, "reciter_#${reciter.id.value}/moshaf_#${moshaf.id}/surah_#$surahNum.mp3")
        }

    /**
     * A coroutine [Job] that manages the lifecycle of bulk download operations, such as
     * [queueDownloads] and [resumeDownloads].
     *
     * This job is used to track and control the coroutine that processes a list of downloads.
     * It can be cancelled to stop the ongoing bulk operation, for example, when the user
     * decides to pause all downloads.
     *
     * @see queueDownloads
     * @see resumeDownloads
     * @see pauseDownloads
     * @see removeDownloads
     */
    internal var downloadJob: Job? = null

    /**
     * A coroutine [Job] that periodically monitors the status of all active downloads
     * managed by the [DownloadManager].
     *
     * This job is typically launched when the [QuranDownloadService] starts and is responsible
     * for iterating through the [DownloadManager.currentDownloads] list, collecting progress information,
     * and dispatching updates to any registered observers via [updateDownloadState].
     *
     * It runs in a loop until cancelled, for example, when all downloads are paused or removed,
     * or when the service is destroyed. Cancelling this job stops the progress updates.
     *
     * @see downloadJob
     * @see QuranDownloadService
     * @see QuranDownloadService.startDownloadMonitor
     */
    internal var downloadMonitorJob: Job? = null

    /**
     * A nullable instance of [Media3's DownloadManager](https://developer.android.com/reference/androidx/media3/exoplayer/offline/DownloadManager).
     *
     * This property holds the central component for managing all download operations. It is
     * initialized and managed by the [QuranDownloadService] and accessed by this manager
     * to query download states, and to send commands like `pause`, `resume`, or `remove`.
     *
     * It is declared as `internal` and `nullable` because its lifecycle is tied to the
     * background [DownloadService].
     *
     * @see QuranDownloadService
     * @see DownloadManager
     */
    internal var downloadManagerInstance: DownloadManager? = null

    /**
     * An observer interface for receiving updates on the state of Quran audio downloads.
     *
     * Implement this interface to subscribe to notifications from the [QuranDownloadManager].
     * The [onDownloadStateChanged] method will be called on a coroutine whenever a download's
     * status changes, such as progress updates, completion, failure, or state transitions
     * (e.g., from [QUEUED] to [DOWNLOADING]).
     *
     * This allows UI components or other parts of the application to react to the download
     * lifecycle in real-time. The method is a `suspend` function, allowing for asynchronous
     * operations within the observer's implementation.
     *
     * Example Usage:
     * ```kotlin
     * class MyDownloadUi : DownloadStatusObserver {
     *     init {
     *         QuranDownloadManager.downloadStatusObservers.add(this)
     *     }
     *
     *     override suspend fun onDownloadStateChanged(downloadState: DownloadState) {
     *         // Update UI with the new state, e.g., progress bar, status text.
     *         withContext(Dispatchers.Main) {
     *             updateProgressBar(downloadState.percentage)
     *             statusTextView.text = "Status: ${downloadState.state}"
     *         }
     *     }
     *
     *     fun cleanup() {
     *         QuranDownloadManager.downloadStatusObservers.remove(this)
     *     }
     * }
     * ```
     *
     * @see DownloadState
     * @see QuranDownloadManager.downloadStatusObservers
     */
    fun interface DownloadStatusObserver : IObservable {

        /**
         * A suspend function that is invoked when the state of a download changes.
         *
         * This callback receives a [DownloadState] object containing the latest information
         * about a download, such as its progress, current state (e.g., [DOWNLOADING],
         * [COMPLETED]), and associated metadata.
         *
         * Implementations of this method should handle UI updates or other logic based on the
         * received [downloadState]. As a [suspend] function, it can perform asynchronous
         * operations without blocking the caller's thread.
         *
         * @param downloadState [DownloadState] The [DownloadState] object representing the current status
         *                      of the download.
         */
        suspend fun onDownloadStateChanged(downloadState: DownloadState)
    }

    /**
     * Represents a request to download a collection of surahs for a specific reciter and moshaf.
     *
     * This data class is used to group multiple download operations into a single logical unit. It's
     * passed to functions like [queueDownloads], [resumeDownloads], [pauseDownloads], and [removeDownloads]
     * to manage the lifecycle of a set of related audio files.
     *
     * @property reciter [Reciter] The [Reciter] for whom the surahs are being downloaded.
     * @property moshaf [Moshaf] The [Moshaf] (Quran version) to which the surahs belong.
     * @property surahs [List< Surah >][List] A list of [Surah] objects to be downloaded.
     */
    data class BulkDownloadRequest(val reciter: Reciter, val moshaf: Moshaf, val surahs: List<Surah>) : Serializable {

        /**
         * Provides a human-readable string representation of the [DownloadRequestId].
         * This is useful for logging and debugging purposes, as it clearly shows the
         * reciter and moshaf associated with the request.
         *
         * @return [String] A string in the format: `BulkDownloadRequest(reciter=(id: name), moshaf=(id: name), surahs=<size>[...])`.
         */
        override fun toString() = "${this::class.java.simpleName}(" +
                                  "${::reciter.name}=(${reciter.id.value}: ${reciter.name}), " +
                                  "${::moshaf.name}=(${moshaf.id}: ${moshaf.name}), " +
                                  "${::surahs.name}=<${surahs.size}>" +
                                  surahs.joinToString(prefix = "[", postfix = "]", separator = ", ") { "(${it.id}: ${it.name})" } +
                                  ")"
    }

    /**
     * Represents the state of a single Quran audio file download at a given moment.
     *
     * This data class encapsulates all relevant information about a download, such as its current
     * status (e.g., [DOWNLOADING], [COMPLETED]), progress percentage, bytes downloaded, total size,
     * and any failure reasons. It also includes the [DownloadRequestData], which contains metadata
     * about the download, such as the reciter, moshaf, and surah.
     *
     * Instances of this class are created by the [QuranDownloadManager] and broadcast to all
     * registered [DownloadStatusObserver]s to provide real-time updates on download progress
     * and state changes.
     *
     * @property data [DownloadRequestData] The metadata associated with the download, including [Reciter], [Moshaf], and [Surah].
     * @property state [State] The current state of the download, such as [QUEUED], [DOWNLOADING], or [COMPLETED].
     * @property percentage [Float] The download progress as a float value from `0f` to `100f`.
     * @property downloaded [Long] The number of bytes that have been downloaded so far.
     * @property total [Long] The total size of the file in bytes. This may be unknown (`-1`) initially.
     * @property failureReason [FailureReason] The reason for the download failure, if any. Defaults to [FailureReason.NONE].
     *
     * @see DownloadStatusObserver
     * @see QuranDownloadManager.updateDownloadState
     */
    data class DownloadState(
            val data: DownloadRequestData = DownloadRequestData(),
            val state: State = STOPPED,
            val percentage: Float = 0f,
            val downloaded: Long = 0L,
            val total: Long = 0L,
            val failureReason: FailureReason = Download.FAILURE_REASON_NONE.asFailureReason
    ) : Serializable {

        /**
         * Represents the various states of a download, mirroring the states defined in [Download].
         * Each state is associated with a specific integer code from the Media3 library, providing
         * a type-safe way to interpret download statuses.
         *
         * @property code [Int] The integer constant from [Download] that represents this state.
         *
         * @see Download
         * @see Download.state
         */
        enum class State(val code: Int) : Serializable {

            /**
             * The download is waiting in the queue to be started. This occurs when other downloads
             * are in progress or the maximum number of parallel downloads has been reached.
             *
             * @see STOPPED
             * @see COMPLETED
             * @see FAILED
             * @see DOWNLOADING
             * @see REMOVING
             */
            QUEUED(Download.STATE_QUEUED),

            /**
             * The download is stopped and can be resumed. A download may be stopped because it was paused by
             * the user, or because it failed and the service is waiting to retry.
             *
             * @see QUEUED
             * @see COMPLETED
             * @see FAILED
             * @see DOWNLOADING
             * @see REMOVING
             */
            STOPPED(Download.STATE_STOPPED),

            /**
             * The download has finished successfully. The downloaded content is now available
             * for offline playback.
             *
             * @see QUEUED
             * @see STOPPED
             * @see FAILED
             * @see DOWNLOADING
             * @see REMOVING
             */
            COMPLETED(Download.STATE_COMPLETED),

            /**
             * The download has failed and cannot be resumed. This state is terminal and may occur
             * due to network issues, invalid URLs, or other errors. The reason for the failure can
             * be found in the [failureReason] property of the [DownloadState] object.
             *
             * @see QUEUED
             * @see STOPPED
             * @see COMPLETED
             * @see DOWNLOADING
             * @see REMOVING
             */
            FAILED(Download.STATE_FAILED),

            /**
             * The download is actively in progress. The download manager is currently fetching data
             * for the media item from the network and writing it to the disk.
             *
             * @see QUEUED
             * @see STOPPED
             * @see COMPLETED
             * @see FAILED
             * @see REMOVING
             */
            DOWNLOADING(Download.STATE_DOWNLOADING),

            /**
             * The download is being removed. This is a transient state that occurs after a removal
             * request has been made. Once the removal process is complete, the download will no longer
             * be tracked by the [DownloadManager].
             *
             * @see QUEUED
             * @see STOPPED
             * @see COMPLETED
             * @see FAILED
             * @see DOWNLOADING
             */
            REMOVING(Download.STATE_REMOVING);

            /**
             * A companion object for the [State] enum, providing utility functions.
             * It includes an extension property to convert an [Int] code from the Media3 library
             * into its corresponding [State] enum constant.
             *
             * @see Int.asDownloadState
             */
            companion object {

                /**
                 * An extension property that converts an integer code from [Download] into its
                 * corresponding [DownloadState.State] enum constant. This provides a type-safe way
                 * to interpret the state of a download.
                 *
                 * It searches through the [entries] of the [DownloadState.State] enum and returns the first
                 * entry whose [code] matches the receiver integer. If no match is found, it will
                 * throw a [NoSuchElementException].
                 *
                 * @receiver [Int] The integer code representing a download state (e.g., [Download.STATE_DOWNLOADING]).
                 *
                 * @return [DownloadState.State] The matching enum constant for the given code.
                 *
                 * @throws NoSuchElementException If no matching enum constant is found.
                 *
                 * @see Download.state
                 */
                val Int.asDownloadState get() = entries.first { it.code == this }
            }
        }

        /**
         * Represents the reason why a download failed, mirroring the failure reasons defined in [Download].
         * This provides a more specific cause of failure than the general [State.FAILED] state.
         *
         * @property code [Int] The integer constant from [Download] that represents this failure reason.
         *
         * @see Download.failureReason
         */
        enum class FailureReason(val code: Int) : Serializable {

            /**
             * The download has not failed. This is the default state when a download is
             * [QUEUED], [DOWNLOADING], or [COMPLETED].
             *
             * @see UNKNOWN
             */
            NONE(Download.FAILURE_REASON_NONE),

            /**
             * The download failed for a reason that is not otherwise categorized.
             * This serves as a catch-all for unexpected errors during the download process.
             *
             * @see NONE
             */
            UNKNOWN(Download.FAILURE_REASON_UNKNOWN);

            /**
             * A companion object for the [FailureReason] enum.
             *
             * This object provides utility functions and extensions related to failure reasons.
             *
             * @see FailureReason
             */
            companion object {

                /**
                 * An extension property for [Int] that converts a failure reason code into its
                 * corresponding [FailureReason] enum constant. This is used to map the integer
                 * codes from [Download.failureReason] to a type-safe enum.
                 *
                 * It finds the first [FailureReason] entry whose [code] matches the receiver integer.
                 *
                 * @receiver [Int] The failure reason code from a [Download] object.
                 *
                 * @return [FailureReason] The matching [FailureReason] enum constant.
                 *
                 * @see Download.failureReason
                 */
                val Int.asFailureReason get() = entries.first { it.code == this }
            }
        }

        /**
         * Provides a human-readable string representation of the [DownloadRequestId].
         * This is useful for logging and debugging purposes, as it clearly shows the
         * reciter and moshaf associated with the request.
         *
         * @return [String] A string in the format:
         *   `DownloadState(data=..., state=..., percentage=...%, downloaded=..., total=..., failureReason=...)`.
         *   Byte counts are formatted as human-readable sizes (e.g., KB, MB).
         */
        override fun toString() = "${this::class.java.simpleName}(" +
                                  "${::data.name}=${data}, " +
                                  "${::state.name}=${state.name}, " +
                                  "${::percentage.name}=${String.format(Locale.ENGLISH, "%06.2f", percentage)}%, " +
                                  "${::downloaded.name}=${downloaded.asHumanReadableSize}, " +
                                  "${::total.name}=${total.asHumanReadableSize}, " +
                                  "${::failureReason.name}=${failureReason.name}" +
                                  ")"
    }

    /**
     * A data class that holds the essential metadata for a single download operation.
     *
     * This class encapsulates the specific `reciter`, `moshaf`, and `surah`
     * that are the subject of a download request. It is serialized into a JSON string and
     * attached to a [DownloadRequest] as custom data. This allows the [QuranDownloadManager]
     * to later identify and manage the download, associating it with the correct Quranic content.
     *
     * It implements [Serializable] to allow it to be easily passed between components.
     *
     * @property reciter [Reciter] The reciter of the audio file to be downloaded. Defaults to an empty [Reciter] object.
     * @property moshaf [Moshaf] The specific Quran moshaf (e.g., Hafs, Warsh) for the download. Defaults to an empty [Moshaf] object.
     * @property surah [Surah] The surah (chapter) to be downloaded. Defaults to an empty [Surah] object.
     *
     * @see DownloadRequest
     * @see DownloadRequest.serializedData
     */
    data class DownloadRequestData(val reciter: Reciter = Reciter(), val moshaf: Moshaf = Moshaf(), val surah: Surah = Surah()) : Serializable {

        /**
         * Provides a human-readable string representation of the [DownloadState].
         *
         * This override is primarily for logging and debugging purposes, offering a clear and
         * detailed summary of the download's current status.
         *
         * @return [String] A string in the format: `DownloadRequestData(reciter=(id: name), moshaf=(id: name), surah=(id: name))`.
         */
        override fun toString() = "${this::class.java.simpleName}(" +
                                  "${::reciter.name}=(${reciter.id.value}: ${reciter.name}), " +
                                  "${::moshaf.name}=(${moshaf.id}: ${moshaf.name}), " +
                                  "${::surah.name}=(${surah.id}: ${surah.name})" +
                                  ")"
    }

    /**
     * Uniquely identifies a bulk download request by combining a [Reciter] and a [Moshaf].
     *
     * This class serves as a key for managing and tracking groups of related downloads,
     * such as all surahs for a specific reciter's moshaf. It is used internally by the
     * [QuranDownloadManager] to associate download operations with the correct [BulkDownloadRequest].
     *
     * Since it only contains the [Reciter] and [Moshaf], it provides a stable identifier
     * for a collection of surahs, regardless of how many individual surahs are in the request.
     *
     * @property reciter [Reciter] The [Reciter] associated with the download request.
     * @property moshaf [Moshaf] The [Moshaf] associated with the download request.
     *
     * @see BulkDownloadRequest
     * @see completedDownloads
     */
    internal data class DownloadRequestId(val reciter: Reciter, val moshaf: Moshaf) : Serializable {

        /**
         * Provides a human-readable string representation of the [DownloadRequestId].
         * This is useful for logging and debugging purposes, as it clearly shows the
         * reciter and moshaf associated with the request.
         *
         * @return [String] A string in the format: `DownloadRequestId(reciter=(id: name), moshaf=(id: name))`.
         */
        override fun toString() = "${this::class.java.simpleName}(" +
                                  "${::reciter.name}=(${reciter.id.value}: ${reciter.name}), " +
                                  "${::moshaf.name}=(${moshaf.id}: ${moshaf.name})" +
                                  ")"
    }

    /**
     * Queues a list of surahs for download based on a [BulkDownloadRequest].
     *
     * This function processes each surah in the request, checks if the corresponding audio file
     * already exists on disk, and then takes one of two actions:
     * - **File Exists**: If the file is already downloaded, it immediately reports its status as [COMPLETED]
     *   to all registered [DownloadStatusObserver]s without adding it to the [DownloadManager].
     * - **File Does Not Exist**: If the file is not yet downloaded, it creates a [DownloadRequest] and
     *   sends it to the [QuranDownloadService] to be added to the download queue.
     *
     * The entire operation is managed by a [downloadJob], which allows the process to be controlled (e.g., `cancelled`).
     *
     * If there're completed downloads, a new coroutine is launched on the [mainCoroutineScope] to handle state updates
     * and queuing, ensuring that UI-related notifications are dispatched safely.
     *
     * @param context [Context] The application [Context], required to access the file system and send commands to
     *   the [DownloadService].
     * @param bulkDownloadRequest [BulkDownloadRequest] The [BulkDownloadRequest] containing the reciter, moshaf,
     *   and list of surahs to be queued for download.
     *
     * @see BulkDownloadRequest
     * @see DownloadStatusObserver
     * @see queueDownload
     * @see updateDownloadState
     */
    fun queueDownloads(context: Context, bulkDownloadRequest: BulkDownloadRequest) = bulkDownloadRequest.surahs.map { surah ->
        val downloadRequest = with(bulkDownloadRequest) { DownloadRequestData(reciter, moshaf, surah).downloadRequest } ?: return
        val downloadPath = with(context) { downloadRequest.downloadPath }

        when {
            downloadPath.exists() -> downloadRequest to downloadPath
            else                  -> downloadRequest to null
        }
    }.run {
        val queuedDownloads = filter { (_, downloadPath) -> downloadPath == null }

        @Suppress("UNCHECKED_CAST")
        val finishedDownloads = filter { (_, downloadPath) -> downloadPath != null } as List<Pair<DownloadRequest, File>>

        mainCoroutineScope.launch {
            finishedDownloads.forEach { (downloadRequest, downloadPath) ->
                val surah = downloadRequest.serializedData.surah
                val download = with(downloadRequest) { downloadPath.completedDownload }
                val downloadRequestId = with(bulkDownloadRequest) { DownloadRequestId(reciter, moshaf) }

                Timber.debug("Queued Download '${downloadRequest.id}': (${surah.id}: ${surah.name}) is already ${COMPLETED}!")

                completedDownloads[downloadRequestId]?.run {
                    add(size, download)
                } ?: run {
                    completedDownloads[downloadRequestId] = mutableListOf(download)
                }

                updateDownloadState(download)
            }

            queuedDownloads.forEach { (downloadRequest, _) -> queueDownload(context, downloadRequest) }

            Timber.debug("${queuedDownloads.size} Downloads Queued!")
        }.also { downloadJob = it }.run { Timber.debug("Queueing ${queuedDownloads.size} Downloads...") }
    }

    /**
     * Resumes a set of paused or failed downloads specified in a [BulkDownloadRequest].
     *
     * This function iterates through the surahs in the [bulkDownloadRequest]. For each surah, it checks if the
     * corresponding audio file already exists on disk.
     * - If a **file exists**, it's treated as an already completed download, and a [COMPLETED]
     *   status is immediately broadcast to observers.
     * - If a **file does not exist**, a command is sent to the [QuranDownloadService] to resume the download.
     *   The [DownloadManager] will then restart any downloads that are in a [STOPPED] or [FAILED] state.
     *
     * The process is executed within a coroutine on the [mainCoroutineScope], and the operation
     * is tracked by the [downloadJob].
     *
     * @param context [Context] The application context, required to send commands to the [QuranDownloadService] and check file paths.
     * @param bulkDownloadRequest [BulkDownloadRequest] An object containing the reciter, moshaf, and list of surahs to be resumed.
     *
     * @see resumeDownload
     * @see QuranDownloadService
     * @see DownloadManager.setStopReason
     */
    fun resumeDownloads(context: Context, bulkDownloadRequest: BulkDownloadRequest) = bulkDownloadRequest.surahs.map { surah ->
        val downloadRequest = with(bulkDownloadRequest) { DownloadRequestData(reciter, moshaf, surah).downloadRequest } ?: return
        val downloadPath = with(context) { downloadRequest.downloadPath }

        when {
            downloadPath.exists() -> downloadRequest to downloadPath
            else                  -> downloadRequest to null
        }
    }.run {
        val queuedDownloads = filter { (_, downloadPath) -> downloadPath == null }

        @Suppress("UNCHECKED_CAST")
        val finishedDownloads = filter { (_, downloadPath) -> downloadPath != null } as List<Pair<DownloadRequest, File>>

        mainCoroutineScope.launch {
            finishedDownloads.forEach { (downloadRequest, downloadPath) ->
                val surah = downloadRequest.serializedData.surah
                val download = with(downloadRequest) { downloadPath.completedDownload }
                val downloadRequestId = with(bulkDownloadRequest) { DownloadRequestId(reciter, moshaf) }

                Timber.debug("Resumed Download '$downloadRequestId': (${surah.id}: ${surah.name}) is already ${COMPLETED}!")

                updateDownloadState(download)
            }

            queuedDownloads.forEach { (downloadRequest, _) -> resumeDownload(context, downloadRequest) }

            Timber.debug("${queuedDownloads.size} Downloads Resumed!")
        }.also { downloadJob = it }.run { Timber.debug("Resuming ${queuedDownloads.size} Downloads...") }
    }

    /**
     * Pauses all ongoing or queued downloads associated with a [BulkDownloadRequest].
     *
     * This function first cancels any active bulk operations ([downloadJob]) and stops the
     * progress monitoring loop ([downloadMonitorJob]) to prevent further processing or status updates.
     *
     * It then iterates through the surahs specified in the [bulkDownloadRequest] and sends a command
     * to the [QuranDownloadService] to pause each corresponding download. Pausing is achieved by setting a
     * non-zero [Download.stopReason] on the download via [DownloadService.sendSetStopReason].
     *
     * The operation is performed asynchronously on the [ioCoroutineScope] to avoid blocking the main thread.
     *
     * @param context [Context] The application context, required for sending commands to the [DownloadService].
     * @param bulkDownloadRequest [BulkDownloadRequest] An object defining the reciter, moshaf, and list of surahs
     *   to be paused.
     *
     * @see pauseDownload
     * @see QuranDownloadService
     * @see DownloadManager.setStopReason
     */
    fun pauseDownloads(context: Context, bulkDownloadRequest: BulkDownloadRequest) = bulkDownloadRequest.run {
        downloadJob?.cancel()
        downloadMonitorJob?.cancel()

        val downloadRequestId = DownloadRequestId(reciter, moshaf)
        var pausedDownloadsSize = 0

        ioCoroutineScope.launch {
            surahs.forEach { surah ->
                val download = downloadManagerInstance?.currentDownloads?.find { it.request.serializedData.surah.id == surah.id }

                download?.let {
                    Timber.debug("Download '$downloadRequestId': (${surah.id}: ${surah.name}) is ${it.state.asDownloadState}!")

                    pauseDownload(context, it.request).also { pausedDownloadsSize++ }
                } ?: Timber.warn("Download '$downloadRequestId': (${surah.id}: ${surah.name}) is non-existent!!!")
            }

            Timber.debug("$pausedDownloadsSize Downloads Paused!")
        }.run { Timber.debug("Pausing ${surahs.size} Downloads...") }
    }

    /**
     * Removes a set of downloads specified in a [BulkDownloadRequest], including their associated files.
     *
     * This function first cancels any ongoing bulk operations ([downloadJob] and [downloadMonitorJob])
     * to prevent conflicts. It then iterates through the surahs in the [bulkDownloadRequest]
     * and sends a command to the [QuranDownloadService] to remove each corresponding download.
     *
     * The removal process includes deleting the cached content from the disk. This operation is
     * performed asynchronously on an I/O-optimized coroutine scope. It also removes the download
     * from the [completedDownloads] map if it exists there.
     *
     * @param context [Context] The application context, required to send commands to the [QuranDownloadService].
     * @param bulkDownloadRequest [BulkDownloadRequest] An object containing the reciter, moshaf, and list of
     *   surahs to be removed.
     *
     * @see removeDownload
     * @see QuranDownloadService
     * @see DownloadManager.removeDownload
     */
    fun removeDownloads(context: Context, bulkDownloadRequest: BulkDownloadRequest) = bulkDownloadRequest.run {
        downloadJob?.cancel()
        downloadMonitorJob?.cancel()

        val downloadRequestId = DownloadRequestId(reciter, moshaf)
        var removedDownloadsSize = 0

        ioCoroutineScope.launch {
            surahs.forEach { surah ->
                val download = downloadManagerInstance?.currentDownloads?.find { it.request.serializedData.surah.id == surah.id }

                download?.let {
                    Timber.debug("Download '$downloadRequestId': (${surah.id}: ${surah.name}) is ${it.state.asDownloadState}!")

                    completedDownloads[downloadRequestId]?.remove(it)

                    removeDownload(context, it.request).also { removedDownloadsSize++ }
                } ?: Timber.warn("Download '$downloadRequestId': (${surah.id}: ${surah.name}) is non-existent!!!")
            }

            Timber.debug("$removedDownloadsSize Downloads Removed!")
        }.run { Timber.debug("Removing ${surahs.size} Downloads...") }
    }

    /**
     * Queues a single audio file for download using a [DownloadRequest].
     *
     * This function sends an `add download` command to the [QuranDownloadService], which then
     * handles the request. The download will be added to the [DownloadManager]'s queue and will
     * start automatically when resources are available. The `foreground` parameter is set to `true`,
     * ensuring the download service runs in the foreground, which is necessary for long-running tasks
     * on modern Android versions.
     *
     * It logs the queuing action for debugging purposes, including the download's unique ID and the
     * associated surah information.
     *
     * @param context [Context] The application context, needed to send the command to the service.
     * @param downloadRequest [DownloadRequest] The request object containing all necessary information
     *   for the download, such as the URI, cache key, and custom data.
     *
     * @see sendAddDownload
     */
    fun queueDownload(context: Context, downloadRequest: DownloadRequest) = downloadRequest.run {
        val serializedData = downloadRequest.serializedData
        Timber.debug("Queueing download '$id': (${serializedData.surah.id}: ${serializedData.surah.name})...")

        sendAddDownload(
                context,
                QuranDownloadService::class.java,
                this@run,
                true
        )
    }

    /**
     * Resumes a single paused or failed download.
     *
     * This function sends a command to the [QuranDownloadService] to resume a specific download
     * identified by its [DownloadRequest]. It works by setting the download's stop reason to
     * [Download.STOP_REASON_NONE], which signals the [DownloadManager] to restart the download
     * if it was previously paused or had failed with a retryable error.
     *
     * @param context [Context] The application context, needed to send the resume command to the [DownloadService].
     * @param downloadRequest [DownloadRequest] The [DownloadRequest] for the download to be resumed.
     *
     * @see resumeDownloads
     * @see DownloadManager.setStopReason
     */
    fun resumeDownload(context: Context, downloadRequest: DownloadRequest) = downloadRequest.run {
        val serializedData = downloadRequest.serializedData
        Timber.debug("Resuming download '$id': (${serializedData.surah.id}: ${serializedData.surah.name})...")

        sendSetStopReason(
                context,
                QuranDownloadService::class.java,
                downloadRequest.id,
                Download.STOP_REASON_NONE, // Zero value resumes the download
                true
        )
    }

    /**
     * Pauses a single download identified by a [DownloadRequest].
     *
     * This function sends a command to the [QuranDownloadService] to stop the specified download.
     * The download's state will be set to [STOPPED], allowing it to be resumed later.
     * A non-zero `stopReason` is used to indicate that the download is being intentionally paused.
     *
     * @receiver [DownloadRequest] The download request to be paused.
     * @param context [Context] The application context, required to send commands to the [QuranDownloadService].
     * @param downloadRequest [DownloadRequest] The specific download request to be paused.
     *
     * @see pauseDownloads
     * @see DownloadManager.setStopReason
     */
    fun pauseDownload(context: Context, downloadRequest: DownloadRequest) = downloadRequest.run {
        val serializedData = downloadRequest.serializedData
        Timber.debug("Pausing download '$id': (${serializedData.surah.id}: ${serializedData.surah.name})...")

        sendSetStopReason(
                context,
                QuranDownloadService::class.java,
                downloadRequest.id,
                Download.STOP_REASON_NONE + 1, // Any non-zero value pauses the download
                true
        )
    }

    /**
     * Sends a command to the [QuranDownloadService] to remove a single download.
     *
     * This function initiates the removal of a specific download identified by the provided
     * [downloadRequest]. It sends an intent to the background download service, which then
     * instructs the [DownloadManager] to cancel the download (if in progress) and delete
     * its associated cached files from the disk.
     *
     * This is an individual action that targets one surah. For removing multiple surahs at once,
     * see [removeDownloads].
     *
     * @param context [Context] The application context, required to send the command to the service.
     * @param downloadRequest [DownloadRequest] The request object that uniquely identifies the download to be removed.
     *
     * @see removeDownloads
     * @see DownloadService.sendRemoveDownload
     */
    fun removeDownload(context: Context, downloadRequest: DownloadRequest) = downloadRequest.run {
        val serializedData = downloadRequest.serializedData
        Timber.debug("Removing download '$id': (${serializedData.surah.id}: ${serializedData.surah.name})...")

        sendRemoveDownload(
                context,
                QuranDownloadService::class.java,
                downloadRequest.id,
                true
        )
    }

    /**
     * Creates a [DownloadState] object from a [Download] instance and notifies all registered observers.
     *
     * This function is a central part of the download monitoring process. It's called periodically
     * to transform the low-level [Download] object provided by the Media3 [DownloadManager] into a
     * more user-friendly [DownloadState] data class. This includes converting raw integer codes for
     * state and failure reasons into type-safe enums, and coercing progress values to ensure they
     * are within valid ranges.
     *
     * After creating the [DownloadState], it logs the state for debugging purposes and then calls
     * [notifyDownloadStatusObservers] to broadcast the update to all subscribed components. A small
     * delay is introduced using [UPDATE_PROGRESS_INTERVAL] to throttle the rate of updates, preventing
     * excessive processing and UI redraws.
     *
     * @receiver [Download] The Media3 [Download] object containing the latest status information.
     *
     * @see DownloadState
     * @see notifyDownloadStatusObservers
     * @see QuranDownloadService.startDownloadMonitor
     */
    internal suspend fun updateDownloadState(download: Download) = download.run {
        val state = DownloadState(
                data = request.serializedData,
                state = state.asDownloadState,
                percentage = percentDownloaded.coerceIn(0f, 100f),
                downloaded = bytesDownloaded.coerceIn(0L, Long.MAX_VALUE),
                total = contentLength.coerceIn(0L, Long.MAX_VALUE),
                failureReason = failureReason.asFailureReason
        )

        Timber.debug("$state")

        notifyDownloadStatusObservers(state = state)
        delay(UPDATE_PROGRESS_INTERVAL.milliseconds)
    }

    /**
     * Notifies all registered [DownloadStatusObserver]s about a change in a download's state.
     *
     * This function iterates through the [downloadStatusObservers] list and invokes the
     * [DownloadStatusObserver.onDownloadStateChanged] method for each observer, passing the latest [DownloadState].
     * This ensures that all subscribed components receive real-time updates on download
     * progress, completion, or failure.
     *
     * @param state [DownloadState] The [DownloadState] object containing the updated status
     *   of a download, which will be broadcast to all observers.
     *
     * @see DownloadStatusObserver
     * @see downloadStatusObservers
     */
    internal suspend fun notifyDownloadStatusObservers(state: DownloadState) = downloadStatusObservers.forEach { observer ->
        observer.onDownloadStateChanged(state)
    }
}
