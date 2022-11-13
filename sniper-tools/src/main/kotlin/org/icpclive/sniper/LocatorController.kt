package org.icpclive.sniper

import java.io.*
import java.util.*

object LocatorController {
    private const val WIDTH = 1920
    private const val HEIGHT = 1080

    @Throws(FileNotFoundException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        Util.init()
        val `in` = BufferedReader(InputStreamReader(System.`in`))
        while (true) {
            try {
                println("Select sniper (1-" + Util.snipers.size + ")")
                val sniper = `in`.readLine().trim { it <= ' ' }.toInt()
                val scanner = Scanner(File("coordinates-$sniper.txt"))
                val n = scanner.nextInt()
                println("Select teams (space separated) (1-$n)")
                val ss = `in`.readLine().split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                println(Arrays.toString(ss))
                val k = ss.size
                val need: MutableSet<Int> = HashSet()
                for (i in 0 until k) {
                    need.add(ss[i].toInt())
                }
                var res: MutableList<LocatorPoint?> = ArrayList()
                val allPoints: MutableList<LocatorPoint> = ArrayList()
                for (i in 1..n) {
                    val point = LocatorPoint(
                        scanner.nextInt(),
                        scanner.nextDouble(),
                        scanner.nextDouble(),
                        scanner.nextDouble()
                    )
                    allPoints.add(point)
                    if (need.contains(point.id)) {
                        res.add(point)
                    }
                }
                var D = 1e100
                for (p1 in allPoints) {
                    for (p2 in allPoints) {
                        if (p1 === p2) continue
                        D = Math.min(D, p1.distTo(p2))
                    }
                }
                res = translatePoints(res, sniper, D)
                val url = "http://172.24.0.173:8080/api/admin/teamLocator/"
                val parts: MutableList<String> = ArrayList()
                for (p in res) {
                    parts.add(
                        String.format(
                            "{\"x\": %d, \"y\": %d, \"radius\": \"%d\", \"cdsTeamId\": %d}",
                            p!!.x.toInt(),
                            p.y.toInt(),
                            p.r.toInt(),
                            p.id
                        )
                    )
                }
                println(parts)
                val data = "{\"circles\": [" + java.lang.String.join(",", parts) + "]}"
                println(data)
                Util.sendPost(url + "show_with_settings", "application/json", data)
                println("Press Enter to hide")
                `in`.readLine()
                Util.sendPost(url + "hide", "application/json", "")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @Throws(Exception::class)
    private fun translatePoints(points: List<LocatorPoint?>, sniper: Int, d: Double): MutableList<LocatorPoint?> {
        val camera = Util.snipers[sniper - 1]
        val response = Util.sendGet(
            camera!!.hostName + "axis-cgi/com/ptz.cgi?query=position,limits&camera=1&html=no&timestamp="
                    + Util.getUTCTime()
        )
        camera.update()
        val config = Util.parseCameraConfiguration(response)
        val res: MutableList<LocatorPoint?> = ArrayList()
        for (pp in points) {
            var p = pp
            p!!.r = d / 2
            p = p.rotateY(config.pan)
            p = p.rotateX(-config.tilt)
            p = p.multiply(1 / p.z)
            p = p.multiply(WIDTH / config.angle)
            p = p.move(LocatorPoint((WIDTH / 2).toDouble(), (HEIGHT / 2).toDouble(), 0.0))
            res.add(p)
        }
        return res
    }
}