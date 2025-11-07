package org.icpclive.cds.adapters.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.api.ContestInfo
import org.icpclive.cds.api.TeamId
import org.icpclive.cds.tunning.buildTemplateSubstitutor
import org.icpclive.cds.tunning.substitute
import org.icpclive.cds.utils.withGroupedRuns

internal fun propagateRunMediaTemplates(flow: Flow<ContestUpdate>): Flow<ContestUpdate> =
    flow.withGroupedRuns(
        { it.teamId },
        { key, _, original, info ->
            val reactionVideoTemplate = info?.teams?.get(key)?.reactionVideoTemplate
            val sourceTemplate = info?.teams?.get(key)?.sourceTemplate
            if (reactionVideoTemplate == null && sourceTemplate == null) {
                original
            } else {
                original.map {
                    val substitutor = buildTemplateSubstitutor {
                        addJson("run", Json.encodeToJsonElement(it))
                    }
                    it.copy(
                        reactionVideos = reactionVideoTemplate?.map(substitutor::substitute) ?: it.reactionVideos,
                        sourceFiles = sourceTemplate?.map(substitutor::substitute) ?: it.sourceFiles,
                    )
                }
            }
        },
        { new: ContestInfo, old: ContestInfo?, key: TeamId ->
            val newTeam = new.teams[key]
            val oldTeam = old?.teams?.get(key)
            newTeam?.reactionVideoTemplate != oldTeam?.reactionVideoTemplate ||
                    newTeam?.sourceTemplate != oldTeam?.sourceTemplate
        }
    ).map { it.event }