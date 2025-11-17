package com.hifnawy.alquran.shared.utils

import java.util.Locale
import kotlin.time.Duration

/**
 * Provides extension functions for [Duration] that provide human-readable representations of the duration.
 *
 * @author AbdAlMoniem AlHifnawy
 */
object DurationExtensionFunctions {

    val Duration.hoursLong: Long get() = toComponents { hours, _, _, _ -> hours }
    val Duration.minutesInt: Int get() = toComponents { _, minutes, _, _ -> minutes }
    val Duration.secondsInt: Int get() = toComponents { _, _, seconds, _ -> seconds }
    val Duration.millisecondsInt: Int get() = toComponents { _, _, _, _, nanoseconds -> nanoseconds / 1_000_000 }
    val Duration.microsecondsInt: Int get() = toComponents { _, _, _, _, nanoseconds -> nanoseconds / 1_000 }
    val Duration.nanosecondsInt: Int get() = toComponents { _, _, _, _, nanoseconds -> nanoseconds }

    /**
     * Converts the duration to a formatted string suitable for display to the user.
     *
     * The format of the string is determined by the current locale. If the locale is a right-to-left locale, the format will be `HH:mm:ss`.
     * Otherwise, the format will be `HH MM SS`.
     *
     * @param hideLegend [Boolean] if `true`, the string will not include the `hour`, `minute`, or `second` labels.
     * @param showHours [Boolean] if `true`, the string will include the `hour` part of the time.
     *
     * @return [String] the formatted string.
     */
    fun Duration.toFormattedTime(hideLegend: Boolean = false, showHours: Boolean = true): String = toComponents { hours, minutes, seconds, _ ->
        val isHoursShown = showHours || hours > 0L

        when {
            isInfinite() -> "Ꝏ"
            else         -> {
                val format = when {
                    hideLegend -> if (!isHoursShown) "%02d:%02d" else "%02d:%02d:%02d"
                    else       -> if (!isHoursShown) "%02dm %02ds" else "%02dh %02dm %02ds"
                }

                when {
                    isHoursShown -> String.format(Locale.ENGLISH, format, hours, minutes, seconds)
                    else         -> String.format(Locale.ENGLISH, format, minutes, seconds)
                }
            }
        }
    }

    /**
     * Formats the duration in a localized string that is suitable for display to the user.
     *
     * The format of the string is determined by the current locale. If the locale is a right-to-left locale, the format will be `HH:mm:ss`.
     * Otherwise, the format will be `HH MM SS`.
     *
     * @param showHours [Boolean] if `true`, the string will include the `hour` part of the time.
     *
     * @return [String] the formatted string.
     */
    fun Duration.toLocalizedFormattedTime(showHours: Boolean = true): String = toComponents { hours, minutes, seconds, _ ->
        val isHoursShown = showHours || hours > 0L
        val locale = Locale.Builder().setLanguage("ar").setRegion("EG").build()

        when {
            isInfinite() -> "∞"
            else         -> {
                val format = when {
                    !isHoursShown -> "%02d:%02d"
                    else          -> "%02d:%02d:%02d"
                }

                when {
                    isHoursShown -> String.format(locale, format, hours, minutes, seconds)
                    else         -> String.format(locale, format, minutes, seconds)
                }
            }
        }
    }
}
