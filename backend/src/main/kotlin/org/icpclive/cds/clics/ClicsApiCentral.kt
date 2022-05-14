package org.icpclive.cds.clics

import org.icpclive.utils.BasicAuth
import org.icpclive.utils.NetworkUtils
import org.icpclive.utils.processCreds
import java.util.*

class ClicsApiCentral(properties: Properties) {
    private val contestUrl = properties.getProperty("url")
    private val login = properties.getProperty("login")?.processCreds()
    private val password = properties.getProperty("password")?.processCreds()

    init { // may be login and password useless for event-feed
        NetworkUtils.prepareNetwork(login, password)
    }

    val auth = login?.let { login -> password?.let { password -> BasicAuth(login, password) } }
    val teamsUrl = apiRequestUrl("teams")
    val problemsUrl = apiRequestUrl("problems")
    val stateUrl = apiRequestUrl("state")
    val eventFeedUrl = apiRequestUrl("event-feed")

    private fun apiRequestUrl(method: String) = "$contestUrl/$method"
}
