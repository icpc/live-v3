package org.icpclive.util

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.UnsynchronizedAppenderBase
import ch.qos.logback.core.encoder.Encoder
import kotlinx.coroutines.runBlocking
import org.icpclive.server.AdminDataBus

class FlowLogger<E : ILoggingEvent>(private val encoder: Encoder<E>) : UnsynchronizedAppenderBase<E>() {
    override fun append(eventObject: E) {
        val message = encoder.encode(eventObject)
        runBlocking {
            AdminDataBus.loggerFlow.emit(String(message))
        }
    }
}
