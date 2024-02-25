package org.icpclive.util

import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.lang.Exception
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

public object DurationInMillisecondsSerializer : KSerializer<Duration> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DurationMs", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Duration) {
        encoder.encodeLong(value.inWholeMilliseconds)
    }

    override fun deserialize(decoder: Decoder): Duration {
        return decoder.decodeLong().milliseconds
    }
}

public object DurationInSecondsSerializer : KSerializer<Duration> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DurationS", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Duration) {
        encoder.encodeLong(value.inWholeSeconds)
    }

    override fun deserialize(decoder: Decoder): Duration {
        return decoder.decodeLong().seconds
    }
}

public object DurationInMinutesSerializer : KSerializer<Duration> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DurationM", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Duration) {
        encoder.encodeLong(value.inWholeMinutes)
    }

    override fun deserialize(decoder: Decoder): Duration {
        return decoder.decodeLong().minutes
    }
}


public object UnixMillisecondsSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("InstantMs", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeLong(value.toEpochMilliseconds())
    }

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.fromEpochMilliseconds(decoder.decodeLong())
    }
}

public object UnixSecondsSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("InstantS", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeLong(value.toEpochMilliseconds() / 1000)
    }

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.fromEpochMilliseconds(decoder.decodeLong() * 1000)
    }
}

public object HumanTimeSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("InstantH", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.humanReadable)
    }

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

public val Instant.humanReadable: String
    get() = Date(this.toEpochMilliseconds()).toString()
