package org.icpclive.sniper

import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

object Util {
    var snipers: MutableList<SniperInfo?> = ArrayList()
    const val ANGLE = 1.28
    @Throws(Exception::class)
    fun sendGet(url: String?): String {
        val obj = URL(url)
        val con = obj.openConnection() as HttpURLConnection
        con.requestMethod = "GET"
        val `in` = BufferedReader(
            InputStreamReader(con.inputStream)
        )
        var inputLine: String?
        val response = StringBuffer()
        while (`in`.readLine().also { inputLine = it } != null) {
            response.append(inputLine)
        }
        `in`.close()
        return response.toString()
    }

    fun getUTCTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-d'T'HH:mm:ss'Z'")
        sdf.timeZone = SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC")
        val cal: Calendar = GregorianCalendar(TimeZone.getTimeZone("UTC"))
        return sdf.format(cal.time)
    }

    fun parseCameraConfiguration(ss: String?): LocatorConfig {
        var s = ss
        s = s!!.trim { it <= ' ' }
        var l: Int
        var r = 0
        var newPan = Double.NaN
        var newTilt = Double.NaN
        var newAngle = Double.NaN
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
            try {
                val value = s.substring(l, r).toDouble()
                when (key) {
                    "pan" -> newPan = value * Math.PI / 180
                    "tilt" -> newTilt = value * Math.PI / 180
                    "zoom" -> {
                        val maxmag = 35.0
                        val mag = 1 + (maxmag - 1) * value / 9999
                        newAngle = ANGLE / mag
                    }
                }
            } catch (e: Exception) {
            }
        }
        if (java.lang.Double.isNaN(newPan) || java.lang.Double.isNaN(newTilt) || java.lang.Double.isNaN(newAngle)) {
            throw AssertionError()
        }
        return LocatorConfig(newPan, newTilt, newAngle)
    }

    fun init() {
        try {
            val `in` = Scanner(File("snipers.txt"))
            val m = `in`.nextInt()
            val urls = arrayOfNulls<String>(m)
            for (i in 0 until m) {
                urls[i] = `in`.next()
            }
            `in`.close()
            for (i in urls.indices) {
                snipers.add(
                    SniperInfo(
                        urls[i],
                        File("coordinates-" + (i + 1) + ".txt"), i + 1
                    )
                )
            }
        } catch (e: FileNotFoundException) {
            throw AssertionError(e)
        }
    }

    @Throws(IOException::class)
    fun sendPost(urlString: String?, contentType: String?, data: String) {
        val url = URL(urlString)
        val con = url.openConnection()
        val http = con as HttpURLConnection
        http.requestMethod = "POST"
        http.setRequestProperty("Content-Type", contentType)
        http.doOutput = true
        val out = data.toByteArray(StandardCharsets.UTF_8)
        val length = out.size
        http.setFixedLengthStreamingMode(length)
        http.connect()
        val os = http.outputStream
        os.write(out)
        os.close()
    }
}