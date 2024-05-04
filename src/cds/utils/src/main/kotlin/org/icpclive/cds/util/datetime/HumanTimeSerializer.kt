package org.icpclive.cds.util.datetime

import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*

public object HumanTimeSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("InstantH", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(format(value))
    }

    public fun format(value: Instant): String = Date(value.toEpochMilliseconds()).toString()

    override fun deserialize(decoder: Decoder): Instant {
        return guessDatetimeFormat(decoder.decodeString())
    }

    private inline fun <reified T> catchToNull(f: () -> T) = try {
        f()
    } catch (e: Exception) {
        null
    }

    private fun guessDatetimeFormatLocal(time: String) =
        catchToNull { LocalDateTime.parse(time) } ?: catchToNull { LocalDateTime.parse(time.trim().replace(" ", "T")) }


    private fun guessDatetimeFormat(time: String): Instant =
        Clock.System.now().takeIf { time == "now" }
            ?: catchToNull { Instant.fromEpochMilliseconds(time.toLong() * 1000L) }
            ?: catchToNull { Instant.parse(time) }
            ?: catchToNull { Instant.parse(time.trim().replaceFirst(" ", "T").replace(" ", "")) }
            ?: guessDatetimeFormatLocal(time)?.toInstant(TimeZone.currentSystemDefault())
            ?: throw SerializationException("Failed to parse date: $time")
}