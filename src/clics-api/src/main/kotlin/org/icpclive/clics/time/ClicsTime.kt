package org.icpclive.clics.time

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private fun DateTimeFormatBuilder.WithDateTimeComponents.formatBase(padding: Padding) {
    year(padding)
    char('-')
    monthNumber(padding)
    char('-')
    dayOfMonth(padding)
    char('T')
    hour(padding)
    char(':')
    minute(padding)
    char(':')
    second(padding)
    optional {
        char('.')
        alternativeParsing({ secondFraction() }) {
            secondFraction(3)
        }
    }
    alternativeParsing({}, { char('z') }) {
        optional(ifZero = "Z") {
            offsetHours(padding)
            alternativeParsing({}) {
                alternativeParsing({}) { chars(":") }
                offsetMinutesOfHour()
                optional {
                    char(':')
                    offsetSecondsOfMinute()
                }
            }
        }
    }
}

// Basically DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET, but with several tweaks
// 1. Skipping leading zeros is allowed
// 2. Exactly 3 digits of the second fraction are printed if any
private val format = DateTimeComponents.Format {
    alternativeParsing({ formatBase(Padding.NONE) }) {
        formatBase(Padding.ZERO)
    }
}

internal fun parseClicsTime(csTime: CharSequence) = format.parse(csTime).toInstantUsingOffset()
internal fun formatClicsTime(instant: Instant) = instant.format(format, TimeZone.currentSystemDefault().offsetAt(instant))

private val RELATIVE_TIME_PATTERN = Regex("^([-+])?(([0-9]+):)?(([0-9]+):)?([0-9]+([.][0-9]+)?)$")
internal fun parseClicsRelativeTime(csTime: CharSequence): Duration {
    val matcher = RELATIVE_TIME_PATTERN.matchAt(csTime, 0) ?: error("Invalid relative time format $csTime")
    val signStr = matcher.groups[1]?.value
    var hourStr = matcher.groups[3]?.value
    var minuteStr = matcher.groups[5]?.value
    if (minuteStr == null && hourStr != null) {
        minuteStr = hourStr
        hourStr = null
    }
    val secondStr = matcher.groups[6]!!.value
    val sign = if (signStr != null && signStr == "-") -1 else 1
    val hour = hourStr?.toInt() ?: 0
    val minute = minuteStr?.toInt() ?: 0
    val second = secondStr.toDouble()
    return (hour.hours + minute.minutes + second.seconds) * sign
}

internal fun formatClicsRelativeTime(duration: Duration) =
    (if (duration.isPositive()) duration else -duration).toComponents { hours, minutes, seconds, nanoseconds ->
        "%s%02d:%02d:%02d.%03d".format(
            if (duration.isPositive()) "" else "-",
            hours,
            minutes,
            seconds,
            nanoseconds / 1000000
        )
    }