package org.icpclive.clics

import kotlinx.serialization.modules.SerializersModule
import org.icpclive.util.postProcess

interface IdEvent<T> {
    val id: String
    val data: T?
}

interface GlobalEvent<T> {
    val data: T?
}

private fun String.processIfUrl(block: (String)-> String) =
    if (startsWith("http://") || startsWith("https://"))
        this
    else
        block(this)

fun clicsEventsSerializersModule(
    mediaUrlPostprocessor: (String) -> String = { it },
) = SerializersModule {
   postProcess(onEncode = { it: org.icpclive.clics.v202207.Media -> it.copy(href = it.href.processIfUrl(mediaUrlPostprocessor)) })
   postProcess(onEncode = { it: org.icpclive.clics.v202003.Media -> it.copy(href = it.href.processIfUrl(mediaUrlPostprocessor)) })
}