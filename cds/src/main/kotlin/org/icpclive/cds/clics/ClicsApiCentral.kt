package org.icpclive.cds.clics

import org.icpclive.cds.common.ClientAuth
import org.icpclive.util.getCredentials
import java.util.*

class ClicsApiCentral(properties: Properties, creds: Map<String, String>) {
    private val contestUrl = properties.getProperty("url")
    private val login = properties.getCredentials("login", creds)
    private val password = properties.getCredentials("password", creds)

    val auth = login?.let { login -> password?.let { password -> ClientAuth.Basic(login, password) } }
    val eventFeedUrl = apiRequestUrl("event-feed")

    private fun apiRequestUrl(method: String) = "$contestUrl/$method"
}
