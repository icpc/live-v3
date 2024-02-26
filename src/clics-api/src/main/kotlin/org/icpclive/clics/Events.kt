package org.icpclive.clics

import kotlinx.serialization.modules.SerializersModule
import org.icpclive.util.postProcess

public interface IdEvent<T> {
    public val id: String
    public val data: T?
}

public interface GlobalEvent<T> {
    public val data: T?
}

private fun String.processIfUrl(block: (String)-> String) =
    if (startsWith("http://") || startsWith("https://"))
        this
    else
        block(this)

public fun clicsEventsSerializersModule(
    mediaUrlPostprocessor: (String) -> String = { it },
): SerializersModule = SerializersModule {
   postProcess(onDeserialize = { it: org.icpclive.clics.v202207.Media -> it.copy(href = it.href.processIfUrl(mediaUrlPostprocessor)) })
   postProcess(onDeserialize = { it: org.icpclive.clics.v202003.Media -> it.copy(href = it.href.processIfUrl(mediaUrlPostprocessor)) })
}