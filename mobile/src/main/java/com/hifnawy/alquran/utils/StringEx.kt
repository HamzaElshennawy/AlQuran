package com.hifnawy.alquran.utils

import com.hifnawy.alquran.utils.StringEx.snakeCase
import com.hifnawy.alquran.utils.StringEx.stripFormattingChars

/**
 * A singleton object that provides a collection of convenient extension functions and properties for the [String] class.
 *
 * This object acts as a namespace for utility functions that enhance the functionality of strings,
 * making common operations like case conversion or character stripping more concise and readable.
 * By centralizing these extensions here, we maintain a clean and organized codebase.
 *
 * ## Usage
 *
 * To use the extensions, simply import them and call them on any String instance:
 *
 * ```kotlin
 * import com.hifnawy.alquran.utils.StringEx.snakeCase
 * import com.hifnawy.alquran.utils.StringEx.stripFormattingChars
 *
 * val camelCaseString = "thisIsACamelCaseString123"
 * val snakeCaseString = camelCaseString.snakeCase // "this_is_a_camel_case_string_123"
 *
 * val formattedString = "\u00ADHello\u200B World\u0000"
 * val cleanString = formattedString.stripFormattingChars // "Hello World"
 * ```
 *
 * @see snakeCase
 * @see stripFormattingChars
 */
object StringEx {

    /**
     * Converts a `camelCase` or `PascalCase` string into `snake_case`.
     *
     * This extension property handles various capitalization patterns, including consecutive uppercase letters (like in acronyms)
     * and numbers followed by letters, correctly inserting underscores. The entire resulting string is converted to lowercase.
     *
     * ### Examples:
     * ```kotlin
     * import com.hifnawy.alquran.utils.StringEx.snakeCase
     *
     * val thisIsACamelCaseString123 = "thisIsACamelCaseString123".snakeCase
     * val thisIsCamelCase = "thisIsCamelCase".snakeCase
     * val PascalCase = "PascalCase".snakeCase
     * val aNumber123FollowedByText = "aNumber123FollowedByText".snakeCase
     * val HTTPRequest = "HTTPRequest".snakeCase
     *
     * println(thisIsACamelCaseString123) // "this_is_a_camel_case_string_123"
     * println(thisIsCamelCase) // "this_is_camel_case"
     * println(PascalCase) // "pascal_case"
     * println(aNumber123FollowedByText) // "a_number_123_followed_by_text"
     * println(HTTPRequest) // "http_request"
     * ```
     *
     * @return [String] A `snake_case` version of the original string.
     */
    val String.snakeCase
        get() = this
            .replace(Regex("([A-Z]+)([A-Z][a-z])"), "$1_$2")
            .replace(Regex("([a-z])([A-Z])"), "$1_$2")
            .replace(Regex("([0-9])([A-Z])"), "$1_$2")
            .lowercase()

    /**
     * Removes invisible formatting and control characters from a string.
     *
     * This extension property filters out characters that are typically not visible but can affect layout or processing,
     * such as control characters, format characters (like zero-width spaces), and ISO control characters. It helps in
     * sanitizing strings for display or comparison where such characters are undesirable.
     *
     * It specifically removes characters that satisfy any of the following conditions:
     * - `Char.isISOControl()` returns `true`.
     * - The character's category is `CharCategory.FORMAT`.
     * - The character's category is `CharCategory.CONTROL`.
     *
     * ### Examples:
     * ```kotlin
     * import com.hifnawy.alquran.utils.StringEx.stripFormattingChars
     *
     * val stringWithFormatting = "\u200BHello\u00AD World\u0000" // Contains zero-width space, soft hyphen, and null char
     * val cleanString = stringWithFormatting.stripFormattingChars
     *
     * println(cleanString) // "Hello World"
     * ```
     *
     * @return [String] A new string with all formatting and control characters removed.
     * @see Char.isISOControl
     * @see CharCategory.FORMAT
     * @see CharCategory.CONTROL
     */
    val String.stripFormattingChars: String
        get() = this.filter { char ->
            !char.isISOControl() && char.category != CharCategory.FORMAT && char.category != CharCategory.CONTROL
        }
}
