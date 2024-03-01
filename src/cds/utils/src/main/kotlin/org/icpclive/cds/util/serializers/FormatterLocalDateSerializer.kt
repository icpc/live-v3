package org.icpclive.cds.util.serializers

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

public abstract class FormatterLocalDateSerializer(private val formatter: DateTimeFormat<LocalDateTime>) : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(formatter.format(value))
    }
    override fun deserialize(decoder: Decoder): LocalDateTime {
        return formatter.parse(decoder.decodeString())
    }
}