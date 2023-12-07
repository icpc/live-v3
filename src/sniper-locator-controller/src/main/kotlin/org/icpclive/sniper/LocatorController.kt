package org.icpclive.sniper

object LocatorController {
    private const val WIDTH = 1920;
    private const val HEIGHT = 1920;
    val overlayUrl = "http://127.0.0.1:8080"

    @JvmStatic
    fun main(args: Array<String>) {
        Util.init();
    }
}