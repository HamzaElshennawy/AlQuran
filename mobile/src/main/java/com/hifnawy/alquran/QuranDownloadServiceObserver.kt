package com.hifnawy.alquran

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.hifnawy.alquran.shared.domain.QuranDownloadManager
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.DownloadState
import com.hifnawy.alquran.shared.domain.QuranDownloadManager.DownloadStatusObserver
import com.hifnawy.alquran.shared.domain.QuranDownloadService

/**
 * A Composable that observes the state of [QuranDownloadService] downloads.
 *
 * This function provides a declarative and lifecycle-aware way to subscribe to download status
 * updates from the [QuranDownloadManager] within a Jetpack Compose UI. It uses a [DisposableEffect]
 * to register the provided [observer] when the Composable enters the composition and automatically
 * unregisters it when it leaves, preventing memory leaks.
 *
 * When the download state changes (e.g., `starts`, `progresses`, `completes`, or `fails`), the [observer]'s
 * `onDownloadStateChanged` callback will be invoked with the new [DownloadState].
 *
 * **Example Usage:**
 * ```kotlin
 * @Composable
 * fun DownloadScreen() {
 *     var downloadState by remember { mutableStateOf(DownloadState()) }
 *
 *     QuranDownloadServiceObserver { newState ->
 *         // This block will run whenever the download state changes.
 *         downloadState = newState
 *     }
 *
 *     // UI that reacts to the downloadState, e.g., showing a progress bar.
 *     when (val state = downloadState) {
 *      is DownloadState.State.QUEUED      -> Text("Download queued.")
 *      is DownloadState.State.STOPPED     -> Text("Download stopped.")
 *      is DownloadState.State.COMPLETED   -> Text("Download complete!")
 *      is DownloadState.State.FAILED      -> Text("Error: ${state.failureReason}")
 *
 *      is DownloadState.State.DOWNLOADING -> {
 *          Text("Downloading: ${state.percentage}%")
 *          LinearProgressIndicator(progress = state.percentage / 100f)
 *      }
 *
 *      is DownloadState.State.REMOVING    -> Text("Removing download...")
 * }
 * ```
 *
 * @param observer [DownloadStatusObserver] A [DownloadStatusObserver] lambda that will be invoked with the new [DownloadState]
 *   whenever the download status changes.
 */
@Composable
@SuppressLint("UnsafeOptInUsageError")
fun QuranDownloadServiceObserver(observer: DownloadStatusObserver) {
    val quranDownloadManager = remember { QuranDownloadManager }

    DisposableEffect(quranDownloadManager) {
        quranDownloadManager.downloadStatusObservers.add(observer)

        onDispose { quranDownloadManager.downloadStatusObservers.remove(observer) }
    }
}
