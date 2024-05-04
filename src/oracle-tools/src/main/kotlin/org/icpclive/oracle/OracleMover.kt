package org.icpclive.oracle

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.icpclive.cds.util.getLogger
import java.util.*
import kotlin.math.atan2
import kotlin.math.hypot

object OracleMover {
    @JvmStatic
    suspend fun main(args: Array<String>) {
        Locale.setDefault(Locale.US)
        val `in` = Scanner(System.`in`)
        while (true) {
            println("Select oracle (1-" + Util.oracles.size + ")")
            val oracle = `in`.nextInt()
            println("Select team")
            val teamId = `in`.next()
            if (moveToTeam(oracle, teamId) == null) {
                println("No such team $teamId location for oracle $oracle")
            }
        }
    }

    private const val DEFAULT_SPEED = "0.52"

    suspend fun moveToTeam(oracleNumber: Int, teamId: String): LocatorPoint? {
        println("moveToTeam $oracleNumber $teamId")
        val point = getLocationPointByTeam(oracleNumber, teamId) ?: return null

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
        move(oracleNumber, pan, tilt, zoom.toInt())
        println(point)
        return point
    }

    private fun getLocationPointByTeam(oracleNumber: Int, teamId: String): LocatorPoint? {
        val x = Util.loadLocatorPoints(oracleNumber).find { it.id == teamId }
        return x
    }

    @Throws(Exception::class)
    private suspend fun move(oracle: Int, pan: Double, tilt: Double, zoom: Int) {
        val hostName = Util.oracles[oracle - 1].hostName
        val setPositionResponse = Util.oracleRequest(
            hostName, mapOf(
                "camera" to 1,
                "tilt" to tilt,
                "pan" to pan,
                "zoom" to zoom,
                "speed" to 100,
                "timestamp" to Util.getUTCTime()
            )
        )
        logger.info("Set oracle $oracle position: $setPositionResponse")

        val setSpeedResponse = Util.oracleRequest(
            hostName, mapOf("camera" to 1, "speed" to DEFAULT_SPEED, "timestamp" to Util.getUTCTime())
        )
        logger.info("Set oracle $oracle speed: $setSpeedResponse")
    }

    private val logger = getLogger(OracleMover::class)
}
