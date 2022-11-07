package org.icpclive.cds.clics

import org.icpclive.cds.common.ClientAuth
import org.icpclive.util.getCredentials
import java.util.*

class ClicsApiCentral(properties: Properties, creds: Map<String, String>) {
    private val contestUrl = properties.getProperty("url")

    val auth = ClientAuth.BasicOrNull(
        properties.getCredentials("login", creds),
        properties.getCredentials("password", creds)
    )
    val eventFeedUrl = apiRequestUrl("event-feed")

    private fun apiRequestUrl(method: String) = "$contestUrl/$method"

    val additionalEventFeedUrl: String? = properties.getProperty("additionalFeedUrl")?.takeIf { it.isNotEmpty() }
    val additionalEventFeedAuth = ClientAuth.BasicOrNull(
        properties.getCredentials("additionalFeedLogin", creds),
        properties.getCredentials("additionalFeedPassword", creds)
    )
}
