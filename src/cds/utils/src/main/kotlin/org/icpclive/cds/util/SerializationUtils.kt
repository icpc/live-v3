package org.icpclive.cds.util

import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
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
    contextual(T::class, serializer.map(onDeserialize, onSerialize))
}

public inline fun <T, R> KSerializer<T>.map(
    crossinline onDeserialize: (T) -> R,
    crossinline onSerialize: (R) -> T,
): KSerializer<R> = object : DelegatedSerializer<R, T>(this) {
    override fun onDeserialize(value: T) = onDeserialize(value)
    override fun onSerialize(value: R) = onSerialize(value)
}

public abstract class DelegatedSerializer<T, D>(private val delegate: KSerializer<D>) : KSerializer<T> {
    override val descriptor: SerialDescriptor get() = delegate.descriptor
    protected abstract fun onDeserialize(value: D): T
    protected abstract fun onSerialize(value: T): D
    override fun deserialize(decoder: Decoder): T = onDeserialize(delegate.deserialize(decoder))
    override fun serialize(encoder: Encoder, value: T) {
        delegate.serialize(encoder, onSerialize(value))
    }
}

public inline fun <reified S, reified T : S> KSerializer<T>.asSuperClass(): KSerializer<S> = map(
    onDeserialize = { it },
    onSerialize = { it as T }
)

public class ListOrSingleElementSerializer<T>(elementSerializer: KSerializer<T>) : JsonTransformingSerializer<List<T>>(ListSerializer(elementSerializer)) {
    @OptIn(InternalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor("ListOrSingleElement", SerialKind.CONTEXTUAL, elementSerializer.descriptor) {
        element("list", listSerialDescriptor(elementSerializer.descriptor))
        element("element", elementSerializer.descriptor)
    }

    override fun transformSerialize(element: JsonElement): JsonElement =
        (element as? JsonArray)?.singleOrNull() ?: element

    override fun transformDeserialize(element: JsonElement): JsonElement =
        (element as? JsonArray) ?: JsonArray(listOf(element))
}

public class ListOrSingleOrNullElementSerializer<T>(elementSerializer: KSerializer<T>) : JsonTransformingSerializer<List<T>>(ListSerializer(elementSerializer)) {
    @OptIn(InternalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor("ListOrSingleElement", SerialKind.CONTEXTUAL, elementSerializer.descriptor) {
        element("list", listSerialDescriptor(elementSerializer.descriptor))
        element("element", elementSerializer.descriptor)
    }.nullable

    override fun transformSerialize(element: JsonElement): JsonElement {
        if (element !is JsonArray) throw SerializationException("Unexpect json node from list: ${element::class}")
        return element.singleOrNull() ?: element
    }

    override fun transformDeserialize(element: JsonElement): JsonElement {
        return when (element) {
            is JsonNull -> JsonArray(emptyList())
            is JsonArray -> element
            else -> JsonArray(listOf(element))
        }
    }
}
