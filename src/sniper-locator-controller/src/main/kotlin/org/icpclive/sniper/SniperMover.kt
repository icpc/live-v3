package org.icpclive.sniper

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.icpclive.util.getLogger
import java.io.File
import java.util.*
import kotlin.math.atan2
import kotlin.math.hypot

object SniperMover {
    @JvmStatic
    suspend fun main(args: Array<String>) {
        Locale.setDefault(Locale.US)
        val `in` = Scanner(System.`in`)
        while (true) {
            println("Select sniper (1-" + Util.snipers.size + ")")
            val sniper = `in`.nextInt()
            println("Select team")
            val teamId = `in`.nextInt()
            if (moveToTeam(sniper, teamId) == null) {
                println("No such team $teamId location for sniper $sniper")
            }
        }
    }

    private const val DEFAULT_SPEED = "0.52"

    suspend fun moveToTeam(sniperNumber: Int, teamId: Int): LocatorPoint? {
        val point = getLocationPointByTeam(sniperNumber, teamId) ?: return null

        if (point.y > 0) {
            point.x = -point.x
            point.y = -point.y
            point.z = -point.z
        }
        var tilt = atan2(point.y, hypot(point.x, point.z))
        var pan = atan2(-point.x, -point.z)
        pan *= 180 / Math.PI
        tilt *= 180 / Math.PI
        val d = hypot(point.x, hypot(point.y, point.z))
        val mag = 0.5 * d
        val maxmag = 35.0
        val zoom = (mag * 9999 - 1) / (maxmag - 1)
        move(sniperNumber, pan, tilt, zoom.toInt())
        println(point)
        return point
    }

    private fun getLocationPointByTeam(sniperNumber: Int, teamId: Int): LocatorPoint? {
        val scanner =
            Scanner(File(Config.configDirectory.toAbsolutePath().toString() + "/coordinates-$sniperNumber.txt"))
        scanner.nextInt() // count of teams in coordinates file (we can ignore this number)
        while (scanner.hasNextInt()) {
            val id = scanner.nextInt()
            if (id == teamId) {
                return LocatorPoint(
                    scanner.nextDouble(),
                    scanner.nextDouble(),
                    scanner.nextDouble()
                )
            }
        }
        return null
    }

    @Throws(Exception::class)
    private suspend fun move(sniper: Int, pan: Double, tilt: Double, zoom: Int) {
        val hostName = Util.snipers[sniper - 1].hostName
        val setPositionResponse = Util.sniperRequest(
            hostName, mapOf(
                "camera" to 1,
                "tilt" to tilt,
                "pan" to pan,
                "zoom" to zoom,
                "speed" to 100,
                "timestamp" to Util.getUTCTime()
            )
        )
        logger.info("Set sniper $sniper position: $setPositionResponse")

        val setSpeedResponse = Util.sniperRequest(
            hostName, mapOf("camera" to 1, "speed" to DEFAULT_SPEED, "timestamp" to Util.getUTCTime())
        )
        logger.info("Set sniper $sniper speed: $setSpeedResponse")
    }

    private val logger = getLogger(this::class)
}
