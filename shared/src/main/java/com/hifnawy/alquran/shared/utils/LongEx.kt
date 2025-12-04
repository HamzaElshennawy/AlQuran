package com.hifnawy.alquran.shared.utils

import java.util.Locale

/**
 * A utility object that provides convenient extension properties for the [Long] data type.
 *
 * This object centralizes common transformations and formatting operations on [Long] values,
 * making the code more readable and reusable. It's designed to be a singleton,
 * ensuring that its extensions are globally accessible within the project scope
 * where it's imported.
 *
 * Current extensions include:
 *  - [asHumanReadableSize]: Converts a [Long] value, typically representing bytes,
 *    into a human-readable string format (e.g., `KB`, `MB`, `GB`).
 *
 * By scoping extensions within this object, we avoid polluting the global namespace
 * for the [Long] class, requiring an explicit import of the desired extension.
 * This practice enhances code clarity and prevents potential extension function conflicts.
 *
 * @author AbdElMoniem ElHifnawy
 */
object LongEx {
    /**
     * Converts a [Long] value representing a size in bytes into a human-readable string.
     *
     * This extension property formats the byte count into a more understandable format,
     * using kilobytes `KB`, megabytes `MB`, or gigabytes `GB` as appropriate.
     * The formatting is done with two decimal places for precision. If the size is less than
     * `1024 bytes`, it is simply returned with ` bytes` appended.
     *
     * - Values >= `1 GB` are formatted as `X.XX GB`.
     * - Values >= `1 MB` are formatted as `X.XX MB`.
     * - Values >= `1 KB` are formatted as `X.XX KB`.
     * - Values < `1 KB` are formatted as `X bytes`.
     *
     * The formatting uses [Locale.ENGLISH] to ensure a consistent decimal point (`.`).
     *
     * Example usage:
     * ```kotlin
     * import com.hifnawy.alquran.shared.utils.LongEx.asHumanReadableSize
     *
     * val fileSizeInBytes: Long = 10485760L // 10 MB
     * val formattedSize = fileSizeInBytes.asHumanReadableSize // "10.00 MB"
     * println(formattedSize)
     *
     * val smallFileSize: Long = 512L
     * val smallFormattedSize = smallFileSize.asHumanReadableSize // "512 bytes"
     * println(smallFormattedSize)
     * ```
     *
     * @receiver [Long] The [Long] value to be formatted.
     *
     * @return A [String] representing the size in a human-readable format (e.g., `1.50 MB`, `2.00 GB`, `512 bytes`).
     */
    val Long.asHumanReadableSize
        get() = when {
            this >= 1024 * 1024 * 1024 -> String.format(Locale.ENGLISH, "%.2f GB", this / (1024.0 * 1024.0 * 1024.0))
            this >= 1024 * 1024        -> String.format(Locale.ENGLISH, "%.2f MB", this / (1024.0 * 1024.0))
            this >= 1024               -> String.format(Locale.ENGLISH, "%.2f KB", this / 1024.0)
            else                       -> "$this bytes"
        }
}
