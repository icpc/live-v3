package org.icpclive.converter.export

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.html.HtmlBlockTag
import kotlinx.html.a
import org.icpclive.cds.api.AccountInfo
import org.icpclive.cds.scoreboard.ContestStateWithScoreboard
import org.icpclive.converter.isAdminAccount

abstract class SingleFileExporter(
    private val httpPath: String,
    private val exportName: String,
    private val exportDescription: String,
    private val contentType: ContentType
) : Exporter {
    abstract fun format(state: ContestStateWithScoreboard): String

    override fun CoroutineScope.runOn(
        contestUpdates: Flow<ContestStateWithScoreboard>,
        adminContestUpdates: Flow<ContestStateWithScoreboard>,
    ): Router {
        val stateFlow = contestUpdates
            .stateIn(this, SharingStarted.Eagerly, null)
            .filterNotNull()
            .filter { it.state.infoAfterEvent != null }
        val adminStateFlow = adminContestUpdates
            .stateIn(this, SharingStarted.Eagerly, null)
            .filterNotNull()
            .filter { it.state.infoAfterEvent != null }

        return object : Router {
            override fun HtmlBlockTag.mainPage() {
                a("$httpPath/$exportName") {
                    +exportDescription
                }
            }
            override fun Route.setUpRoutes() {
                route(httpPath) {
                    get {
                        call.respondRedirect(call.url { pathSegments += exportName }, permanent = true)
                    }
                    get(exportName) {
                        call.respondText(contentType = contentType) {
                            val state = if (call.principal<AccountInfo>().isAdminAccount()) {
                                adminStateFlow
                            } else {
                                stateFlow
                            }.first()
                            format(state)
                        }
                    }
                }
            }
        }
    }
}