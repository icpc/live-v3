package org.icpclive.util

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.UnsynchronizedAppenderBase
import ch.qos.logback.core.encoder.Encoder
import kotlinx.coroutines.runBlocking
import org.icpclive.data.DataBus

class FlowLogger<E : ILoggingEvent>(private val encoder: Encoder<E>) : UnsynchronizedAppenderBase<E>() {
    override fun append(eventObject: E) {
        val message = encoder.encode(eventObject)
        runBlocking {
            DataBus.loggerFlow.emit(String(message))
        }
    }
}
