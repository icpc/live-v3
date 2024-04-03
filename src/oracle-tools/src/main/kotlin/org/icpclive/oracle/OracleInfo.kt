package org.icpclive.oracle

import java.io.File
import java.util.*


class OracleInfo(val hostName: String, private val coordinatesFile: File, val cameraID: Int) {
    private var coordinates = load()

    fun update() {
        coordinates = load()
    }

    private fun load(): Array<LocatorPoint> {
        return Scanner(coordinatesFile).use { inp ->
            inp.useLocale(Locale.US)
            Array(inp.nextInt()) {
                LocatorPoint(inp.next(), inp.nextDouble(), inp.nextDouble(), inp.nextDouble())
            }
        }
    }

    override fun toString(): String {
        return "Oracle ${cameraID + 1}"
    }
}
