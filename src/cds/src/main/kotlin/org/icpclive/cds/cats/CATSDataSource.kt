package org.icpclive.cds.cats

import org.icpclive.cds.ContestParseResult
import org.icpclive.cds.FullReloadContestDataSource
import org.icpclive.cds.common.ClientAuth
import org.icpclive.cds.common.stringLoaderService
import org.icpclive.util.getCredentials
import java.util.*
import kotlin.time.Duration.Companion.seconds

class CATSDataSource(val properties: Properties, creds: Map<String, String>): FullReloadContestDataSource(5.seconds) {
    private val login = properties.getCredentials("login", creds)
    private val password = properties.getCredentials("password", creds)
    private val dataLoader = stringLoaderService(login?.let { ClientAuth.Basic(login, password!!) }) {
        properties.getProperty("url")
    }

    init {
        println("CATSDataSource initialized")
    }

    override suspend fun loadOnce(): ContestParseResult {
        TODO("Not yet implemented")
    }

}