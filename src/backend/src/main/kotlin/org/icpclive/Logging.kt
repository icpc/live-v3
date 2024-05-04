package org.icpclive.org.icpclive

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.FileAppender
import org.icpclive.Config
import org.icpclive.util.FlowLogger
import org.slf4j.LoggerFactory
import kotlin.io.path.absolutePathString

fun setupLogging(config: Config) {
    val logbackLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    val lc = LoggerFactory.getILoggerFactory() as LoggerContext
    val ple = PatternLayoutEncoder().apply {
        pattern = "%d{YYYY-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
        context = lc
        start()
    }

    logbackLogger.detachAndStopAllAppenders()
    if (!config.disableLogFile) {
        val fileAppender = FileAppender<ILoggingEvent>().apply {
            file = config.logFile.absolutePathString()
            isAppend = true
            encoder = ple
            context = lc
            start()
        }
        logbackLogger.addAppender(fileAppender)
    }

    if (!config.disableStdoutLogs) {
        val stdoutAppender = ConsoleAppender<ILoggingEvent>().apply {
            encoder = ple
            context = lc
            start()
        }
        logbackLogger.addAppender(stdoutAppender)
    }
    val flogLogger = FlowLogger<ILoggingEvent>(ple).apply {
        context = lc
        start()
    }
    logbackLogger.addAppender(flogLogger)
    logbackLogger.level = ch.qos.logback.classic.Level.toLevel(config.logLevel)
}