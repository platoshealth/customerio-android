package io.customer.sdk.util

import android.os.Environment
import android.util.Log
import io.customer.base.extenstions.DateFormat
import io.customer.base.extenstions.toString
import io.customer.sdk.CustomerIOConfig
import java.io.File
import java.io.FileWriter
import java.util.*

interface Logger {
    fun info(message: String)
    fun debug(message: String)
    fun error(message: String)
}

enum class CioLogLevel {
    NONE,
    ERROR,
    INFO,
    DEBUG;

    fun shouldLog(levelForMessage: CioLogLevel): Boolean {
        return when (this) {
            NONE -> false
            ERROR -> levelForMessage == ERROR
            INFO -> levelForMessage == ERROR || levelForMessage == INFO
            DEBUG -> true
        }
    }
}

class LogcatLogger(
    private val sdkConfig: CustomerIOConfig
) : Logger {

    private val tag = "[CIO]"

    override fun info(message: String) {
        runIfMeetsLogLevelCriteria(CioLogLevel.INFO) {
            log(Log.INFO, message)
        }
    }

    override fun debug(message: String) {
        runIfMeetsLogLevelCriteria(CioLogLevel.DEBUG) {
            log(Log.DEBUG, message)
        }
    }

    override fun error(message: String) {
        runIfMeetsLogLevelCriteria(CioLogLevel.ERROR) {
            log(Log.ERROR, message)
        }
    }

    private fun runIfMeetsLogLevelCriteria(levelForMessage: CioLogLevel, block: () -> Unit) {
        val shouldLog = sdkConfig.logLevel.shouldLog(levelForMessage)

        if (shouldLog) block()
    }

    private fun log(level: Int, message: String) {
        Log.println(level, tag, message)

        // Writing to external storage on Android has changed a lot over the many versions of the OS. Therefore, there are
        // many use cases where an exception can be thrown and crash the app. This code is not critical to the functionality
        // of the SDK so we wrap all of it's logic in a try/catch to prevent crashing the app.
        try {
            val target = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            // Create a new log file for each day to prevent creatig 1 file that is really long.
            val logFile = File(target, "customerio-sdk-logs-${Date().toString(DateFormat.DATE_NO_TIME)}.txt")

            FileWriter(logFile, true).apply {
                val logDateTimestamp = Date().toString(DateFormat.ISO8601_MILLISECONDS)
                val logLevelString = when (level) {
                    Log.INFO -> "I"
                    Log.DEBUG -> "D"
                    Log.ERROR -> "E"
                    else -> "?"
                }
                // The string that we write to the file is designed to look similar to Logcat's output.
                write("$logDateTimestamp $logLevelString: $message\n")
                close()
            }
        } catch (e: Throwable) {
            // No logging of this error because we don't want to spam customer's logs with the same error over and over.
        }
    }
}
