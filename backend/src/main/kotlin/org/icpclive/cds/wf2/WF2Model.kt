package org.icpclive.cds.wf2

import kotlinx.datetime.Instant
import kotlinx.serialization.json.*
import org.icpclive.cds.wf2.api.WF2Contest
import org.icpclive.cds.wf2.api.WF2Organisation
import org.icpclive.cds.wf2.api.WF2Problem
import org.icpclive.cds.wf2.api.WF2Team
import org.icpclive.cds.wf2.model.WF2ContestInfo
import org.icpclive.cds.wf2.model.WF2OrganisationInfo
import org.icpclive.cds.wf2.model.WF2ProblemInfo
import org.icpclive.cds.wf2.model.WF2TeamInfo
import java.awt.Color
import kotlin.time.Duration.Companion.hours

class WF2Model {
    val problems = mutableMapOf<Int, WF2ProblemInfo>()
    private val organisations = mutableMapOf<String, WF2OrganisationInfo>()
    val teams = mutableMapOf<String, WF2TeamInfo>()
    var startTime = Instant.fromEpochMilliseconds(0)
    var contestLength = 5.hours
    var freezeTime = 4.hours

    val contestInfo: WF2ContestInfo
        get() = WF2ContestInfo(
            problemsMap = problems,
            teams = teams.values.toList(),
            startTime = startTime,
            contestLength = contestLength,
            freezeTime = freezeTime
        )

    fun processContest(o: JsonObject) {
        val contestObject = jsonDecoder.decodeFromJsonElement<WF2Contest>(o)
        contestObject.start_time?.let { startTime = it }
        contestLength = contestObject.duration
        contestObject.scoreboard_freeze_duration?.let { freezeTime = contestLength - it }
    }

    fun processProblem(o: JsonObject) {
        val problemObject = jsonDecoder.decodeFromJsonElement<WF2Problem>(o)
        problems[problemObject.ordinal] = WF2ProblemInfo(
            id = problemObject.ordinal,
            letter = problemObject.label,
            name = problemObject.name,
            color = problemObject.rgb?.let { Color.decode(it) } ?: Color.GRAY,
            testCount = problemObject.test_data_count
        )
    }

    fun processOrganisation(o: JsonObject) {
        val organisationObject = jsonDecoder.decodeFromJsonElement<WF2Organisation>(o)

        val id = organisationObject.id
        organisations[id] = WF2OrganisationInfo(
            id = id,
            name = organisationObject.name,
            formalName = organisationObject.formal_name ?: organisationObject.name,
            logo = organisationObject.logo.lastOrNull()?.href,
            hashtag = organisationObject.twitter_hashtag
        )
        // todo: update team if something changed
    }

    fun processTeam(o: JsonObject) {
        val teamObject = jsonDecoder.decodeFromJsonElement<WF2Team>(o)
        val id = teamObject.id
        val teamOrganization = teamObject.organization_id?.let { organisations[it] }

        teams[id] = WF2TeamInfo(
            id = id.hashCode(),
            name = teamOrganization?.formalName ?: teamObject.name,
            shortName = teamOrganization?.name ?: teamObject.name,
            contestSystemId = id,
            groups = emptySet(),
            hashTag = teamOrganization?.hashtag,
            photo = teamObject.photo.firstOrNull()?.href,
            video = teamObject.video.firstOrNull()?.href,
            screens = teamObject.desktop.map { it.href},
            cameras = teamObject.webcam.map { it.href },
        )
    }



    private val jsonDecoder = Json { ignoreUnknownKeys = true; explicitNulls = false }
}
