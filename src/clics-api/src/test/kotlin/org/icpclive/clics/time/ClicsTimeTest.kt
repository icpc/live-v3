package org.icpclive.clics.time

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object ClicsTimeTest {
    var years = arrayOf(
        "2001" to 2001,
        "2345" to 2345
    )
    var months = arrayOf(
        "1" to 1,
        "01" to 1,
        "1" to 1,
        "01" to 1,
        "12" to 12
    )
    var days = arrayOf(
        "7" to 7,
        "22" to 22
    )
    var hours = arrayOf(
        "00" to 0,
        "0" to 0,
        "1" to 1,
        "8" to 8,
        "08" to 8,
        "23" to 23
    )
    var minutes = arrayOf(
        "00" to 0,
        "0" to 0,
        "8" to 8,
        "08" to 8,
        "59" to 59
    )
    var seconds = arrayOf(
        "00" to 0.0,
        "0" to 0.0,
        "8" to 8.0,
        "08" to 8.0,
        "59" to 59.0,
        "1.2" to 1.2,
        "1.23" to 1.23,
        "1.234" to 1.234,
        "1.23456" to 1.234
    )
    var zones = arrayOf(
        "" to "+00:00",
        "Z" to "+00:00",
        "z" to "+00:00",
        "+12" to "+12:00",
        "+1" to "+01:00",
        "-730" to "-07:30",
        "+1234" to "+12:34",
        "-12:34" to "-12:34"
    )

    @Test
    fun parseTimeTest() {
        for (YY in years) for (MM in months) for (DD in days) {
            for (hh in hours) for (mm in minutes) for (ss in seconds) for (zz in zones) {
                val testTime = "${YY.first}-${MM.first}-${DD.first}T${hh.first}:${mm.first}:${ss.first}${zz.first}"
                val intS = floor(ss.second).toInt()
                val ns = round(1e9 * (ss.second - intS)).toInt()
                val isoDateTime = String.format(
                    "%04d-%02d-%02dT%02d:%02d:%02d.%09d%s",
                    YY.second, MM.second, DD.second, hh.second, mm.second, intS, ns, zz.second
                )
                val zdt = ZonedDateTime.parse(isoDateTime, DateTimeFormatter.ISO_DATE_TIME)
                val expect = zdt.toInstant().toEpochMilli()
                val result: Long = parseClicsTime(testTime).toEpochMilliseconds()
                assertEquals(expect, result)
                val formatted = formatClicsTime(Instant.fromEpochMilliseconds(expect))
                val parsedBack = ZonedDateTime.parse(formatted, DateTimeFormatter.ISO_DATE_TIME)
                assertEquals(parsedBack.toInstant(), zdt.toInstant())
            }
        }
    }

    var relHours = arrayOf(
        null to 0,
        "00" to 0,
        "0" to 0,
        "1" to 1,
        "8" to 8,
        "08" to 8,
        "23" to 23
    )
    var relMinutes = arrayOf(
        null to 0,
        "00" to 0,
        "0" to 0,
        "8" to 8,
        "08" to 8,
        "59" to 59
    )
    var relSeconds = arrayOf(
        "00" to 0.0,
        "0" to 0.0,
        "8" to 8.0,
        "08" to 8.0,
        "59" to 59.0,
        "1.2" to 1.2,
        "1.23" to 1.23,
        "1.234" to 1.234,
        "1.23456" to 1.235
    )
    var relSigns = arrayOf(
        "" to 1,
        "+" to 1,
        "-" to -1
    )

    @Test
    fun parseRelativeTimeTest() {
        for (pp in relSigns) for (hh in relHours) for (mm in relMinutes) for (ss in relSeconds) {
            if (mm.first == null && hh.first != null) continue
            val testRelTime = String.format(
                "%s%s%s%s",
                pp.first,
                if (hh.first != null) "" + hh.first + ":" else "",
                if (mm.first != null) "" + mm.first + ":" else "",
                ss.first
            )
            val t = pp.second * (60.0 * 60.0 * hh.second + 60.0 * mm.second + ss.second)
            val expect = Math.round(t * 1000.0)
            val s = ss.second
            val `is` = s.toInt()
            val ns = Math.round(1e9 * (s - `is`)).toInt()
            val isoInstant = String.format(
                "%s%02d:%02d:%02d.%09d",
                if (pp.second == 1) "+" else "-", hh.second, mm.second, `is`, ns
            )
            val result: Long = parseClicsRelativeTime(testRelTime).toDouble(DurationUnit.MILLISECONDS).roundToLong()
            assertEquals(expect, result, "$testRelTime vs $isoInstant")
        }
    }

    @Test
    fun durationJsonSerialisation() {
        @Serializable
        data class ObjectWithDuration(
            @Serializable(with = DurationSerializer::class)
            val dur: Duration,
        )

        for (pp in relSigns) for (hh in relHours) for (mm in relMinutes) for (ss in relSeconds) {
            hh.first ?: continue
            mm.first ?: continue
            val duration = (hh.second.hours + mm.second.minutes + ss.second.seconds) * pp.second
            val durationString = formatClicsRelativeTime(duration)
            parseClicsRelativeTime(durationString)
            val obj = ObjectWithDuration(duration)
            val encodedString = Json.encodeToString(obj)
            assertEquals("{\"dur\":\"$durationString\"}", encodedString)
            assertEquals(obj, Json.decodeFromString<ObjectWithDuration>(encodedString))
        }
    }
}