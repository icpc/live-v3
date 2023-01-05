package org.icpclive.cds.cats

import org.icpclive.cds.ContestParseResult
import org.icpclive.cds.FullReloadContestDataSource
import org.icpclive.cds.common.ClientAuth
import org.icpclive.cds.common.xmlLoaderService
import org.icpclive.util.getCredentials
import java.util.*
import kotlin.time.Duration.Companion.seconds

class CATSDataSource(val properties: Properties, creds: Map<String, String>): FullReloadContestDataSource(5.seconds) {
    private val login = properties.getCredentials("login", creds)
    private val password = properties.getCredentials("password", creds)
    private val dataLoader = xmlLoaderService(login?.let { ClientAuth.Basic(login, password!!) }) {
        properties.getProperty("url")
    }

    override suspend fun loadOnce(): ContestParseResult {
        TODO("Not yet implemented")
    }
}