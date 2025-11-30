package com.hifnawy.alquran.view.player.widgets

import android.annotation.SuppressLint
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.widget.RemoteViews
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.GlanceId
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.runComposition
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.session.Session
import androidx.glance.session.SessionManagerScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.work.Operation
import androidx.work.WorkManager
import com.hifnawy.alquran.shared.domain.ServiceStatus
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.debug
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.error
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.warn
import com.hifnawy.alquran.utils.GlanceIdEx.appWidgetId
import com.hifnawy.alquran.utils.sampleReciters
import com.hifnawy.alquran.utils.sampleSurahs
import com.hifnawy.alquran.view.player.widgets.PlayerWidgetManager.updateGlanceWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * A singleton manager object responsible for handling the lifecycle and updates of [PlayerWidget] instances.
 *
 * This object centralizes the logic for updating Glance-based app widgets, addressing challenges
 * with the standard Glance update mechanism, which can be slow and prone to delays due to session
 * locking. [PlayerWidgetManager] provides methods to manually trigger widget recomposition and apply
 * updates directly via the [AppWidgetManager], ensuring timely and reliable UI refreshes.
 *
 * Key responsibilities include:
 * - Handling initial widget setup ([onUpdate]) and resizing events ([onAppWidgetOptionsChanged]).
 * - Providing a custom, faster update mechanism ([executeWidgetUpdates]) that bypasses Glance's session locks.
 * - Managing widget state by persisting and retrieving [PlayerWidgetState].
 * - Interacting with [WorkManager] to cancel default Glance update workers to take over control of the update flow.
 * - Synchronizing widget operations using coroutines and a [Mutex] to prevent race conditions and redundant updates.
 *
 * This manager is critical for the player feature, as it allows the app to reflect the current
 * media playback status (e.g., playing, paused, track info) on the home screen widgets in near
 * real-time.
 *
 * @author AbdElMoniem ElHifnawy
 *
 * @see PlayerWidget
 * @see PlayerWidgetState
 * @see PlayerWidgetStateDefinition
 * @see GlanceAppWidgetManager
 * @see GlanceAppWidget
 * @see AppWidgetManager
 * @see WorkManager
 */
object PlayerWidgetManager {

    /**
     * Represents the state of a single widget session being managed by [PlayerWidgetManager].
     *
     * This data class is used internally to track the lifecycle and update status of each
     * app widget instance. It helps in coordinating asynchronous operations and preventing
     * redundant or conflicting updates.
     *
     * @property appWidgetId [Int] The unique identifier for the app widget instance.
     * @property sessionKey [String] The key used by Glance to identify the session for this widget.
     * @property isCreated [Boolean] A flag indicating whether the Glance session for this widget has been
     *                     successfully created. This is used to ensure operations are not attempted
     *                     before the session is ready.
     * @property isUpdating [Boolean] A flag that acts as a lock to indicate whether an update operation is
     *                      currently in progress for this widget. This helps prevent race conditions
     *                      where multiple updates might be triggered simultaneously for the same widget.
     *
     * @author AbdElMoniem ElHifnawy
     *
     * @see PlayerWidgetManager
     */
    private data class WidgetSession(val appWidgetId: Int, val sessionKey: String, val isCreated: Boolean, val isUpdating: Boolean)

    /**
     * An extension property on [Int] that generates a unique session key for a given `appWidgetId`.
     *
     * This key is used internally by the Glance library to identify and manage the session
     * associated with a specific app widget instance. The format `"appWidget-$appWidgetId"`
     * matches the convention used by Glance's `AppWidgetId.toSessionKey()`, ensuring
     * compatibility when interacting with Glance's session management APIs.
     *
     * @return [String] The session key in the format `"appWidget-<appWidgetId>"`.
     *
     * @see SessionManagerScope.getSession
     * @see SessionManagerScope.closeSession
     * @see WorkManager.cancelUniqueWork
     */
    private val Int.sessionKey get() = "appWidget-$this"


    /**
     * An extension property on [Bundle] that formats its contents into a human-readable string.
     *
     * This property is primarily used for debugging purposes to log the options [Bundle] received in
     * [onAppWidgetOptionsChanged]. It iterates through the keys in the [Bundle] and formats known
     * `AppWidgetManager.OPTION_*` keys with their corresponding values, including special handling
     * for different Android versions (e.g., for `OPTION_APPWIDGET_SIZES`).
     *
     * The output is a string representation of the key-value pairs, which is useful for
     * understanding the size and configuration changes applied to a widget.
     *
     * @return [String] A formatted [String] representing the key-value pairs in the [Bundle].
     */
    private val Bundle.bundleOptions
        get() = keySet().sorted().map { key ->
            key to when (key) {
                AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
                AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,
                AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
                AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT        -> getInt(key)

                AppWidgetManager.OPTION_APPWIDGET_SIZES             -> when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelableArrayList(key, SizeF::class.java)
                    else                                                  -> @Suppress("DEPRECATION", "RemoveExplicitTypeArguments") getParcelableArrayList<SizeF>(key)
                }?.joinToString { "${it.width.toInt()}x${it.height.toInt()}" }

                AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY     -> getInt(key)
                AppWidgetManager.OPTION_APPWIDGET_RESTORE_COMPLETED -> getBoolean(key)

                else                                                -> "Unknown Value"
            }
        }.joinToString(prefix = "[", postfix = "]", separator = ", ") { (key, value) -> "($key: $value)" }

    /**
     * A [Mutex] to ensure thread-safe access to widget session data and to prevent race
     * conditions during widget update operations.
     *
     * This lock is used to synchronize access to the [widgetSessions] map and to ensure that
     * update logic for a specific widget instance is executed atomically, preventing multiple
     * update coroutines from running concurrently for the same widget.
     *
     * @return [Mutex] A [Mutex] used to synchronize access to the [widgetSessions] map.
     */
    private val mutex = Mutex()

    /**
     * A map that holds the current state of each active widget session, keyed by the widget's [appWidgetId].
     *
     * This map is crucial for managing the lifecycle and update status of individual widget instances.
     * It stores [WidgetSession] objects, which track whether a widget's Glance session has been
     * created and whether an update is currently in progress.
     *
     * This allows the [PlayerWidgetManager] to:
     * - Prevent multiple concurrent updates to the same widget, avoiding race conditions.
     * - Ensure that update operations are only attempted after the widget's session is fully initialized.
     * - Maintain a snapshot of the state for all managed widgets.
     *
     * Access to this map is synchronized using a [Mutex] to ensure thread safety during concurrent
     * operations.
     *
     * @return [MutableMap<Int, WidgetSession>][MutableMap] A [MutableMap] of [WidgetSession] objects, keyed by the widget's [appWidgetId].
     *
     * @see WidgetSession
     * @see Mutex
     */
    private var widgetSessions = mutableMapOf<Int, WidgetSession>()


    /**
     * A [CoroutineScope] for launching coroutines related to widget updates.
     *
     * This scope uses [Dispatchers.Default], making it suitable for offloading work from the main
     * thread, such as processing widget updates and recomposing UI via [runComposition]. It is
     * defined in the companion object to provide a single, shared scope for all widget-related
     * background tasks, ensuring that these operations do not block the UI thread and are
     * managed within a lifecycle-independent context.
     *
     * @return [CoroutineScope] A [CoroutineScope] for launching coroutines related to widget updates.
     */
    private val coroutineScope = CoroutineScope(Dispatchers.Default)


    /**
     * Called when the widget's options are changed, for example, when the widget is resized.
     *
     * This function is triggered by the Android system whenever a widget's size or other
     * layout options are modified by the user. It launches a coroutine to handle the update
     * asynchronously, preventing blocking of the main thread.
     *
     * Before proceeding with an update, it checks if the Glance session for the widget has been
     * fully created. If the session isn't ready, it logs a warning and aborts the update to
     * prevent errors that can occur when trying to update a widget that is not yet fully initialized.
     * Once the session is confirmed to be ready, it triggers a full update of the widget's content
     * by calling [updateWidget], ensuring the UI adapts to the new size constraints.
     *
     * @param context [Context] The [Context] in which the receiver is running.
     * @param appWidgetManager [AppWidgetManager] The [AppWidgetManager] instance for interacting with app widgets.
     * @param appWidgetId [Int] The ID of the app widget whose options have changed.
     * @param newOptions [Bundle] A [Bundle] containing the new options for this app widget, such as
     * new size constraints.
     */
    @Suppress("unused")
    fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        coroutineScope.launch {
            mutex.withLock {
                Timber.warn("widgetSessions[${appWidgetId}].isCreated = ${widgetSessions[appWidgetId]?.isCreated}")
                if (widgetSessions[appWidgetId]?.isCreated != true) {
                    Timber.warn("Waiting for session to be created for key: ${appWidgetId.sessionKey}...")
                    return@launch
                }
            }

            val options = newOptions.bundleOptions
            Timber.debug("onAppWidgetOptionsChanged called with appWidgetId: $appWidgetId with options: $options!")
            updateWidget(context = context, appWidgetId = appWidgetId)
        }
    }


    /**
     * Called by the [AppWidgetHost] to update the AppWidget in response to a broadcast.
     *
     * This method is also called when a new instance of the widget is added to the home screen. It handles
     * the initial setup for new widgets by taking control of the update process from the Glance library.
     *
     * Glance's default update mechanism can be slow and subject to long session locks, which often
     * results in a newly placed widget remaining blank for a significant period. To prevent this,
     * this function performs the following steps for each new widget ID:
     * - Waits for the Glance [Session] for the widget to be created.
     * - Immediately closes the session to prevent Glance from managing updates.
     * - Cancels the associated [WorkManager] unique work that Glance schedules for updates.
     * - Once the session and worker are terminated, it triggers a manual update via [updateWidget]
     *   to render the widget with a default or last-known state, ensuring the UI appears quickly.
     *
     * This approach effectively replaces Glance's slow, locked update cycle with a more responsive,
     * manually controlled one, providing a better user experience when adding widgets.
     *
     * During testing I found out that when a widget is first placed on the home screen, onUpdate is called
     * with appWidgetIds containing only one element, which is the ID of the newly added widget. Glance then
     * takes over and tries to update the widget with the new state. However, it is extremely slow and takes
     * a long time to update the widget. As a result, the widget remains blank until the user restarts the app.
     *
     * I also noticed in the logcat that the widget is updated several multiple times per second which I think
     * could be the cause of the blank widget since the [AppWidgetManager] won't have time to update the widget
     * before Glance tries to update it again. I think it is a bug in the Glance library.
     *
     * In logcat when the widget is added you'll find:
     *
     * ```
     * - updateAppWidgetIds, mPackageName: com.hifnawy.alquran.debug, appWidgetIds: [633], views:android.widget.RemoteViews@7fe892d
     * - updateAppWidgetIds, mPackageName: com.hifnawy.alquran.debug, appWidgetIds: [633], views:android.widget.RemoteViews@7fe892d
     * - updateAppWidgetIds, mPackageName: com.hifnawy.alquran.debug, appWidgetIds: [633], views:android.widget.RemoteViews@7fe892d
     * - updateAppWidgetIds, mPackageName: com.hifnawy.alquran.debug, appWidgetIds: [633], views:android.widget.RemoteViews@7fe892d
     * - updateAppWidgetIds, mPackageName: com.hifnawy.alquran.debug, appWidgetIds: [633], views:android.widget.RemoteViews@7fe892d
     * - updateAppWidgetIds, mPackageName: com.hifnawy.alquran.debug, appWidgetIds: [633], views:android.widget.RemoteViews@7fe892d
     * - updateAppWidgetIds, mPackageName: com.hifnawy.alquran.debug, appWidgetIds: [633], views:android.widget.RemoteViews@7fe892d
     * - updateAppWidgetIds, mPackageName: com.hifnawy.alquran.debug, appWidgetIds: [633], views:android.widget.RemoteViews@7fe892d
     * - updateAppWidgetIds, mPackageName: com.hifnawy.alquran.debug, appWidgetIds: [633], views:android.widget.RemoteViews@7fe892d
     * - updateAppWidgetIds, mPackageName: com.hifnawy.alquran.debug, appWidgetIds: [633], views:android.widget.RemoteViews@7fe892d
     * - updateAppWidgetIds, mPackageName: com.hifnawy.alquran.debug, appWidgetIds: [633], views:android.widget.RemoteViews@7fe892d
     * - updateAppWidgetIds, mPackageName: com.hifnawy.alquran.debug, appWidgetIds: [633], views:android.widget.RemoteViews@7fe892d
     * - updateAppWidgetIds, mPackageName: com.hifnawy.alquran.debug, appWidgetIds: [633], views:android.widget.RemoteViews@7fe892d
     * - updateAppWidgetIds, mPackageName: com.hifnawy.alquran.debug, appWidgetIds: [633], views:android.widget.RemoteViews@7fe892d
     * - updateAppWidgetIds, mPackageName: com.hifnawy.alquran.debug, appWidgetIds: [633], views:android.widget.RemoteViews@7fe892d
     * - updateAppWidgetIds, mPackageName: com.hifnawy.alquran.debug, appWidgetIds: [633], views:android.widget.RemoteViews@7fe892d
     * ...
     * ```
     *
     * which is an indicator that the widget is being updated multiple times per second.
     *
     * To mitigate this issue, we need to call [PlayerWidgetManager.updateGlanceWidgets] in the onUpdate method.
     * This will update the widget with the a default state and prevent it from remaining blank.
     *
     * But before we do that we have to stop the Glance library from spamming the [AppWidgetManager]
     * with update requests. To do that we need to close the session that hosts the update worker used by the Glance library
     * to update the widget.
     * Poking around the [GlanceAppWidget.deleted] method, I found:
     *
     * ```
     * val glanceId = AppWidgetId(appWidgetId)
     * getSessionManager(context).runWithLock { closeSession(glanceId.toSessionKey()) }
     *
     * // toSessionKey is defined in androidx.glance.appwidget.AppWidgetUtils which is simply
     * // defined as "appWidget-$appWidgetId"
     *
     * internal fun createUniqueRemoteUiName(appWidgetId: Int) = "appWidget-$appWidgetId"
     * internal fun AppWidgetId.toSessionKey() = createUniqueRemoteUiName(appWidgetId)
     * ```
     * so we use that key and pass it to [SessionManagerScope.closeSession] to terminate the session.
     * and then start our custom update function [updateGlanceWidgets].
     *
     * @param context [Context] The [Context] in which this receiver is running.
     * @param appWidgetManager [AppWidgetManager] A manager for updating AppWidget views.
     * @param appWidgetIds [IntArray] The IDs of the app widgets that need to be updated.
     *
     * @param context [Context] The [Context] in which this receiver is running.
     * @param appWidgetManager [AppWidgetManager] A manager for updating AppWidget views.
     * @param appWidgetIds [IntArray] The IDs of the app widgets that need to be updated.
     *
     * @see SessionManagerScope.closeSession
     * @see WorkManager.cancelUniqueWork
     * @see updateWidget
     */
    @SuppressLint("RestrictedApi")
    fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Timber.debug("onUpdate called for widget IDs: ${appWidgetIds.contentToString()}!")

        coroutineScope.launch {
            appWidgetIds.forEach { appWidgetId ->
                var session: Session?

                val sessionManager = PlayerWidget().getSessionManager(context)

                // the session manager is under lock and key 🤣 it is shared, so we can't keep holding the lock
                // while we wait, so we hold it, check if the session exists, if not, we release the lock and wait 50ms
                // for it to be created and then repeat.
                do {
                    session = sessionManager.runWithLock { getSession(appWidgetId.sessionKey) }
                    Timber.warn("Waiting for session to be created for key: ${appWidgetId.sessionKey}...")
                    delay(50)
                } while (session == null)

                mutex.withLock { widgetSessions[appWidgetId] = WidgetSession(appWidgetId = appWidgetId, sessionKey = appWidgetId.sessionKey, isCreated = true, isUpdating = false) }

                Timber.warn("Closing session for key: ${appWidgetId.sessionKey}...")
                sessionManager.runWithLock { closeSession(appWidgetId.sessionKey) }

                Timber.warn("Canceling all WorkManager work for key: ${appWidgetId.sessionKey}...")
                val operation = WorkManager.getInstance(context).cancelUniqueWork(appWidgetId.sessionKey)

                // widget updates must be on the main thread
                withContext(Dispatchers.Main) {
                    operation.state.observeOnce { state ->
                        Timber.warn("WorkManager work cancellation state: $state")

                        if (state is Operation.State.SUCCESS) {
                            Timber.warn("WorkManager work canceled successfully for key: ${appWidgetId.sessionKey}!")

                            updateWidget(context = context, appWidgetId = appWidgetId)
                        }
                    }
                }
            }
        }
    }

    /**
     * Updates a single Glance widget instance based on the provided [ServiceStatus].
     *
     * This function is a targeted version of [updateGlanceWidgets], designed to update a
     * specific widget identified by its [appWidgetId]. It orchestrates the entire update
     * process for that widget:
     *
     * - It first finds the corresponding [GlanceId] for the given [appWidgetId]. If the
     *   widget doesn't exist (e.g., it has been removed from the home screen), it logs an
     *   error and aborts.
     * - It only proceeds if the provided [status] is a [ServiceStatus.MediaInfo] subtype, as
     *   this contains the necessary data to render the widget UI. Other status types are ignored.
     * - It calls [tryUpdateWidgetState] to persist the new state to the widget's data store.
     *   If the new state is identical to the old state and [forceUpdate] is `false`, the
     *   function exits early to avoid unnecessary recomposition.
     * - If the state was updated (or if [forceUpdate] is `true`), it triggers a manual UI
     *   refresh by calling [executeWidgetUpdate]. This ensures the visual changes are
     *   reflected immediately on the home screen.
     *
     * @param context [Context] The application [Context] used to access widget managers and state.
     * @param appWidgetId [Int] The unique ID of the specific app widget instance to update.
     * @param status [ServiceStatus] The new status to be reflected in the widget's state and UI.
     *               Only updates for [ServiceStatus.MediaInfo] are processed.
     * @param forceUpdate [Boolean] If `true`, the widget will be recomposed and updated even if
     *                    its state hasn't changed. Defaults to `false`.
     *
     * @return [Job] A [Job] representing the asynchronous update operation, or `null` if no update was performed.
     *
     * @see tryUpdateWidgetState
     * @see executeWidgetUpdate
     * @see updateGlanceWidgets
     */
    suspend fun updateGlanceWidget(context: Context, appWidgetId: Int, status: ServiceStatus, forceUpdate: Boolean = false): Job? {
        val glanceId = getGlanceIds(context).find { it.appWidgetId == appWidgetId } ?: run {
            Timber.error("Failed to update state for appWidgetId $appWidgetId. Widget is not found!")
            return null
        }
        if (status !is ServiceStatus.MediaInfo) return null

        val isStateModified = tryUpdateWidgetState(context, glanceId, status)
        if (!isStateModified && !forceUpdate) return null

        val updateJob = executeWidgetUpdate(context = context, glanceId = glanceId)
        Timber.debug("Updated glance widget #$appWidgetId state to ${status.javaClass.simpleName}")

        return updateJob
    }

    /**
     * Updates all active Glance widget instances with a new [ServiceStatus].
     *
     * This function iterates through all existing [PlayerWidget] instances and updates their
     * state and UI to reflect the provided status. It is designed to be called when a global
     * state change occurs, such as a media playback event, that needs to be broadcast to all
     * widgets.
     *
     * Key behaviors:
     * - Retrieves a list of all active widget [GlanceId]s.
     * - Only processes updates if the provided [status] is a [ServiceStatus.MediaInfo] type, as
     *   this contains the necessary data for the UI. Other types are ignored.
     * - It calls [tryUpdateWidgetState] for each widget. To optimize performance, it tracks if
     *   ***any*** widget's state was actually modified.
     * - If at least one widget's state changed (or if [forceUpdate] is `true`), it triggers a
     *   bulk UI refresh for all widgets by calling [executeWidgetUpdates]. This avoids redundant
     *   recompositions if the new state is identical to the old one across all widgets.
     * - This bulk update approach is more efficient than updating each widget individually when
     *   the same status applies to all.
     *
     * @param context [Context] The application [Context] used to access widget managers and state.
     * @param status [ServiceStatus] The new status to be reflected in the widgets' state and UI.
     *               Only updates for [ServiceStatus.MediaInfo] are processed.
     * @param forceUpdate [Boolean] If `true`, all widgets will be recomposed and updated even if
     *                    their state hasn't changed. Defaults to `false`.
     *
     * @return [Job] A [Job] representing the asynchronous update operation, or `null` if no update was performed.
     *
     * @see tryUpdateWidgetState
     * @see executeWidgetUpdates
     * @see updateGlanceWidget
     */
    suspend fun updateGlanceWidgets(context: Context, status: ServiceStatus, forceUpdate: Boolean = false): Job? {
        val glanceIds = getGlanceIds(context)
        val appWidgetIds = glanceIds.map { it.appWidgetId }.joinToString(prefix = "[", postfix = "]", separator = ", ") { "#$it" }

        if (status !is ServiceStatus.MediaInfo) return null

        val isAnyStateModified = glanceIds.fold(false) { isStateModified, glanceId ->
            isStateModified or tryUpdateWidgetState(context, glanceId, status)
        }

        if (!isAnyStateModified && !forceUpdate) return null

        val updateJob = executeWidgetUpdates(context, glanceIds)
        Timber.debug("Updated ${glanceIds.size} glance widgets $appWidgetIds states to ${status.javaClass.simpleName}")

        return updateJob
    }

    /**
     * Retrieves the [GlanceId]s for all active [PlayerWidget] instances.
     *
     * This is a helper function that uses the [GlanceAppWidgetManager] to query for all
     * app widget IDs associated with the [PlayerWidget] class and wraps them in a list of
     * [GlanceId] objects. This list is essential for performing targeted or bulk updates
     * on all existing widgets.
     *
     * @param context [Context] The application context, needed to get an instance of [GlanceAppWidgetManager].
     *
     * @return [List< GlanceId >][List] A list of [GlanceId]s representing all currently placed player widgets.
     *                                  The list will be empty if no widgets are active.
     *
     * @see GlanceAppWidgetManager
     * @see GlanceId
     */
    private suspend fun getGlanceIds(context: Context) = GlanceAppWidgetManager(context).getGlanceIds(PlayerWidget::class.java)

    /**
     * Attempts to update the state of a specific Glance widget and reports whether the state changed.
     *
     * This function handles the persistence of a new [PlayerWidgetState]. It compares the new state,
     * derived from the provided [status], with the existing state stored for the widget.
     *
     * - If the new state is different from the old state, it updates the widget's state store
     *   via [updateAppWidgetState] and returns `true`.
     * - If the new state is identical to the old state, it does nothing and returns `false`.
     * - It includes error handling to catch [IllegalArgumentException], which can occur if an attempt
     *   is made to update a widget that has been deleted from the home screen. In such cases, it
     *   logs an error and returns `false`.
     *
     * This function is a key part of the optimization strategy to avoid unnecessary UI recompositions.
     * By returning whether the state was actually modified, callers can decide whether a full UI
     * refresh is needed.
     *
     * @param context [Context] The application context, needed to access the widget's state.
     * @param glanceId [GlanceId] The identifier for the specific widget instance to update.
     * @param status [ServiceStatus.MediaInfo] The new media information to be stored in the widget's state.
     *
     * @return [Boolean] `true` if the widget's state was changed, `false` otherwise.
     *
     * @see updateAppWidgetState
     * @see PlayerWidgetState
     * @see PlayerWidgetStateDefinition
     */
    private suspend fun tryUpdateWidgetState(context: Context, glanceId: GlanceId, status: ServiceStatus.MediaInfo) = try {
        val newState = PlayerWidgetState(reciter = status.reciter, moshaf = status.moshaf, surah = status.surah, status = status)
        var isCurrentStateModified = false
        updateAppWidgetState(context, PlayerWidgetStateDefinition, glanceId) { oldState ->
            isCurrentStateModified = oldState != newState

            when {
                isCurrentStateModified -> newState
                else                   -> oldState
            }
        }

        isCurrentStateModified
    } catch (_: IllegalArgumentException) {
        Timber.error("Failed to update glance widget #${glanceId.appWidgetId}, Widget is likely deleted!")
        false
    }

    /**
     * Triggers a manual update for a single Glance widget.
     *
     * This function serves as a wrapper around [updateWidgetRemoteViews], launching the update
     * process in a coroutine using the shared [coroutineScope]. It is a convenience method for
     * updating a single widget instance asynchronously without needing to manage the coroutine
     * lifecycle at the call site.
     *
     * It immediately returns a [Job] that represents the ongoing update operation, allowing the
     * caller to optionally wait for its completion.
     *
     * @param context [Context] The application [Context].
     * @param glanceId [GlanceId] The identifier for the specific widget to be updated.
     * @return [Job] A [Job] handle to the coroutine executing the update.
     *
     * @see updateWidgetRemoteViews
     * @see executeWidgetUpdates
     */
    private fun executeWidgetUpdate(context: Context, glanceId: GlanceId) = coroutineScope.launch { updateWidgetRemoteViews(context, glanceId) }


    /**
     * Executes a manual update for a list of Glance widgets.
     *
     * This function bypasses the standard [updateAll][GlanceAppWidget.updateAll] or [update][GlanceAppWidget.update] methods of [GlanceAppWidget],
     * which can be slow and subject to locking delays (a "session lock" that can last up to 50 seconds),
     * especially during frequent updates from a service.
     *
     * Instead, it manually recomposes each widget's UI to get a [RemoteViews] object and then
     * uses the [AppWidgetManager] to apply this [RemoteViews] object directly to the widget instance.
     * This results in a much faster and more reliable UI update.
     *
     * The process for each widget is:
     * - Launch a new coroutine to process widgets concurrently.
     * - Call [PlayerWidget.runComposition] to generate the [RemoteViews].
     * - Use [AppWidgetManager.updateAppWidget]
     *    to push the update to the specific widget on the home screen.
     * - Catches, and logs [IllegalStateException] which can be thrown by [runComposition] if the widget is not in a
     *    valid state to be composed, or is already in the middle of another update, preventing the app from crashing.
     *
     * If we look at the update code inside the Glance library, it talks about session and lock,
     * the logs will also say something similar.
     *
     * - Once the widget is updated from the app or a new widget is created, the worker session
     * (produced by Glance lib) is locked and released after ***45-50*** sec.!!!
     * - Any updates made during that ***45-50*** second interval are ignored, so for a successful update,
     * the app needs to wait for that time window to pass. You can confirm this time window by watching the
     * for the following in logcat:
     *
     * ```
     * Worker result SUCCESS for Work [ id=69cc3029-d1b1-4e9a-9084-108baa942a49, tags={ androidx.glance.session.SessionWorker } ]
     *```
     *
     * - I don't know much about session and locks but all this is true as per my testing.
     *
     * so in short, the [PlayerWidget.updateAll] or even [PlayerWidget.update] will be very slow to update the UI in time.
     *
     * so the best way I found was to update the widget manually using [AppWidgetManager.updateAppWidget], but the trick is to get
     * the remote view from the [PlayerWidget.runComposition] method and then pass it to the [AppWidgetManager.updateAppWidget] method.
     *
     * @param context [Context] The application [Context].
     * @param glanceIds [List< GlanceId >][List] A [List] of [GlanceId]s for the widgets that need to be updated.
     *
     * @return [Job] A [Job] representing the asynchronous update operation.
     *
     * @see updateWidgetRemoteViews
     * @see executeWidgetUpdate
     */
    private fun executeWidgetUpdates(context: Context, glanceIds: List<GlanceId>) = coroutineScope.launch {
        // PlayerWidget().updateAll(context)
        glanceIds.forEach { glanceId -> async(Dispatchers.Main) { updateWidgetRemoteViews(context = context, glanceId = glanceId) } }
    }

    /**
     * Composes a [RemoteViews] object for a given widget and applies it directly.
     *
     * This function manually triggers the composition process for a single widget instance by
     * calling [runComposition]. It then takes the resulting [RemoteViews] and
     * uses the standard [AppWidgetManager] to update the widget on the home screen.
     *
     * This direct update approach is a core part of the manager's strategy to bypass the slow,
     * lock-prone update mechanism of the Glance library, ensuring responsive UI changes.
     *
     * It includes error handling to catch [IllegalStateException], which can be thrown by
     * [runComposition] if the widget's session is not in a valid state (e.g., if it is
     * already being composed). This prevents the app from crashing during concurrent or
     * conflicting update attempts.
     *
     * @param context The application [Context].
     * @param glanceId The [GlanceId] of the widget to update.
     *
     * @see runComposition
     * @see AppWidgetManager.updateAppWidget
     * @see executeWidgetUpdates
     */
    @OptIn(ExperimentalGlanceApi::class)
    private suspend fun updateWidgetRemoteViews(context: Context, glanceId: GlanceId) = try {
        val remoteViews = PlayerWidget().runComposition(context, glanceId).first()
        AppWidgetManager.getInstance(context).updateAppWidget(glanceId.appWidgetId, remoteViews)
    } catch (_: IllegalStateException) {
        Timber.error("Failed to update glance widget #${glanceId.appWidgetId}, Widget is likely already being updated!")
    }

    /**
     * Observes a [LiveData] of [Operation.State] only until it reaches a terminal state
     * ([Operation.State.SUCCESS] or [Operation.State.FAILURE]), then removes the observer.
     *
     * This is a one-shot observer, useful for handling the result of a single asynchronous
     * operation, such as a [WorkManager] task, without leaving a lingering observer that
     * could cause memory leaks or receive unwanted subsequent updates.
     *
     * @param observer [(state: Operation.State) -> Unit][observer] A lambda function that will be invoked with the [Operation.State] value.
     *                 It is called for each state change until the operation completes.
     *
     * @see LiveData.observeForever
     * @see LiveData.removeObserver
     * @see Observer
     */
    private fun LiveData<Operation.State>.observeOnce(observer: (state: Operation.State) -> Unit) = observeForever(
            object : Observer<Operation.State> {
                override fun onChanged(value: Operation.State) {
                    observer(value)

                    if (value !is Operation.State.SUCCESS && value !is Operation.State.FAILURE) return
                    removeObserver(this)
                }
            }
    )


    /**
     * Updates a specific widget instance with either the last known state or a default sample state.
     *
     * This function fetches the current state of the widget. If the widget has a previously saved
     * status, it forces an update using that status. If the widget has no saved status (e.g., it's a
     * new instance or the data was cleared), it updates the widget with a random sample state to
     * provide a default view.
     *
     * The update process is executed within a coroutine on the [Dispatchers.Default] dispatcher.
     * A mutex is used to prevent concurrent updates to the same widget, ensuring that only one
     * update operation can run at a time for a given [appWidgetId].
     *
     * @param context [Context] The [Context] used to access widget state and perform updates.
     * @param appWidgetId [Int] The ID of the specific app widget instance to update.
     *
     * @return [Job] A [Job] representing the asynchronous update operation.
     *
     * @see Dispatchers.Default
     * @see mutex
     * @see widgetSessions
     */
    @SuppressLint("RestrictedApi")
    private fun updateWidget(context: Context, appWidgetId: Int) = coroutineScope.launch {
        mutex.withLock {
            Timber.warn("widgetSessions[$appWidgetId].isUpdating = ${widgetSessions[appWidgetId]?.isUpdating}")
            if (widgetSessions[appWidgetId]?.isUpdating == true) {
                Timber.warn("Widget #$appWidgetId is already being updated!")
                return@launch
            }

            widgetSessions[appWidgetId] = widgetSessions[appWidgetId]?.copy(isUpdating = true) ?: return@launch
        }

        val reciter = sampleReciters.random()
        val moshaf = reciter.moshafList.first()
        val surah = sampleSurahs.random()
        val status = ServiceStatus.Paused(
                reciter = sampleReciters.random(),
                moshaf = moshaf,
                surah = surah,
                durationMs = 0,
                currentPositionMs = 0,
                bufferedPositionMs = 0
        )

        // TODO: Load the last status from the data store
        val widgetState = PlayerWidget().getAppWidgetState<PlayerWidgetState>(context = context, glanceId = AppWidgetId(appWidgetId))
        val widgetStatus = widgetState.status ?: status

        Timber.warn("Updating glance widget #$appWidgetId with status: $widgetStatus...")
        val updateJob = updateGlanceWidget(context = context, appWidgetId = appWidgetId, status = widgetStatus, forceUpdate = widgetState.status != null)

        // wait for the update to finish
        updateJob?.join()
        mutex.withLock { widgetSessions[appWidgetId] = widgetSessions[appWidgetId]?.copy(isUpdating = false) ?: return@launch }
    }
}
