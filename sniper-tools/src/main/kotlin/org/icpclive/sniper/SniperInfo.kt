package org.icpclive.sniper

import java.io.*
import java.util.*

class SniperInfo(val hostName: String?, val coordinatesFile: File, val cameraID: Int) {
    var coordinates: Array<LocatorPoint?> = emptyArray()

    init {
        update()
    }

    @Throws(FileNotFoundException::class)
    fun update() {
        val `in` = Scanner(coordinatesFile)
        `in`.useLocale(Locale.US)
        val n = `in`.nextInt()
        coordinates = arrayOfNulls(n)
        for (i in 0 until n) {
            coordinates[i] = LocatorPoint(`in`.nextInt(), `in`.nextDouble(), `in`.nextDouble(), `in`.nextDouble())
        }
    }

    override fun toString(): String {
        return "Sniper " + (cameraID + 1)
    }
}