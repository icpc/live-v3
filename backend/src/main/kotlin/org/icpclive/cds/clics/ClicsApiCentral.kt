package org.icpclive.cds.clics

import org.icpclive.utils.*
import java.util.*

class ClicsApiCentral(properties: Properties) {
    private val contestUrl = properties.getProperty("url")
    private val login = properties.getCredentials("login")
    private val password = properties.getCredentials("password")

    val auth = login?.let { login -> password?.let { password -> ClientAuth.Basic(login, password) } }
    val eventFeedUrl = apiRequestUrl("event-feed")

    private fun apiRequestUrl(method: String) = "$contestUrl/$method"
}
