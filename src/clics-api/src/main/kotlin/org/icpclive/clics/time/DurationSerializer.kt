package org.icpclive.clics.time

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.icpclive.clics.time.formatClicsRelativeTime
import org.icpclive.clics.time.parseClicsRelativeTime
import kotlin.time.Duration

internal object DurationSerializer : KSerializer<Duration> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ClicsDuration", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Duration) {
        encoder.encodeString(formatClicsRelativeTime(value))
    }

    override fun deserialize(decoder: Decoder): Duration {
        return parseClicsRelativeTime(decoder.decodeString())
    }
}