package com.hifnawy.alquran.shared.domain

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadProgress
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService.sendAddDownload
import androidx.media3.exoplayer.offline.DownloadService.sendRemoveDownload
import androidx.media3.exoplayer.offline.DownloadService.sendSetStopReason
import com.google.gson.Gson
import com.hifnawy.alquran.shared.domain.QuranCacheDataSource.CacheKey.Companion.asCacheKey
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

@UnstableApi
object QuranDownloadManager {

    val downloadStatusObservers = mutableListOf<DownloadStatusObserver>()

    internal const val UPDATE_PROGRESS_INTERVAL = 100L

    internal val ioCoroutineScope by lazy { CoroutineScope(Dispatchers.IO + Job()) }
    internal val mainCoroutineScope by lazy { CoroutineScope(Dispatchers.Main + Job()) }
    internal val completedDownloads = mutableMapOf<DownloadRequestId, MutableList<Download>>()

    internal val ByteArray.asUTF8String get() = String(this@asUTF8String, Charsets.UTF_8)

    internal val DownloadRequestData.cacheKey
        get() = "reciter_#${reciter.id.value}_moshaf_#${moshaf.id}_surah_#${surah.id.toString().padStart(3, '0')}".asCacheKey

    internal val DownloadRequestData.downloadRequest
        get() = surah.uri?.toUri()?.let { uri ->
            DownloadRequest.Builder(cacheKey.value, uri).run {
                setCustomCacheKey(cacheKey.value)
                setData(asJsonString.toByteArray(Charsets.UTF_8))
                build()
            }
        }

    internal val DownloadRequest.serializedData get() = Gson().fromJson(data.asUTF8String, DownloadRequestData::class.java)

    internal val Int.asDownloadState get() = DownloadState.State.fromCode(this)

    context(downloadRequest: DownloadRequest)
    internal val File.completedDownload
        get() = Download(
                downloadRequest,
                DownloadState.State.COMPLETED.code,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                length(),
                DownloadState.State.STOPPED.code,
                Download.FAILURE_REASON_NONE,
                DownloadProgress().apply {
                    bytesDownloaded = length()
                    percentDownloaded = 100f
                }
        )

    context(context: Context)
    internal val DownloadRequest.downloadPath
        get() = serializedData.let { (reciter, moshaf, surah) ->
            val surahNum = surah.id.toString().padStart(3, '0')
            val baseDestination = File(context.filesDir, "downloads")

            File(baseDestination, "reciter_#${reciter.id.value}/moshaf_#${moshaf.id}/surah_#$surahNum.mp3")
        }

    internal var downloadJob: Job? = null
    internal var downloadMonitorJob: Job? = null
    internal var downloadManagerInstance: DownloadManager? = null

    fun interface DownloadStatusObserver : IObservable {

        suspend fun onDownloadStateChanged(downloadState: DownloadState)
    }

    data class BulkDownloadRequest(val reciter: Reciter, val moshaf: Moshaf, val surahs: List<Surah>) : Serializable {

        override fun toString() = "${this::class.java.simpleName}(" +
                                  "${::reciter.name}=(${reciter.id.value}: ${reciter.name}), " +
                                  "${::moshaf.name}=(${moshaf.id}: ${moshaf.name}), " +
                                  "${::surahs.name}=<${surahs.size}>" +
                                  surahs.joinToString(prefix = "[", postfix = "]", separator = ", ") { "(${it.id}: ${it.name})" } +
                                  ")"
    }

    data class DownloadState(
            val data: DownloadRequestData = DownloadRequestData(),
            val state: State = State.STOPPED,
            val percentage: Float = 0f,
            val downloaded: Long = 0L,
            val total: Long = 0L,
            val failureReason: FailureReason = FailureReason.fromCode(Download.FAILURE_REASON_NONE)
    ) : Serializable {

        enum class State(val code: Int) : Serializable {
            QUEUED(Download.STATE_QUEUED),
            STOPPED(Download.STATE_STOPPED),
            COMPLETED(Download.STATE_COMPLETED),
            FAILED(Download.STATE_FAILED),
            DOWNLOADING(Download.STATE_DOWNLOADING),
            REMOVING(Download.STATE_REMOVING);

            companion object {

                fun fromCode(code: Int) = entries.first { it.code == code }
            }
        }

        enum class FailureReason(val code: Int) : Serializable {
            NONE(Download.FAILURE_REASON_NONE),
            UNKNOWN(Download.FAILURE_REASON_UNKNOWN);

            companion object {

                fun fromCode(code: Int) = entries.first { it.code == code }
            }
        }

        override fun toString() = "${this::class.java.simpleName}(" +
                                  "${::data.name}=${data}, " +
                                  "${::state.name}=${state.name}, " +
                                  "${::percentage.name}=${String.format(Locale.ENGLISH, "%06.2f", percentage)}%, " +
                                  "${::downloaded.name}=${downloaded.asHumanReadableSize}, " +
                                  "${::total.name}=${total.asHumanReadableSize}, " +
                                  "${::failureReason.name}=${failureReason.name}" +
                                  ")"
    }

    data class DownloadRequestData(val reciter: Reciter = Reciter(), val moshaf: Moshaf = Moshaf(), val surah: Surah = Surah()) : Serializable {

        override fun toString() = "${this::class.java.simpleName}(" +
                                  "${::reciter.name}=(${reciter.id.value}: ${reciter.name}), " +
                                  "${::moshaf.name}=(${moshaf.id}: ${moshaf.name}), " +
                                  "${::surah.name}=(${surah.id}: ${surah.name})" +
                                  ")"
    }

    internal data class DownloadRequestId(val reciter: Reciter, val moshaf: Moshaf) : Serializable {

        override fun toString() = "${this::class.java.simpleName}(" +
                                  "${::reciter.name}=(${reciter.id.value}: ${reciter.name}), " +
                                  "${::moshaf.name}=(${moshaf.id}: ${moshaf.name})" +
                                  ")"
    }

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

                Timber.debug("Queued Download '${downloadRequest.id}': (${surah.id}: ${surah.name}) is already ${DownloadState.State.COMPLETED}!")

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

                Timber.debug("Resumed Download '$downloadRequestId': (${surah.id}: ${surah.name}) is already ${DownloadState.State.COMPLETED}!")

                updateDownloadState(download)
            }

            queuedDownloads.forEach { (downloadRequest, _) -> resumeDownload(context, downloadRequest) }

            Timber.debug("${queuedDownloads.size} Downloads Resumed!")
        }.also { downloadJob = it }.run { Timber.debug("Resuming ${queuedDownloads.size} Downloads...") }
    }

    fun pauseDownloads(context: Context, bulkDownloadRequest: BulkDownloadRequest) = bulkDownloadRequest.run {
        downloadJob?.cancel()
        downloadMonitorJob?.cancel()

        val downloadRequestId = DownloadRequestId(reciter, moshaf)

        ioCoroutineScope.launch {
            surahs.forEach { surah ->
                val download = downloadManagerInstance?.currentDownloads?.find { it.request.serializedData.surah.id == surah.id }

                download?.let {
                    Timber.debug("Download '$downloadRequestId': (${surah.id}: ${surah.name}) is ${it.state.asDownloadState}!")

                    pauseDownload(context, it.request)
                } ?: Timber.warn("Download '$downloadRequestId': (${surah.id}: ${surah.name}) is non-existent!!!")
            }
        }
    }

    fun removeDownloads(context: Context, bulkDownloadRequest: BulkDownloadRequest) = bulkDownloadRequest.run {
        downloadJob?.cancel()
        downloadMonitorJob?.cancel()

        val downloadRequestId = DownloadRequestId(reciter, moshaf)

        ioCoroutineScope.launch {
            surahs.forEach { surah ->
                val download = downloadManagerInstance?.currentDownloads?.find { it.request.serializedData.surah.id == surah.id }

                download?.let {
                    Timber.debug("Download '$downloadRequestId': (${surah.id}: ${surah.name}) is ${it.state.asDownloadState}!")

                    completedDownloads[downloadRequestId]?.remove(it)

                    removeDownload(context, it.request)
                } ?: Timber.warn("Download '$downloadRequestId': (${surah.id}: ${surah.name}) is non-existent!!!")
            }
        }
    }

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

    internal suspend fun updateDownloadState(download: Download) = download.run {
        val state = DownloadState(
                data = request.serializedData,
                state = state.asDownloadState,
                percentage = percentDownloaded.coerceIn(0f, 100f),
                downloaded = bytesDownloaded.coerceIn(0L, Long.MAX_VALUE),
                total = contentLength.coerceIn(0L, Long.MAX_VALUE),
                failureReason = DownloadState.FailureReason.fromCode(failureReason)
        )

        Timber.debug("$state")

        notifyDownloadStatusObservers(state = state)
        delay(UPDATE_PROGRESS_INTERVAL.milliseconds)
    }

    internal suspend fun notifyDownloadStatusObservers(state: DownloadState) = downloadStatusObservers.forEach { observer -> observer.onDownloadStateChanged(state) }
}
