package org.icpclive.utils

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.UnsynchronizedAppenderBase
import kotlinx.coroutines.runBlocking
import org.icpclive.data.DataBus
import kotlinx.datetime.Instant

class FlowLogger<E : ILoggingEvent> : UnsynchronizedAppenderBase<E>() {
    override fun append(eventObject: E) {
        val timestamp = Instant.fromEpochMilliseconds(eventObject.timeStamp)
        val message = "${timestamp.humanReadable} [${eventObject.threadName}] ${eventObject.level} ${eventObject.loggerName} - ${eventObject.message}"
        runBlocking {
            DataBus.loggerFlow.emit(message)
        }
    }
}