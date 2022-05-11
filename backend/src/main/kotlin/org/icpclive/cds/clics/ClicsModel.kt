package org.icpclive.cds.clics

import kotlinx.datetime.Instant
import kotlinx.serialization.json.*
import org.icpclive.cds.clics.api.*
import org.icpclive.cds.clics.model.*
import java.awt.Color
import kotlin.time.Duration.Companion.hours
import kotlin.time.DurationUnit

class ClicsModel {
    val problems = mutableMapOf<String, ClicsProblemInfo>()
    private val organisations = mutableMapOf<String, ClicsOrganisationInfo>()
    val teams = mutableMapOf<String, ClicsTeamInfo>()
    private val submissionCdsIdToInt = mutableMapOf<String, Int>()
    val submissions = mutableMapOf<String, ClicsRunInfo>()

    var startTime = Instant.fromEpochMilliseconds(0)
    var contestLength = 5.hours
    var freezeTime = 4.hours

    val contestInfo: ClicsContestInfo
        get() = ClicsContestInfo(
            problemsMap = problems,
            teams = teams.values.toList(),
            startTime = startTime,
            contestLength = contestLength,
            freezeTime = freezeTime
        )

    fun processContest(o: JsonObject) {
        val contestObject = jsonDecoder.decodeFromJsonElement<Contest>(o)
        contestObject.start_time?.let { startTime = it }
        contestLength = contestObject.duration
        contestObject.scoreboard_freeze_duration?.let { freezeTime = contestLength - it }
    }

    fun processProblem(o: JsonObject) {
        val problemObject = jsonDecoder.decodeFromJsonElement<Problem>(o)
        problems[problemObject.id] = ClicsProblemInfo(
            id = problemObject.ordinal - 1, // todo: это не может работать
            letter = problemObject.label,
            name = problemObject.name,
            color = problemObject.rgb?.let { Color.decode(it) } ?: Color.GRAY,
            testCount = problemObject.test_data_count
        )
    }

    fun processOrganisation(o: JsonObject) {
        val organisationObject = jsonDecoder.decodeFromJsonElement<Organisation>(o)
        organisations[organisationObject.id] = ClicsOrganisationInfo(
            id = organisationObject.id,
            name = organisationObject.name,
            formalName = organisationObject.formal_name ?: organisationObject.name,
            logo = organisationObject.logo.lastOrNull()?.href,
            hashtag = organisationObject.twitter_hashtag
        )
        // todo: update team if something changed
    }

    fun processTeam(o: JsonObject) {
        val teamObject = jsonDecoder.decodeFromJsonElement<Team>(o)

        val id = teamObject.id
        val teamOrganization = teamObject.organization_id?.let { organisations[it] }
        teams[id] = ClicsTeamInfo(
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

    fun processSubmission(o: JsonObject): ClicsRunInfo {
        val submissionObject = jsonDecoder.decodeFromJsonElement<Submission>(o)

        val id = synchronized(submissionCdsIdToInt) {
            return@synchronized submissionCdsIdToInt.putIfAbsent(submissionObject.id, submissionCdsIdToInt.size + 1)
                ?: submissionCdsIdToInt[submissionObject.id]!!
        }
        val problem = problems[submissionObject.problem_id]
            ?: throw IllegalStateException("Failed to load submission with problem_id ${submissionObject.problem_id}")
        val team = teams[submissionObject.team_id]
            ?: throw IllegalStateException("Failed to load submission with team_id ${submissionObject.team_id}")
        val run = ClicsRunInfo(
            id = id,
            problemId = problem.id,
            teamId = team.id,
            submissionTime = submissionObject.contest_time
        )
        submissions[submissionObject.id] = run
        return run
    }

    fun processJudgement(o: JsonObject): ClicsRunInfo {
        val judgementObject = jsonDecoder.decodeFromJsonElement<Judgement>(o)

        val run = submissions[judgementObject.submission_id]
            ?: throw IllegalStateException("Failed to load judgment with submission_id ${judgementObject.submission_id}")
        judgementObject.end_contest_time?.let { run.lastUpdateTime = it.toLong(DurationUnit.MILLISECONDS) }
        judgementObject.judgement_type_id?.let { run.result = judgementType(it) }
        return run
    }

    private fun judgementType(typeId: String) = when (typeId) {
        "RTE" -> "RE"
        "MLE" -> "ML"
        "OLE" -> "OL"
        "TLE" -> "TL"
        else -> typeId
    }

    private val jsonDecoder = Json { ignoreUnknownKeys = true; explicitNulls = false }
}
