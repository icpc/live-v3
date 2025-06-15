package org.icpclive.export

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import org.icpclive.Exporter
import org.icpclive.Router
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.adapters.contestState
import org.icpclive.cds.api.ContestInfo
import org.icpclive.cds.api.RunInfo
import org.icpclive.cds.api.ScoreboardRow
import org.icpclive.cds.api.TeamId
import org.icpclive.cds.scoreboard.ContestStateWithScoreboard

abstract class SingleFileExporter(
    private val httpPath: String,
    private val exportName: String,
    private val contentType: ContentType
) : Exporter {
    abstract fun format(state: ContestStateWithScoreboard): String

    override fun CoroutineScope.runOn(contestUpdates: Flow<ContestStateWithScoreboard>): Router {
        val stateFlow = contestUpdates
            .stateIn(this, SharingStarted.Eagerly, null)
            .filterNotNull()
            .filter { it.state.infoAfterEvent != null }
        return Router {
            route(httpPath) {
                get {
                    call.respondRedirect(call.url { pathSegments += exportName }, permanent = true)
                }
                get(exportName) {
                    call.respondText(contentType = contentType) {
                        val state = stateFlow.first()
                        format(state)
                    }
                }
            }
        }
    }
}