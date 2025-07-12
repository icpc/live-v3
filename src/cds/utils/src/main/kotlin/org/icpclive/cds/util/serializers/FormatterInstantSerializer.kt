package org.icpclive.cds.util.serializers

import kotlinx.datetime.*
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Instant

public abstract class FormatterInstantSerializer(private val formatter: DateTimeFormat<DateTimeComponents>) : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.format(formatter, TimeZone.currentSystemDefault().offsetAt(value)))
    }
    override fun deserialize(decoder: Decoder): Instant {
        return formatter.parse(decoder.decodeString()).toInstantUsingOffset()
    }
}