package org.icpclive.export

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.adapters.contestState
import org.icpclive.cds.api.ContestInfo
import org.icpclive.cds.api.RunInfo

abstract class SingleFileExporter(private val exportName: String, private val contentType: ContentType) {
    abstract fun format(info: ContestInfo, runs: List<RunInfo>): String

    fun Route.setUp(scope: CoroutineScope, contestUpdates: Flow<ContestUpdate>) {
        val stateFlow = contestUpdates
            .contestState()
            .stateIn(scope, SharingStarted.Eagerly, null)
            .filterNotNull()
            .filter { it.infoAfterEvent != null }
        get {
            call.respondRedirect(call.url { pathSegments += exportName }, permanent = true)
        }
        get(exportName) {
            call.respondText(contentType = contentType) {
                val state = stateFlow.first()
                format(
                    state.infoAfterEvent!!,
                    state.runsAfterEvent.values.toList()
                )
            }
        }
    }
}