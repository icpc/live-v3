package org.icpclive.sniper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.*

object LocatorController {
    private const val WIDTH = 1920
    private const val HEIGHT = 1080

//    @JvmStatic
//    fun main(args: Array<String>) {
//        Util.init()
//        val input = BufferedReader(InputStreamReader(System.`in`))
//        while (true) {
//            try {
//                println("Select sniper (1-${Util.snipers.size})")
//                val sniper = input.readLine().trim { it <= ' ' }.toInt()
//                println("Select teams (space separated)")
//                val teamIds = input.readLine()
//                    .split("\\s+".toRegex())
//                    .dropLastWhile { it.isEmpty() }
//                    .map { it.toInt() }
//                    .toSet()
//
//                showLocatorWidget(sniper, teamIds)
//
//                println("Press Enter to hide")
//                input.readLine()
//                hideLocatorWidget()
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
//    }

    suspend fun getLocatorWidgetConfig(sniperNumber: Int, teamIds: Set<Int>): TeamLocatorSettings {
        val scanner = withContext(Dispatchers.IO) {
            Scanner(File("coordinates-$sniperNumber.txt"))
        }
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
        return TeamLocatorSettings(
            scene = "sniper${sniperNumber}",
            circles = translatePoints(
                allPoints.filter { teamIds.contains(it.id) },
                sniperNumber,
                d
            ).map {
                TeamLocatorCircleSettings(
                    x = it.x.toInt(),
                    y = it.y.toInt(),
                    radius = it.r.toInt(),
                    teamId = it.id,
                )
            }
        )
    }

//    fun showLocatorWidget(sniperNumber: Int, teamIds: Set<Int>) {
//        val config = getLocatorWidgetConfig(sniperNumber, teamIds)
//        Util.sendPost(showUrl(), "application/json", Json.encodeToString(config))
//    }

//    fun hideLocatorWidget() {
//        Util.sendPost(hideUrl(), "application/json", "")
//    }

    private suspend fun translatePoints(points: List<LocatorPoint>, sniper: Int, d: Double): List<LocatorPoint> {
        val camera = Util.snipers[sniper - 1]
        val response = Util.sniperRequest(
            camera.hostName,
            mapOf("query" to "position,limits", "camera" to 1, "html" to "no", "timestamp" to Util.getUTCTime())
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
