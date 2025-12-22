package com.hifnawy.alquran.shared.utils

import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.critical
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.debug
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.error
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.info
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.shouldShowCaller
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.verbose
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.warn
import timber.log.Timber

/**
 * A custom implementation of [timber.log.Timber.DebugTree] for logging debug messages.
 *
 * This class is responsible for logging messages with a specified package name.
 * It overrides the default behavior of [timber.log.Timber.DebugTree] to include additional
 * information such as the calling method's class and method name.
 *
 * @constructor Creates a new instance of [LogDebugTree] with the specified package name.
 *
 * @author AbdAlMoniem AlHifnawy
 *
 * @see timber.log.Timber.DebugTree
 * @see StackTraceObject
 */
class LogDebugTree : Timber.DebugTree() {

    /**
     * A companion object for [LogDebugTree] that provides extension functions for [Timber.Forest].
     *
     * This object introduces a set of extension functions ([verbose], [debug], [info], [warn], [error], [critical])
     * for [Timber.Forest]. These functions allow developers to specify whether the caller's location
     * (class and method name) should be included in the log message on a per-call basis.
     *
     * It uses a [ThreadLocal] variable, [shouldShowCaller], to pass the `showCaller` flag
     * from the call site to the [log] method of the [LogDebugTree] instance without altering the
     * standard [Timber] API signatures. This ensures that the flag is managed correctly in a
     * multi-threaded environment.
     *
     * @see ThreadLocal
     * @see Timber.Forest
     * @see verbose
     * @see debug
     * @see info
     * @see warn
     * @see error
     * @see critical
     */
    companion object {

        /**
         * A [ThreadLocal] variable used to pass the `showCaller` flag from the extension functions
         * (e.g., [verbose], [debug]) to the [log] method.
         *
         * [ThreadLocal] ensures that the `showCaller` value is specific to the thread that initiated
         * the log call. This prevents race conditions or incorrect behavior in a multi-threaded
         * environment where multiple logs might be issued concurrently. Each thread gets its own
         * independent copy of the boolean flag.
         *
         * The value is set at the beginning of each logging extension function and removed at the end
         * to avoid memory leaks and ensure that the state doesn't persist for subsequent log calls
         * on the same thread.
         *
         * @see isShowCaller
         */
        private val shouldShowCaller = ThreadLocal<Boolean>()

        /**
         * Logs a [verbose] message, with an option to include the caller's class and method name.
         *
         * This is an extension function for [Timber.Forest] that simplifies logging [verbose] messages
         * while providing a mechanism to display the call site information. It uses a [ThreadLocal]
         * variable to pass the `showCaller` preference to the custom [LogDebugTree].
         *
         * @param message [String?][String] The message to be logged. Can be a format string.
         * @param args [Any?][Any] Arguments for the format string in `message`.
         * @param showCaller [Boolean] If `true`, the log message will be prefixed with the class and method name
         *   of the caller. Defaults to `false`.
         */
        fun Timber.Forest.verbose(
                message: String?,
                vararg args: Any?,
                showCaller: Boolean = false
        ) = shouldShowCaller.run {
            set(showCaller)

            v(message, *args)

            remove()
        }

        /**
         * Logs a [debug] message, with an option to include the caller's class and method name.
         *
         * This is an extension function for [Timber.Forest] that simplifies logging [debug] messages
         * while providing a mechanism to display the call site information. It uses a [ThreadLocal]
         * variable to pass the `showCaller` preference to the custom [LogDebugTree].
         *
         * @param message [String?][String] The message to be logged. Can be a format string.
         * @param args [Any?][Any] Arguments for the format string in `message`.
         * @param showCaller [Boolean] If `true`, the log message will be prefixed with the class and method name
         *   of the caller. Defaults to `false`.
         */
        fun Timber.Forest.debug(
                message: String?,
                vararg args: Any?,
                showCaller: Boolean = false
        ) = shouldShowCaller.run {
            set(showCaller)

            d(message, *args)

            remove()
        }

        /**
         * Logs a [info] message, with an option to include the caller's class and method name.
         *
         * This is an extension function for [Timber.Forest] that simplifies logging [info] messages
         * while providing a mechanism to display the call site information. It uses a [ThreadLocal]
         * variable to pass the `showCaller` preference to the custom [LogDebugTree].
         *
         * @param message [String?][String] The message to be logged. Can be a format string.
         * @param args [Any?][Any] Arguments for the format string in `message`.
         * @param showCaller [Boolean] If `true`, the log message will be prefixed with the class and method name
         *   of the caller. Defaults to `false`.
         */
        fun Timber.Forest.info(
                message: String?,
                vararg args: Any?,
                showCaller: Boolean = false
        ) = shouldShowCaller.run {
            set(showCaller)

            i(message, *args)

            remove()
        }

        /**
         * Logs a [warn] message, with an option to include the caller's class and method name.
         *
         * This is an extension function for [Timber.Forest] that simplifies logging [warn] messages
         * while providing a mechanism to display the call site information. It uses a [ThreadLocal]
         * variable to pass the `showCaller` preference to the custom [LogDebugTree].
         *
         * @param message [String?][String] The message to be logged. Can be a format string.
         * @param args [Any?][Any] Arguments for the format string in `message`.
         * @param showCaller [Boolean] If `true`, the log message will be prefixed with the class and method name
         *   of the caller. Defaults to `false`.
         */
        fun Timber.Forest.warn(
                message: String?,
                vararg args: Any?,
                showCaller: Boolean = false
        ) = shouldShowCaller.run {
            set(showCaller)

            w(message, *args)

            remove()
        }

        /**
         * Logs a [error] message, with an option to include the caller's class and method name.
         *
         * This is an extension function for [Timber.Forest] that simplifies logging [error] messages
         * while providing a mechanism to display the call site information. It uses a [ThreadLocal]
         * variable to pass the `showCaller` preference to the custom [LogDebugTree].
         *
         * @param message [String?][String] The message to be logged. Can be a format string.
         * @param args [Any?][Any] Arguments for the format string in `message`.
         * @param showCaller [Boolean] If `true`, the log message will be prefixed with the class and method name
         *   of the caller. Defaults to `false`.
         */
        fun Timber.Forest.error(
                message: String?,
                vararg args: Any?,
                showCaller: Boolean = false
        ) = shouldShowCaller.run {
            set(showCaller)

            e(message, *args)

            remove()
        }

        /**
         * Logs a [critical] message, with an option to include the caller's class and method name.
         *
         * This is an extension function for [Timber.Forest] that simplifies logging [critical] messages
         * while providing a mechanism to display the call site information. It uses a [ThreadLocal]
         * variable to pass the `showCaller` preference to the custom [LogDebugTree].
         *
         * @param message [String?][String] The message to be logged. Can be a format string.
         * @param args [Any?][Any] Arguments for the format string in `message`.
         * @param showCaller [Boolean] If `true`, the log message will be prefixed with the class and method name
         *   of the caller. Defaults to `false`.
         */
        fun Timber.Forest.critical(
                message: String?,
                vararg args: Any?,
                showCaller: Boolean = false
        ) = shouldShowCaller.run {
            set(showCaller)

            wtf(message, *args)

            remove()
        }
    }

    /**
     * A private property that retrieves the `showCaller` flag from the [shouldShowCaller]
     * [ThreadLocal] variable. This flag determines whether the caller's class and method name
     * should be included in the log message.
     *
     * @return `true` if the `showCaller` flag was explicitly set for the current thread's log call,
     *         `false` otherwise. It defaults to `false` if the [ThreadLocal] variable is not set.
     *
     * @see shouldShowCaller
     */
    private val isShowCaller: Boolean get() = shouldShowCaller.get() ?: false

    /**
     * A private computed property that inspects the call stack to find the original caller of the log function.
     *
     * It creates a [Throwable] to get the current stack trace and then iterates through it to find
     * the first stack frame that is not part of the [LogDebugTree] or [Timber] classes.
     * This ensures that the log tag points to the code that actually issued the log request,
     * rather than the logging infrastructure itself.
     *
     * @return [StackTraceObject] A [StackTraceObject] containing the class and method name of the caller.
     *   If the caller cannot be determined, it defaults to a [StackTraceObject]
     *   with `Unknown` values.
     *
     * @see StackTraceObject
     */
    private val callerObject: StackTraceObject
        get() = (Throwable().stackTrace.firstOrNull { element ->
            !element.className.startsWith(LogDebugTree::class.java.name) && !element.className.startsWith(Timber::class.java.name)
        } ?: StackTraceElement("Unknown", "Unknown", "Unknown.kt", 0)).run {
            StackTraceObject(className, methodName)
        }

    /**
     * Represents a single stack trace element from the thread's stack trace.
     *
     * This class is used to extract information about the calling method from the stack trace.
     * It provides the class name and method name of the calling method.
     *
     * @property className [String] the name of the class of the calling method.
     * @property methodName [String] the name of the calling method.
     *
     * @constructor Creates a new instance of [StackTraceObject].
     *
     * @author AbdAlMoniem AlHifnawy
     */
    private data class StackTraceObject(val className: String = "UnknownClass", val methodName: String = "UnknownMethod") {

        /**
         * Converts the [StackTraceObject] to a string representation in the format "className.methodName".
         *
         * @return [String] a string representation of the object.
         */
        override fun toString() = "${className.split(".").lastOrNull()}.$methodName"
            .replace("$", ".")
            .replace(Regex("\\.\\d+"), "")
    }

    /**
     * Logs a message with a [priority] and an optional [tag][timber.log.Timber.Forest.tag].
     *
     * This method is called by the various [log functions][timber.log.Timber.Forest.log] in [timber.log.Timber].
     * It logs the message to the console by writing to [System.out][System.out].
     * The message is prefixed with the [tag] and the [priority] is used to determine the color
     * of the output.
     *
     * @param priority [Int] the logging priority.
     * @param tag [String] the tag for the log message. If not provided, the tag defaults to the [timber.log.Timber.Forest.tag].
     * @param message [String] the log message.
     * @param t [Throwable] the throwable to log, if any.
     *
     * @author AbdAlMoniem AlHifnawy
     *
     * @see timber.log.Timber.Forest.tag
     * @see timber.log.Timber.Forest.log
     * @see System.out
     */
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) = when {
        isShowCaller -> "$callerObject: $message"
        else         -> message
    }.let {
        super.log(priority, callerObject.toString(), it, t)
    }
}
