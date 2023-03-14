package org.icpclive.cds.clics

import kotlinx.datetime.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import kotlin.math.round
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object ClicsTime {
    // https://ccs-specs.icpc.io/2021-11/contest_api#json-attribute-types
    private const val DATE_STR = "([0-9]{1,4})-([0-9]{1,2})-([0-9]{1,2})"
    private const val TIME_STR = "([0-9]{1,2}):([0-9]{1,2}):([0-9]{1,2}([.][0-9]{1,})?)"
    private const val OPT_ZONE_STR = "(([-+])([0-9]{1,2}):?([0-9]{2})?)?[zZ]?"
    private val TIME_PATTERN = Pattern.compile("^($DATE_STR)T($TIME_STR)($OPT_ZONE_STR)$")
    fun parseTime(csTime: CharSequence): Instant {
        val matcher = TIME_PATTERN.matcher(csTime)
        return if (matcher.matches()) {
            val yearStr = matcher.group(2)
            val monthStr = matcher.group(3)
            val dayStr = matcher.group(4)
            var year = yearStr.toInt()
            if (yearStr.length <= 2) {
                // https://www.ibm.com/docs/en/i/7.2?topic=mcdtdi-conversion-2-digit-years-4-digit-years-centuries
                year = if (year >= 40) 1900 + year else 2000 + year
            }
            val month = monthStr.toInt()
            val day = dayStr.toInt()
            val isoDate = String.format("%04d-%02d-%02d", year, month, day)
            val hourStr = matcher.group(6)
            val minuteStr = matcher.group(7)
            val secondStr = matcher.group(8)
            val hour = hourStr.toInt()
            val minute = minuteStr.toInt()
            val second = secondStr.toDouble()
            val iSecond = second.toInt()
            val nanoSecond = round(1e9 * (second - iSecond)).toInt()
            val isoTime = String.format("%02d:%02d:%02d.%09d", hour, minute, iSecond, nanoSecond)
            val offsetSignStr = matcher.group(12)
            val offsetHourStr = matcher.group(13)
            val offsetMinuteStr = matcher.group(14)
            val offsetSign = if (offsetSignStr != null && offsetSignStr == "-") -1 else 1
            val offsetHour = offsetHourStr?.toInt() ?: 0
            val offsetMinute = offsetMinuteStr?.toInt() ?: 0
            val isoOffset = String.format("%c%02d:%02d", if (offsetSign == 1) '+' else '-', offsetHour, offsetMinute)
            val isoDateTime = isoDate + "T" + isoTime + isoOffset
            val zdt = ZonedDateTime.parse(isoDateTime, DateTimeFormatter.ISO_DATE_TIME)
            zdt.toInstant().toKotlinInstant()
        } else {
            throw IllegalArgumentException()
        }
    }

    private val RELATIVE_TIME_PATTERN = Pattern.compile("^([-+])?(([0-9]+):)?(([0-9]+):)?([0-9]+([.][0-9]+)?)$")
    fun parseRelativeTime(csTime: CharSequence): Duration {
        val matcher = RELATIVE_TIME_PATTERN.matcher(csTime)
        return if (matcher.matches()) {
            val signStr = matcher.group(1)
            var hourStr = matcher.group(3)
            var minuteStr = matcher.group(5)
            if (minuteStr == null && hourStr != null) {
                minuteStr = hourStr
                hourStr = null
            }
            val secondStr = matcher.group(6)
            val sign = if (signStr != null && signStr == "-") -1 else 1
            val hour = hourStr?.toInt() ?: 0
            val minute = minuteStr?.toInt() ?: 0
            val second = secondStr.toDouble()
            (hour.hours + minute.minutes + second.seconds) * sign
        } else {
            throw IllegalArgumentException("Invalid time format $csTime")
        }
    }

    fun formatIso(duration: Duration) =
        (if (duration.isPositive()) duration else -duration).toComponents { hours, minutes, seconds, nanoseconds ->
            "%s%02d:%02d:%02d.%03d".format(
                if (duration.isPositive()) "" else "-",
                hours,
                minutes,
                seconds,
                nanoseconds / 1000000
            )
        }

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
    fun formatIso(instant: Instant) = Instant.fromEpochMilliseconds(instant.toEpochMilliseconds()).toJavaInstant().atZone(ZoneId.systemDefault()).format(formatter)

    object DurationSerializer : KSerializer<Duration> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ClicsDuration", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Duration) {
            encoder.encodeString(formatIso(value))
        }

        override fun deserialize(decoder: Decoder): Duration {
            return parseRelativeTime(decoder.decodeString())
        }
    }

    object InstantSerializer : KSerializer<Instant> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ClicsInstant", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Instant) {
            encoder.encodeString(formatIso(value))
        }

        override fun deserialize(decoder: Decoder): Instant {
            return parseTime(decoder.decodeString())
        }
    }

}
