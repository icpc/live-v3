package org.icpclive.oracle

object LocatorController {
    private const val WIDTH = 1920
    private const val HEIGHT = 1080

    suspend fun getLocatorWidgetConfig(oracleNumber: Int, teamIds: Set<String>): TeamLocatorSettings {
        val allPoints = Util.loadLocatorPoints(oracleNumber)
        var d = 1e100
        for (p1 in allPoints) {
            for (p2 in allPoints) {
                if (p1 === p2) continue
                d = d.coerceAtMost(p1.distTo(p2))
            }
        }
        return TeamLocatorSettings(
            scene = "oracle${oracleNumber}",
            circles = translatePoints(
                allPoints.filter { teamIds.contains(it.id) },
                oracleNumber,
                d
            ).map {
                TeamLocatorCircleSettings(
                    x = it.x.toInt(),
                    y = it.y.toInt(),
                    radius = it.r.toInt(),
                    cdsTeamId = it.id,
                )
            }
        )
    }

    private suspend fun translatePoints(points: List<LocatorPoint>, oracle: Int, d: Double): List<LocatorPoint> {
        val camera = Util.oracles[oracle - 1]
        val response = Util.oracleRequest(
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
