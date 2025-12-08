package com.hifnawy.alquran.shared.utils

/**
 * An object that serves as a namespace for a collection of extension properties for numeric types.
 *
 * This object groups extensions that provide convenient and readable syntax for various calculations,
 * such as converting between different data size units (bytes, kilobytes, megabytes, etc.).
 *
 * By importing the members of this object, you can write more expressive code.
 *
 * Example usage for data size conversion:
 * ```kotlin
 * import com.hifnawy.alquran.shared.utils.NumberExt.MB
 *
 * val fileSizeInBytes = 5.MB
 * val cacheSizeInBytes = 256.KB
 * val diskSpaceInBytes = 2.GB
 * ```
 *
 * @author AbdElMoniem ElHifnawy
 */
object NumberExt {

    /**
     * Converts the given value to kilobytes (KB).
     *
     * @receiver [Int] The value to convert.
     *
     * @return [Int] The value in kilobytes (KB).
     */
    inline val Int.KB get() = this * 1024

    /**
     * Converts the given value to megabytes (MB).
     *
     * @receiver [Int] The value to convert.
     *
     * @return [Int] The value in megabytes (MB).
     */
    inline val Int.MB get() = this * 1024 * 1024

    /**
     * Converts the given value to gigabytes (GB).
     *
     * @receiver [Int] The value to convert.
     *
     * @return [Int] The value in gigabytes (GB).
     */
    inline val Int.GB get() = this * 1024 * 1024 * 1024

    /**
     * Converts the given value to kilobytes (KB).
     *
     * @receiver [Long] The value to convert.
     *
     * @return [Long] The value in kilobytes (KB).
     */
    inline val Long.KB get() = this * 1024L

    /**
     * Converts the given value to megabytes (MB).
     *
     * @receiver [Long] The value to convert.
     *
     * @return [Long] The value in megabytes (MB).
     */
    inline val Long.MB get() = this * 1024L * 1024L

    /**
     * Converts the given value to gigabytes (GB).
     *
     * @receiver [Long] The value to convert.
     *
     * @return [Long] The value in gigabytes (GB).
     */
    inline val Long.GB get() = this * 1024L * 1024L * 1024L

    /**
     * Converts the given value to kilobytes (KB).
     *
     * @receiver [Float] The value to convert.
     *
     * @return [Float] The value in kilobytes (KB).
     */
    inline val Float.KB get() = this * 1024f

    /**
     * Converts the given value to megabytes (MB).
     *
     * @receiver [Float] The value to convert.
     *
     * @return [Float] The value in megabytes (MB).
     */
    inline val Float.MB get() = this * 1024f * 1024f

    /**
     * Converts the given value to gigabytes (GB).
     *
     * @receiver [Float] The value to convert.
     *
     * @return [Float] The value in gigabytes (GB).
     */
    inline val Float.GB get() = this * 1024f * 1024f * 1024f

    /**
     * Converts the given value to kilobytes (KB).
     *
     * @receiver [Double] The value to convert.
     *
     * @return [Double] The value in kilobytes (KB).
     */
    inline val Double.KB get() = this * 1024.0

    /**
     * Converts the given value to megabytes (MB).
     *
     * @receiver [Double] The value to convert.
     *
     * @return [Double] The value in megabytes (MB).
     */
    inline val Double.MB get() = this * 1024.0 * 1024.0

    /**
     * Converts the given value to gigabytes (GB).
     *
     * @receiver [Double] The value to convert.
     *
     * @return [Double] The value in gigabytes (GB).
     */
    inline val Double.GB get() = this * 1024.0 * 1024.0 * 1024.0
}