package org.icpclive.cds.clics

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert
import org.junit.Test
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

class ClicksTimeTest {
    var years = arrayOf(arrayOf("2001", 2001), arrayOf("2345", 2345))
    var months = arrayOf(
        arrayOf("1", 1),
        arrayOf("01", 1),
        arrayOf("1", 1),
        arrayOf("01", 1),
        arrayOf("12", 12)
    )
    var days = arrayOf(arrayOf("7", 7), arrayOf("22", 22))
    var hours = arrayOf(
        arrayOf("00", 0),
        arrayOf("0", 0),
        arrayOf("1", 1),
        arrayOf("8", 8),
        arrayOf("08", 8),
        arrayOf("23", 23)
    )
    var minutes = arrayOf(
        arrayOf("00", 0),
        arrayOf("0", 0),
        arrayOf("8", 8),
        arrayOf("08", 8),
        arrayOf("59", 59)
    )
    var seconds = arrayOf(
        arrayOf("00", 0.0),
        arrayOf("0", 0.0),
        arrayOf("8", 8.0),
        arrayOf("08", 8.0),
        arrayOf("59", 59.0),
        arrayOf("1.2", 1.2),
        arrayOf("1.23", 1.23),
        arrayOf("1.234", 1.234),
        arrayOf("1.23456", 1.234)
    )
    var zones = arrayOf(
        arrayOf("", "+00:00"),
        arrayOf("Z", "+00:00"),
        arrayOf("z", "+00:00"),
        arrayOf("+12", "+12:00"),
        arrayOf("+1", "+01:00"),
        arrayOf("-730", "-07:30"),
        arrayOf("+1234", "+12:34"),
        arrayOf("-12:34", "-12:34")
    )
    var signs = arrayOf(arrayOf("", 1), arrayOf("+", 1), arrayOf("-", -1))

    @Test
    fun parseTimeTest() {
        for (YY in years) for (MM in months) for (DD in days) for (hh in hours) for (mm in minutes) for (ss in seconds) for (zz in zones) {
            val testTime = String.format(
                "%s-%s-%sT%s:%s:%s%s",
                YY[0], MM[0], DD[0], hh[0], mm[0], ss[0], zz[0]
            )
            val s = ss[1] as Double
            val `is` = s.toInt()
            val ns = Math.rint(1e9 * (s - `is`)).toInt()
            val isoDateTime = String.format(
                "%04d-%02d-%02dT%02d:%02d:%02d.%09d%s",
                YY[1], MM[1], DD[1], hh[1], mm[1], `is`, ns, zz[1]
            )
            val zdt = ZonedDateTime.parse(isoDateTime, DateTimeFormatter.ISO_DATE_TIME)
            val expect = zdt.toInstant().toEpochMilli()
            val result: Long = ClicsTime.parseTime(testTime).toEpochMilliseconds()
            Assert.assertEquals("$testTime vs $isoDateTime", expect, result)
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
    var relSigns = arrayOf("" to 1, "+" to 1, "-" to -1)

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
            val result: Long = ClicsTime.parseRelativeTime(testRelTime).toDouble(DurationUnit.MILLISECONDS).roundToLong()
            Assert.assertEquals("$testRelTime vs $isoInstant", expect, result)
        }
    }

    @Test
    fun durationJsonSerialisation() {
        @Serializable
        data class ObjectWithDuration(
            @Serializable(with = ClicsTime.DurationSerializer::class)
            val dur: Duration
        )

        for (pp in relSigns) for (hh in relHours) for (mm in relMinutes) for (ss in relSeconds) {
            hh.first ?: continue
            mm.first ?: continue
            val duration = (hh.second.hours + mm.second.minutes + ss.second.seconds) * pp.second
            val durationString = ClicsTime.formatIso(duration)
            ClicsTime.parseRelativeTime(durationString)
            val obj = ObjectWithDuration(duration)
            val encodedString = Json.encodeToString(obj)
            Assert.assertEquals("{\"dur\":\"$durationString\"}", encodedString)
            Assert.assertEquals(obj, Json.decodeFromString<ObjectWithDuration>(encodedString))
        }
    }
}