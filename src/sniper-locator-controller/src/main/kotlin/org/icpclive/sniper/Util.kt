package org.icpclive.sniper

import org.icpclive.sniper.Config.coordinatesTxtPath
import org.icpclive.sniper.Config.snipersTxtPath
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

object Util {
    val snipers: MutableList<SniperInfo> = ArrayList()
    const val ANGLE = 1.28

    fun sendGet(url: String): String {
        println("send get $url")
        val obj = URI(url).toURL()
        val con = obj.openConnection() as HttpURLConnection
        con.requestMethod = "GET"
        return con.inputStream.reader().buffered().use {
            buildString {
                while (true) {
                    append(it.readLine() ?: break)
                }
            }
        }
    }

    fun getUTCTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-d'T'HH:mm:ss'Z'")
        sdf.timeZone = SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC")
        val cal: Calendar = GregorianCalendar(TimeZone.getTimeZone("UTC"))
        return sdf.format(cal.time)
    }

    fun parseCameraConfiguration(ss: String): LocatorConfig {
        val s = ss.trim() { it <= ' ' }
        var l: Int
        var r: Int = 0
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
            while (r < s.length && Character.isAlphabetic(s[r].code)) {
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

    fun init() {
        val inp = Scanner(snipersTxtPath.toFile())
        val m = inp.nextInt();
        val urls = Array(m) { inp.next() }
        inp.close()
        for (i in urls.indices) {
            snipers.add(
                SniperInfo(
                    urls[i],
                    File(coordinatesTxtPath + "-${i + 1}.txt"),
                    i + 1
                )
            )
        }
    }

    fun sendPost(urlString: String, contentType: String?, data: String) {
        val url = URI(urlString).toURL()
        val con = url.openConnection() as HttpURLConnection
        con.requestMethod = "POST"
        con.setRequestProperty("Content-Type", contentType)
        con.doOutput = true
        val out = data.toByteArray(StandardCharsets.UTF_8)
        val length = out.size
        con.setFixedLengthStreamingMode(length)
        con.connect()
        con.outputStream.use {
            it.write(out)
        }
    }
}