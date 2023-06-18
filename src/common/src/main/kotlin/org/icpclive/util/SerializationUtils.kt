package org.icpclive.util

import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.awt.Color
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object DurationInMillisecondsSerializer : KSerializer<Duration> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DurationMs", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Duration) {
        encoder.encodeLong(value.inWholeMilliseconds)
    }

    override fun deserialize(decoder: Decoder): Duration {
        return decoder.decodeLong().milliseconds
    }
}

object DurationInSecondsSerializer : KSerializer<Duration> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DurationS", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Duration) {
        encoder.encodeLong(value.inWholeSeconds)
    }

    override fun deserialize(decoder: Decoder): Duration {
        return decoder.decodeLong().seconds
    }
}

object DurationInMinutesSerializer : KSerializer<Duration> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DurationM", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Duration) {
        encoder.encodeLong(value.inWholeMinutes)
    }

    override fun deserialize(decoder: Decoder): Duration {
        return decoder.decodeLong().minutes
    }
}


object UnixMillisecondsSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("InstantMs", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeLong(value.toEpochMilliseconds())
    }

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.fromEpochMilliseconds(decoder.decodeLong())
    }
}

object UnixSecondsSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("InstantS", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeLong(value.toEpochMilliseconds() / 1000)
    }

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.fromEpochMilliseconds(decoder.decodeLong() * 1000)
    }
}


object ColorSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Color", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Color) {
        encoder.encodeString("#%02x%02x%02x%02x".format(value.red, value.green, value.blue, value.alpha))
    }

    override fun deserialize(decoder: Decoder): Color {
        val data = decoder.decodeString()
        return try {
            if (data.startsWith("0x")) {
                return Color(data.toUInt(radix = 16).toInt(), data.length == 8)
            }
            val str = data.removePrefix("#")
            when (str.length) {
                8 -> Color(
                    str.substring(0, 2).toInt(radix = 16),
                    str.substring(2, 4).toInt(radix = 16),
                    str.substring(4, 6).toInt(radix = 16),
                    str.substring(6, 8).toInt(radix = 16),
                )

                6 -> Color(
                    str.substring(0, 2).toInt(radix = 16),
                    str.substring(2, 4).toInt(radix = 16),
                    str.substring(4, 6).toInt(radix = 16),
                )

                3 -> Color(
                    str[0].digitToInt(16) * 0x11,
                    str[1].digitToInt(16) * 0x11,
                    str[2].digitToInt(16) * 0x11,
                )

                else -> {
                    throw NumberFormatException()
                }
            }
        } catch (e: NumberFormatException) {
            getLogger(ColorSerializer::class).error("Failed to parse color from $data", e)
            Color.BLACK
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
fun defaultJsonSettings() = Json {
    encodeDefaults = true
    isLenient = true
    allowSpecialFloatingPointValues = true
    allowStructuredMapKeys = true
    prettyPrint = false
    useArrayPolymorphism = false
    explicitNulls = false
}