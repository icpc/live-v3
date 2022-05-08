package org.icpclive.cds.wf2

import kotlinx.datetime.Instant
import kotlinx.serialization.json.*
import org.icpclive.cds.wf2.api.*
import org.icpclive.cds.wf2.model.*
import java.awt.Color
import kotlin.time.Duration.Companion.hours
import kotlin.time.DurationUnit

class WF2Model {
    val problems = mutableMapOf<String, WF2ProblemInfo>()
    private val organisations = mutableMapOf<String, WF2OrganisationInfo>()
    val teams = mutableMapOf<String, WF2TeamInfo>()
    private val submissionCdsIdToInt = mutableMapOf<String, Int>()
    val submissions = mutableMapOf<String, WF2RunInfo>()

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
        problems[problemObject.id] = WF2ProblemInfo(
            id = problemObject.ordinal - 1, // todo: это не может работать
            letter = problemObject.label,
            name = problemObject.name,
            color = problemObject.rgb?.let { Color.decode(it) } ?: Color.GRAY,
            testCount = problemObject.test_data_count
        )
    }

    fun processOrganisation(o: JsonObject) {
        val organisationObject = jsonDecoder.decodeFromJsonElement<WF2Organisation>(o)
        organisations[organisationObject.id] = WF2OrganisationInfo(
            id = organisationObject.id,
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
            screens = teamObject.desktop.map { it.href },
            cameras = teamObject.webcam.map { it.href },
        )
    }

    fun processSubmission(o: JsonObject): WF2RunInfo {
        val submissionObject = jsonDecoder.decodeFromJsonElement<WF2Submission>(o)

        val id = synchronized(submissionCdsIdToInt) {
            return@synchronized submissionCdsIdToInt.putIfAbsent(submissionObject.id, submissionCdsIdToInt.size + 1)
                ?: submissionCdsIdToInt[submissionObject.id]!!
        }
        val problem = problems[submissionObject.problem_id]
            ?: throw IllegalStateException("Failed to load submission with problem_id ${submissionObject.problem_id}")
        val team = teams[submissionObject.team_id]
            ?: throw IllegalStateException("Failed to load submission with team_id ${submissionObject.team_id}")
        val run = WF2RunInfo(
            id = id,
            problemId = problem.id,
            teamId = team.id,
            submissionTime = submissionObject.contest_time
        )
        submissions[submissionObject.id] = run
        return run
    }

    fun processJudgement(o: JsonObject): WF2RunInfo {
        val judgementObject = jsonDecoder.decodeFromJsonElement<WF2Judgement>(o)

        val run = submissions[judgementObject.submission_id]
            ?: throw IllegalStateException("Failed to load judgment with submission_id ${judgementObject.submission_id}")
        judgementObject.end_contest_time?.let { run.lastUpdateTime = it.toLong(DurationUnit.MILLISECONDS) }
        judgementObject.judgement_type_id?.let { run.result = it }


//        val problem = problems[submissionObject.problem_id]
//            ?: throw IllegalStateException("Failed to load submission with problem_id ${submissionObject.problem_id}")
//        val team = teams[submissionObject.team_id]
//            ?: throw IllegalStateException("Failed to load submission with team_id ${submissionObject.team_id}")
//        val run = WF2RunInfo(
//            id = id,
//            problemId = problem.id,
//            teamId = team.id,
//            submissionTime = submissionObject.contest_time
//        )
//        submissions[id] = run
        return run
    }

    private val jsonDecoder = Json { ignoreUnknownKeys = true; explicitNulls = false }
}
