package org.icpclive.cds.clics

import kotlinx.datetime.Instant
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

    fun processContest(contest : Contest) {
        contest.start_time?.let { startTime = it }
        contestLength = contest.duration
        contest.scoreboard_freeze_duration?.let { freezeTime = contestLength - it }
    }

    fun processProblem(problem: Problem) {
        problems[problem.id] = ClicsProblemInfo(
            id = problem.ordinal - 1, // todo: это не может работать
            letter = problem.label,
            name = problem.name,
            color = problem.rgb?.let { Color.decode(it) } ?: Color.GRAY,
            testCount = problem.test_data_count
        )
    }

    fun processOrganization(organization: Organization) {
        organisations[organization.id] = ClicsOrganisationInfo(
            id = organization.id,
            name = organization.name,
            formalName = organization.formal_name ?: organization.name,
            logo = organization.logo.lastOrNull()?.href,
            hashtag = organization.twitter_hashtag
        )
        // todo: update team if something changed
    }

    fun processTeam(team: Team) {
        val id = team.id
        val teamOrganization = team.organization_id?.let { organisations[it] }
        teams[id] = ClicsTeamInfo(
            id = id.hashCode(),
            name = teamOrganization?.formalName ?: team.name,
            shortName = teamOrganization?.name ?: team.name,
            contestSystemId = id,
            groups = emptySet(),
            hashTag = teamOrganization?.hashtag,
            photo = team.photo.firstOrNull()?.href,
            video = team.video.firstOrNull()?.href,
            screens = team.desktop.map { it.href },
            cameras = team.webcam.map { it.href },
        )
    }

    fun processSubmission(submission: Submission): ClicsRunInfo {
        val id = synchronized(submissionCdsIdToInt) {
            return@synchronized submissionCdsIdToInt.putIfAbsent(submission.id, submissionCdsIdToInt.size + 1)
                ?: submissionCdsIdToInt[submission.id]!!
        }
        val problem = problems[submission.problem_id]
            ?: throw IllegalStateException("Failed to load submission with problem_id ${submission.problem_id}")
        val team = teams[submission.team_id]
            ?: throw IllegalStateException("Failed to load submission with team_id ${submission.team_id}")
        val run = ClicsRunInfo(
            id = id,
            problemId = problem.id,
            teamId = team.id,
            submissionTime = submission.contest_time
        )
        submissions[submission.id] = run
        return run
    }

    fun processJudgement(judgement: Judgement) : ClicsRunInfo {
        val run = submissions[judgement.submission_id]
            ?: throw IllegalStateException("Failed to load judgment with submission_id ${judgement.submission_id}")
        judgement.end_contest_time?.let { run.lastUpdateTime = it.toLong(DurationUnit.MILLISECONDS) }
        judgement.judgement_type_id?.let { run.result = judgementType(it) }
        return run
    }

    private fun judgementType(typeId: String) = when (typeId) {
        "RTE" -> "RE"
        "MLE" -> "ML"
        "OLE" -> "OL"
        "TLE" -> "TL"
        else -> typeId
    }
}
