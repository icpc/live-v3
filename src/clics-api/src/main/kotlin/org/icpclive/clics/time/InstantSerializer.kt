package org.icpclive.clics.time

import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.icpclive.clics.time.formatClicsTime
import org.icpclive.clics.time.parseClicsTime

internal object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ClicsInstant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(formatClicsTime(value))
    }

    override fun deserialize(decoder: Decoder): Instant {
        return parseClicsTime(decoder.decodeString())
    }
}