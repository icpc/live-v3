import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import java.util.*

inline fun <reified T> catchToNull(f: () -> T) = try { f() } catch (_: Exception) { null }

private fun guessDatetimeFormatLocal(time:String) =
    catchToNull { LocalDateTime.parse(time) }  ?:
    catchToNull { LocalDateTime.parse(time.trim().replace(" ", "T")) }


fun guessDatetimeFormat(time: String) : Instant =
    catchToNull { Instant.fromEpochMilliseconds(time.toLong() * 1000L) } ?:
    catchToNull { Instant.parse(time) }  ?:
    guessDatetimeFormatLocal(time)?.toInstant(TimeZone.currentSystemDefault()) ?:
    throw IllegalArgumentException("Failed to parse date: $time")

val Instant.humanReadable: String
    get() = Date(this.toEpochMilliseconds()).toString()