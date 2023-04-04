package org.icpclive.sniper

import java.io.*
import java.util.*

object LocatorController {
    private const val WIDTH = 1920
    private const val HEIGHT = 1080
    var overlayUrl = "http://172.24.0.173:8080"

    @JvmStatic
    fun main(args: Array<String>) {
        Util.init()
        val input = BufferedReader(InputStreamReader(System.`in`))
        while (true) {
            try {
                println("Select sniper (1-${Util.snipers.size})")
                val sniper = input.readLine().trim { it <= ' ' }.toInt()
                println("Select teams (space separated)")
                val teamIds = input.readLine()
                    .split("\\s+".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .map { it.toInt() }
                    .toSet()

                showLocatorWidget(sniper, teamIds)

                println("Press Enter to hide")
                input.readLine()
                hideLocatorWidget()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showUrl() = "$overlayUrl/api/admin/teamLocator/show_with_settings"
    private fun hideUrl() = "$overlayUrl/api/admin/teamLocator/hide"

    fun showLocatorWidget(sniperNumber: Int, teamIds: Set<Int>) {
        val scanner = Scanner(File("coordinates-$sniperNumber.txt"))
        val n = scanner.nextInt()

        val allPoints = mutableListOf<LocatorPoint>()
        for (i in 1..n) {
            val point = LocatorPoint(
                scanner.nextInt(),
                scanner.nextDouble(),
                scanner.nextDouble(),
                scanner.nextDouble()
            )
            allPoints.add(point)
        }
        var d = 1e100
        for (p1 in allPoints) {
            for (p2 in allPoints) {
                if (p1 === p2) continue
                d = d.coerceAtMost(p1.distTo(p2))
            }
        }
        println("xxx")
        val parts = translatePoints(
            allPoints.filter { teamIds.contains(it.id) },
            sniperNumber,
            d
        ).map {
            "{\"x\": ${it.x}, \"y\": ${it.y}, \"radius\": \"${it.r}\", \"cdsTeamId\": ${it.id}}"
        }
        val data = "{\"circles\": [${parts.joinToString(",")}], \"scene\": \"sniper${sniperNumber}\"}"
        Util.sendPost(showUrl(), "application/json", data)
    }

    fun hideLocatorWidget() {
        Util.sendPost(hideUrl(), "application/json", "")
    }

    private fun translatePoints(points: List<LocatorPoint>, sniper: Int, d: Double): List<LocatorPoint> {
        val camera = Util.snipers[sniper - 1]
        val response = Util.sendGet(
            "${camera.hostName}axis-cgi/com/ptz.cgi?query=position,limits&camera=1&html=no&timestamp=${Util.getUTCTime()}"
        )
        camera.update()
        val config = Util.parseCameraConfiguration(response)
        val res: MutableList<LocatorPoint> = ArrayList()
        for (pp in points) {
            pp.r = d / 2
            res.add(
                pp.rotateY(config.pan)
                    .rotateX(-config.tilt)
                    .multiply(1 / pp.z)
                    .multiply(WIDTH / config.angle)
                    .move(LocatorPoint((WIDTH / 2).toDouble(), (HEIGHT / 2).toDouble(), 0.0))
            )
        }
        return res
    }
}
