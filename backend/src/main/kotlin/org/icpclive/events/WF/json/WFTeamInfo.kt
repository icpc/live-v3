package org.icpclive.events.WF.json

import org.icpclive.events.WF.WFTeamInfo

/**
 * Created by Meepo on  4/1/2018.
 */
class WFTeamInfo(problems: Int) : WFTeamInfo(problems) {
    override var alias: String = ""

    // videos url
    var photo: String? = null
    var video: String? = null
    lateinit var screens: Array<String>
    lateinit var cameras: Array<String>
    fun isTemplated(type: String?): Boolean {
        when (type) {
            "screen" -> return false
            "camera" -> return false
        }
        return true
    }

    fun getUrlByType(type: String?): String? {
        return when (type) {
            "screen" -> screens[0]
            "camera" -> cameras[0]
            "video" -> alias
            else -> ""
        }
    }

    fun getUrlByType(type: String?, id: Int): String? {
        return when (type) {
            "screen" -> screens[id % screens.size]
            "camera" -> cameras[id % cameras.size]
            "video" -> alias
            else -> ""
        }
    }

    override fun toString(): String {
        return alias + ". " + shortName
    }
}