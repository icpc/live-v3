package org.icpclive.cds.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModuleBuilder
import kotlinx.serialization.serializer

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
    contextual(T::class, serializer.map(onDeserialize, onSerialize))
}

public inline fun <T, R> KSerializer<T>.map(
    crossinline onDeserialize: (T) -> R,
    crossinline onSerialize: (R) -> T,
): KSerializer<R> = object : KSerializer<R> {
    override val descriptor: SerialDescriptor
        get() = this@map.descriptor

    override fun deserialize(decoder: Decoder) = onDeserialize(this@map.deserialize(decoder))
    override fun serialize(encoder: Encoder, value: R) = this@map.serialize(encoder, onSerialize(value))
}

public inline fun <reified S, reified T : S> KSerializer<T>.asSuperClass(): KSerializer<S> = map(
    onDeserialize = { it },
    onSerialize = { it as T }
)
