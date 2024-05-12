package org.icpclive.server

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.FileAppender
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.path
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class LoggingOptions(logfileDefaultPrefix: String) : OptionGroup("logging settings") {
    private val logFile by option(
        "--log-file",
        help = "File to print logs"
    ).path(canBeFile = true, canBeDir = false, mustExist = false)
        .default(Path.of("${logfileDefaultPrefix}.log"), "./${logfileDefaultPrefix}.log")

    private val disableLogFile by option("--disable-log-file", help = "Don't print logs to any file").flag()
    private val disableStdoutLogs by option("--disable-stdout-logs", help = "Don't print logs to stdout").flag()
    private val logLevel by option("--log-level", help = "level of logs to write")
        .choice("ALL", "TRACE", "DEBUG", "INFO", "WARN", "ERROR", "OFF")
        .default("INFO")


    fun setupLogging(
        extraLoggers: List<(PatternLayoutEncoder) -> Appender<ILoggingEvent>> = emptyList()
    ) {
        val logbackLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        val lc = LoggerFactory.getILoggerFactory() as LoggerContext
        val ple = PatternLayoutEncoder().apply {
            pattern = "%d{YYYY-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
            context = lc
            start()
        }

        logbackLogger.detachAndStopAllAppenders()
        if (!disableLogFile) {
            val fileAppender = FileAppender<ILoggingEvent>().apply {
                file = logFile.absolutePathString()
                isAppend = true
                encoder = ple
                context = lc
                start()
            }
            logbackLogger.addAppender(fileAppender)
        }

        if (!disableStdoutLogs) {
            val stdoutAppender = ConsoleAppender<ILoggingEvent>().apply {
                encoder = ple
                context = lc
                start()
            }
            logbackLogger.addAppender(stdoutAppender)
        }
        for (extraLogger in extraLoggers) {
            val extra = extraLogger(ple).apply {
                context = lc
                start()
            }
            logbackLogger.addAppender(extra)
        }
        logbackLogger.level = ch.qos.logback.classic.Level.toLevel(logLevel)
    }
}