package org.icpclive.cds.cats

import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import org.icpclive.api.ContestInfo
import org.icpclive.api.ContestResultType
import org.icpclive.api.ContestStatus
import org.icpclive.cds.ContestParseResult
import org.icpclive.cds.FullReloadContestDataSource
import org.icpclive.cds.common.jsonLoaderService
import org.icpclive.util.getCredentials
import java.util.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds


class CATSDataSource(val properties: Properties, creds: Map<String, String>) : FullReloadContestDataSource(5.seconds) {
    private val login = properties.getCredentials("login", creds)
    private val password = properties.getCredentials("password", creds)

    // :TODO I may send less count of quires to the server but I need to know when my sid is expired
    private val sid: String
        get() = runBlocking {
            async { authLoader.loadOnce().sid }.await()
        }


    @Serializable
    data class Auth(val status: String, val sid: String, val cid: String)

    @Serializable
    data class Problem(val id: Long, val name: String, val code: String)

    @Serializable
    data class Problems(val problems: List<Problem>)


    private val authLoader =
        jsonLoaderService<Auth> { "${properties.getProperty("url")}?f=login;login=$login;passwd=$password;json=1" }
    private val problemsLoader =
        jsonLoaderService<Problems> { "${properties.getProperty("url")}problems?cid=${properties.getProperty("cid")};sid=$sid;json=1" }


    override suspend fun loadOnce(): ContestParseResult {
        val element = problemsLoader.loadOnce()
        for (elem in element.problems) {
            println(elem)
        }
        val time = Clock.System.now()

        val contestInfo = ContestInfo(
            status = ContestStatus.UNKNOWN,
            resultType = ContestResultType.ICPC,
            startTime = Clock.System.now(),
            contestLength = 5.hours,
            freezeTime = 1.hours,
            problems = listOf(),
            teams = listOf()
        )
        return ContestParseResult(contestInfo, listOf(), listOf())
    }

}