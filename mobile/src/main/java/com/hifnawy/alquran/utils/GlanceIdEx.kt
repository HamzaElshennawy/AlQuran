package com.hifnawy.alquran.utils

import androidx.glance.GlanceId

/**
 * A utility object that provides extension functions and properties for the [GlanceId] class.
 *
 * This object is used to encapsulate workarounds or helper methods related to [GlanceId]
 * that are not available in the public API of the Glance library. It centralizes potentially
 * fragile, reflection-based code to make it easier to manage and update if the underlying
 * library changes.
 *
 * @author AbdElMoniem ElHifnawy
 *
 * @see GlanceId
 */
object GlanceIdEx {

    /**
     * A private extension property on [GlanceId] to extract the underlying `appWidgetId`.
     *
     * This is a workaround using reflection to access the private `appWidgetId` field
     * within the [GlanceId] class. This is necessary because the `appWidgetId` is not
     * publicly exposed, but it's useful for logging and debugging purposes to identify
     * which specific widget instance is being updated or composed.
     *
     * **Warning:** This implementation relies on the internal structure of the `GlanceId`
     * class and may break in future versions of the Glance library if the field name
     * or class structure changes. Use with caution.
     *
     * @return [Int] The integer ID of the AppWidget associated with this [GlanceId].
     */
    val GlanceId.appWidgetId get() = this::class.java.getDeclaredField("appWidgetId").apply { isAccessible = true }.getInt(this)
}
