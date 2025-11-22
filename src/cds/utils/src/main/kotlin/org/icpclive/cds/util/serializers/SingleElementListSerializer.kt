package org.icpclive.cds.util.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

public class SingleElementListSerializer<T>(private val element: KSerializer<T>) : KSerializer<List<T>> {
    override val descriptor: SerialDescriptor = element.descriptor
    override fun serialize(encoder: Encoder, value: List<T>) {
        if (value.size != 1) throw IllegalArgumentException("Expected single element, got $value")
        encoder.encodeSerializableValue(element, value[0])
    }

    override fun deserialize(decoder: Decoder): List<T> {
        return listOf(decoder.decodeSerializableValue(element))
    }
}