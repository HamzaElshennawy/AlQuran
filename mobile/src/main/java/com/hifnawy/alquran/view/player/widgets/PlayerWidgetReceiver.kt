package com.hifnawy.alquran.view.player.widgets

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.debug
import timber.log.Timber

/**
 * A [GlanceAppWidgetReceiver] for the [PlayerWidget].
 *
 * This class handles the lifecycle events of the app widget, such as updates, enabling,
 * and disabling. It serves as the entry point for the system to interact with the widget.
 * It's responsible for creating and managing instances of [PlayerWidget].
 *
 * @property glanceAppWidget [GlanceAppWidget] The [GlanceAppWidget] instance to be used by the receiver.
 *
 * @author AbdElMoniem ElHifnawy
 *
 * @see PlayerWidget
 * @see GlanceAppWidgetReceiver
 */
class PlayerWidgetReceiver(override val glanceAppWidget: GlanceAppWidget = PlayerWidget()) : GlanceAppWidgetReceiver() {

    /**
     * Called when an instance of the AppWidget is added to the home screen for the first time.
     * This is a good place to perform one-time setup.
     *
     * @param context [Context] The [Context] in which this receiver is running.
     */
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Timber.debug("onEnabled called. Widget added to home screen!")
    }

    /**
     * Called when the widget's options are changed, for example, when the widget is resized.
     * This triggers an update to the widget to reflect any new size constraints.
     *
     * @param context [Context] The [Context] in which this receiver is running.
     * @param appWidgetManager [AppWidgetManager] A manager for updating AppWidget views.
     * @param appWidgetId [Int] The ID of the app widget whose options have changed.
     * @param newOptions [Bundle] The new options for this app widget.
     */
    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        Timber.debug("onAppWidgetOptionsChanged called with appWidgetId: $appWidgetId!")

        PlayerWidgetManager.onAppWidgetOptionsChanged(context = context, appWidgetManager = appWidgetManager, appWidgetId = appWidgetId, newOptions = newOptions)
    }

    /**
     * Called to update the AppWidget at regular intervals defined by the `updatePeriodMillis`
     * in the AppWidget provider info. This method is also called when the user adds the widget,
     * so it needs to perform the initial setup for the widget.
     *
     * This implementation delegates the update logic to [PlayerWidgetManager.onUpdate].
     *
     * @param context [Context] The [Context] in which this receiver is running.
     * @param appWidgetManager [AppWidgetManager] An [AppWidgetManager] for updating AppWidget views.
     * @param appWidgetIds [IntArray] The IDs of the app widget instances to update.
     */
    @SuppressLint("RestrictedApi")
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Timber.debug("onUpdate called for widget IDs: ${appWidgetIds.contentToString()}!")

        PlayerWidgetManager.onUpdate(context = context, appWidgetManager = appWidgetManager, appWidgetIds = appWidgetIds)
    }

    /**
     * Called when the last instance of the AppWidget is removed from the AppWidgetHost.
     * This method is the counterpart to [onEnabled] and is triggered when the user
     * removes the last widget from their home screen.
     *
     * @param context [Context] The [Context] in which this receiver is running.
     */
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Timber.debug("onDisabled called. Last widget instance removed from home screen!")
    }
}
