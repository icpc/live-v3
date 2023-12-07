package org.icpclive.sniper

import java.io.File
import java.util.*


class SniperInfo(val hostName: String, val coordinatesFile: File, val cameraID: Int) {
    var coordinates = load()

    fun update() {
        coordinates = load()
    }

    private fun load() : Array<LocatorPoint> {
        return Scanner(coordinatesFile).use { inp ->
            inp.useLocale(Locale.US)
            Array(inp.nextInt()) {
                LocatorPoint(inp.nextInt(), inp.nextDouble(), inp.nextDouble(), inp.nextDouble())
            }
        }
    }

    override fun toString() : String {
        return "Sniper ${cameraID + 1}";
    }
}