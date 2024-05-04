package org.icpclive.cds.util.serializers

import kotlinx.datetime.TimeZone
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

public object TimeZoneSerializer : KSerializer<TimeZone> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("TimeZone", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: TimeZone) {
        encoder.encodeString(value.id)
    }

    override fun deserialize(decoder: Decoder): TimeZone {
        return try {
            TimeZone.of(decoder.decodeString())
        } catch (e: IllegalArgumentException) {
            throw SerializationException(e.message)
        }
    }
}