package com.hifnawy.alquran.shared.utils

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

    companion object {

        private val shouldShowCaller = ThreadLocal<Boolean>()

        fun Timber.Forest.verbose(
                message: String?,
                vararg args: Any?,
                showCaller: Boolean = false
        ) {
            shouldShowCaller.set(showCaller)
            try {
                v(message, *args)
            } finally {
                shouldShowCaller.remove()
            }
        }

        fun Timber.Forest.debug(
                message: String?,
                vararg args: Any?,
                showCaller: Boolean = false
        ) {
            shouldShowCaller.set(showCaller)
            try {
                d(message, *args)
            } finally {
                shouldShowCaller.remove()
            }
        }

        fun Timber.Forest.info(
                message: String?,
                vararg args: Any?,
                showCaller: Boolean = false
        ) {
            shouldShowCaller.set(showCaller)
            try {
                i(message, *args)
            } finally {
                shouldShowCaller.remove()
            }
        }

        fun Timber.Forest.warn(
                message: String?,
                vararg args: Any?,
                showCaller: Boolean = false
        ) {
            shouldShowCaller.set(showCaller)
            try {
                w(message, *args)
            } finally {
                shouldShowCaller.remove()
            }
        }

        fun Timber.Forest.error(
                message: String?,
                vararg args: Any?,
                showCaller: Boolean = false
        ) {
            shouldShowCaller.set(showCaller)
            try {
                e(message, *args)
            } finally {
                shouldShowCaller.remove()
            }
        }

        fun Timber.Forest.critical(
                message: String?,
                vararg args: Any?,
                showCaller: Boolean = false
        ) {
            shouldShowCaller.set(showCaller)
            try {
                wtf(message, *args)
            } finally {
                shouldShowCaller.remove()
            }
        }
    }

    /**
     * Retrieves the showCaller flag from the ThreadLocal variable, defaulting to true.
     */
    private val isShowCallerSet: Boolean get() = shouldShowCaller.get() ?: false

    private val callerObject: StackTraceObject
        get() {
            val stackTrace = Throwable().stackTrace

            val logClassName = LogDebugTree::class.java.name
            val timberClassName = Timber::class.java.name

            val callerElement = stackTrace.firstOrNull { element ->
                val className = element.className
                !className.startsWith(logClassName) &&
                !className.startsWith(timberClassName)
            } ?: StackTraceElement("Unknown", "Unknown", "Unknown.kt", 0)

            return StackTraceObject(callerElement.className, callerElement.methodName)
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
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val showCaller = isShowCallerSet

        val logMessage = when {
            showCaller -> "$callerObject: $message"
            else       -> message
        }

        super.log(priority, callerObject.toString(), logMessage, t)
    }
}
