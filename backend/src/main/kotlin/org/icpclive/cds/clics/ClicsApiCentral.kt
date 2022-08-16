package org.icpclive.cds.clics

import org.icpclive.utils.ClientAuth
import org.icpclive.utils.processCreds
import java.util.*

class ClicsApiCentral(properties: Properties) {
    private val contestUrl = properties.getProperty("url")
    private val login = properties.getProperty("login")?.processCreds()
    private val password = properties.getProperty("password")?.processCreds()

    val auth = login?.let { login -> password?.let { password -> ClientAuth.Basic(login, password) } }
    val eventFeedUrl = apiRequestUrl("event-feed")

    private fun apiRequestUrl(method: String) = "$contestUrl/$method"
}
