package org.icpclive.cds.util.serializers

import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

public object HumanTimeSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("InstantH", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(format(value))
    }

    private val zone = TimeZone.currentSystemDefault()

    private val readableFormat = DateTimeComponents.Format {
        year()
        char('-')
        monthNumber()
        char('-')
        dayOfMonth()
        char(' ')
        hour()
        char(':')
        minute()
        char(':')
        second()
        alternativeParsing({}) {
            char(' ')
        }
        alternativeParsing(
            { offset(UtcOffset.Formats.ISO) },
            { timeZoneId() }
        ) {
            offsetHours()
            alternativeParsing({}) {
                char(':')
                offsetMinutesOfHour()
                optional {
                    char(':')
                    offsetSecondsOfMinute()
                }
            }
        }
    }

    public fun format(value: Instant): String {
        return readableFormat.format {
            setDateTime(value.toLocalDateTime(zone))
            setOffset(zone.offsetAt(value))
        }
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
            ?: catchToNull { Instant.parse(time, readableFormat) }
            ?: guessDatetimeFormatLocal(time)?.toInstant(TimeZone.currentSystemDefault())
            ?: throw SerializationException("Failed to parse date: $time")
}