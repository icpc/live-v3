package org.icpclive.oracle

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.icpclive.oracle.Config.oraclesTxtPath
import java.io.File
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*

object Util {
    val httpClient = HttpClient {
        install(HttpTimeout)
    }

    suspend fun oracleRequest(oracleHostName: String, parameters: Map<String, Any?>): String {
        return httpClient.get(oracleHostName + "/axis-cgi/com/ptz.cgi") {
            for (parameter in parameters) {
                parameter(parameter.key, parameter.value)
            }
        }.bodyAsText()
    }

    val oracles: MutableList<OracleInfo> = ArrayList()
    const val ANGLE = 1.28
    private var inited: Boolean = false

    fun getUTCTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-d'T'HH:mm:ss'Z'")
        sdf.timeZone = SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC")
        val cal: Calendar = GregorianCalendar(TimeZone.getTimeZone("UTC"))
        return sdf.format(cal.time)
    }

    fun parseCameraConfiguration(ss: String): LocatorConfig {
        val s = ss.trim { it <= ' ' }
        var l: Int
        var r = 0
        var newPan: Double? = null
        var newTilt: Double? = null
        var newAngle: Double? = null
        while (r < s.length) {
            l = r
            r = l + 1
            while (r < s.length && Character.isAlphabetic(s[r].code)) {
                r++
            }
            val key = s.substring(l, r)
            l = r + 1
            r = l + 1
            while (r < s.length && !Character.isAlphabetic(s[r].code)) {
                r++
            }
            val value = s.substring(l, r).toDoubleOrNull() ?: continue
            when (key) {
                "pan" -> newPan = value * Math.PI / 180
                "tilt" -> newTilt = value * Math.PI / 180
                "zoom" -> {
                    val maxmag = 35.0
                    val mag = 1 + (maxmag - 1) * value / 9999
                    newAngle = ANGLE / mag
                }
            }
        }
        require(newPan != null)
        require(newTilt != null)
        require(newAngle != null)
        return LocatorConfig(newPan, newTilt, newAngle)
    }

    fun loadLocatorPoints(oracleNumber: Int): List<LocatorPoint> {
        val x = config.configDirectory.resolve("coordinates-$oracleNumber.txt")
        val scanner = Scanner(x)
        val n = scanner.nextInt()

        val allPoints = mutableListOf<LocatorPoint>()
        for (i in 1..n) {
            try {
                val point = LocatorPoint(
                    scanner.next(),
                    scanner.nextDouble(),
                    scanner.nextDouble(),
                    scanner.nextDouble()
                )
                allPoints.add(point)
            }  catch (e: Throwable) {
                println("sooo bad $e")
            }

        }
        return allPoints
    }

    private fun init(oraclesPath: String, configDir: Path) {
        Locale.setDefault(Locale.US)
        val inp = Scanner(File(oraclesPath))
        val m = inp.nextInt()
        val urls = Array(m) { inp.next() }
        inp.close()
        for (i in urls.indices) {
            val file: File
            try {
                file = configDir.resolve("coordinates-${i + 1}.txt").toFile()
            } catch (e: Exception) {
                println(e.message)
                throw e
            }

            oracles.add(
                OracleInfo(
                    urls[i],
                    file,
                    i + 1
                )
            )
        }
    }

    fun initForCalibrator(oraclesPath: String, configDir: Path) {
        init(oraclesPath, configDir)
    }

    fun initForServer() {
        if (inited) return
        inited = true
        init(oraclesTxtPath.toString(), Config.configDirectory.toAbsolutePath())
    }
}
