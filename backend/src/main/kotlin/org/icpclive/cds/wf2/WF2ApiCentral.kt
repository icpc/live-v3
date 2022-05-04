package org.icpclive.cds.wf2

import org.icpclive.utils.NetworkUtils
import org.icpclive.utils.processCreds
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*

class WF2ApiCentral(properties: Properties) {
    private val contestUrl = properties.getProperty("url")
    private val login = properties.getProperty("login")?.processCreds()
    private val password = properties.getProperty("password")?.processCreds()
    init { // may be login and password useless for event-feed
        NetworkUtils.prepareNetwork(login, password)
    }

    val teamsUrl = apiRequestUrl("teams")
    val eventFeedUrl = apiRequestUrl("event-feed")

    private fun apiRequestUrl(method: String) = "$contestUrl/$method"
}
