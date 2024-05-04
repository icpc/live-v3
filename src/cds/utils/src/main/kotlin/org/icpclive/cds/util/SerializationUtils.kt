package org.icpclive.cds.util

import kotlinx.serialization.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModuleBuilder

public inline fun <reified T: Any> SerializersModuleBuilder.postProcess(
    crossinline onDeserialize: (T) -> T = { it },
    crossinline onSerialize: (T) -> T = { it },
) {
    postProcess(serializer<T>(), onDeserialize, onSerialize)
}

public inline fun <reified T: Any, reified S: Any> SerializersModuleBuilder.postProcess(
    serializer: KSerializer<S> = serializer<S>(),
    crossinline onDeserialize: (S) -> T,
    crossinline onSerialize: (T) -> S,
) {
    contextual(T::class, object : KSerializer<T> {
        private val delegate = serializer
        override val descriptor get() = delegate.descriptor
        override fun deserialize(decoder: Decoder) = onDeserialize(delegate.deserialize(decoder))
        override fun serialize(encoder: Encoder, value: T) = delegate.serialize(encoder, onSerialize(value))
    })
}