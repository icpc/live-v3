package org.icpclive.cds.clics

import kotlinx.datetime.Instant
import org.icpclive.api.*
import org.icpclive.cds.clics.api.*
import org.icpclive.cds.clics.model.ClicsJudgementTypeInfo
import org.icpclive.cds.clics.model.ClicsOrganisationInfo
import org.icpclive.cds.clics.model.ClicsRunInfo
import org.icpclive.utils.getLogger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class ClicsModel {
    private val judgementTypes = mutableMapOf<String, ClicsJudgementTypeInfo>()
    private val problems = mutableMapOf<String, Problem>()
    private val organisations = mutableMapOf<String, ClicsOrganisationInfo>()
    private val teams = mutableMapOf<String, Team>()
    private val submissionCdsIdToId = mutableMapOf<String, Int>()
    private val teamCdsIdToId = mutableMapOf<String, Int>()
    private val problemCdsIdToId = mutableMapOf<String, Int>()
    val submissions = mutableMapOf<String, ClicsRunInfo>()
    private val judgements = mutableMapOf<String, Judgement>()
    private val groups = mutableMapOf<String, Group>()

    var startTime = Instant.fromEpochMilliseconds(0)
    var contestLength = 5.hours
    var freezeTime = 4.hours
    var status = ContestStatus.BEFORE
    var penaltyPerWrongAttempt = 20
    var holdBeforeStartTime: Duration? = null

    fun Team.toApi(): TeamInfo {
        val teamOrganization = organization_id?.let { organisations[it] }
        return TeamInfo(
            id = liveTeamId(id),
            name = teamOrganization?.formalName ?: name,
            shortName = teamOrganization?.name ?: name,
            contestSystemId = id,
            groups = group_ids.mapNotNull { groups[it]?.name },
            hashTag = teamOrganization?.hashtag,
            medias = buildMap {
                photo.firstOrNull()?.let { put(MediaType.PHOTO, it.href) }
                video.firstOrNull()?.let { put(MediaType.RECORD, it.href) }
                webcam.firstOrNull()?.let { put(MediaType.CAMERA, it.href) }
                desktop.firstOrNull()?.let { put(MediaType.SCREEN, it.href) }
            }
        )
    }

    fun Problem.toApi() = ProblemInfo(
        letter = label,
        name = name,
        color = rgb,
        id = liveProblemId(id),
        ordinal = ordinal
    )

    val contestInfo: ContestInfo
        get() = ContestInfo(
            status = status,
            startTime = startTime,
            contestLength = contestLength,
            freezeTime = freezeTime,
            problems = problems.values.map { it.toApi() },
            teams = teams.values.map { it.toApi() },
            penaltyPerWrongAttempt = penaltyPerWrongAttempt,
            holdBeforeStartTime = holdBeforeStartTime
        )

    fun processContest(contest: Contest) {
        contest.start_time?.let { startTime = it }
        contestLength = contest.duration
        contest.scoreboard_freeze_duration?.let { freezeTime = contestLength - it }
        contest.countdown_pause_time?.let {
            holdBeforeStartTime = it
        }
        penaltyPerWrongAttempt = contest.penalty_time ?: 20
    }

    fun processProblem(id:String, problem: Problem?) {
        if (problem == null) {
            problems.remove(id)
        } else {
            require(id == problem.id)
            problems[problem.id] = problem
        }
    }

    fun processOrganization(id: String, organization: Organization?) {
        if (organization == null) {
            organisations.remove(id)
        } else {
            require(id == organization.id)
            organisations[organization.id] = ClicsOrganisationInfo(
                id = organization.id,
                name = organization.name,
                formalName = organization.formal_name ?: organization.name,
                logo = organization.logo.lastOrNull()?.href,
                hashtag = organization.twitter_hashtag
            )
        }
    }

    fun processTeam(id: String, team: Team?) {
        if (team == null) {
            teams.remove(id)
        } else {
            require(id == team.id)
            teams[id] = team
        }
    }

    fun processJudgementType(id: String, judgementType: JudgementType?) {
        if (judgementType == null) {
            judgementTypes.remove(id)
        } else {
            require(id == judgementType.id)
            judgementTypes[judgementType.id] = ClicsJudgementTypeInfo(
                id = judgementType.id,
                isAccepted = judgementType.solved!!,
                isAddingPenalty = judgementType.penalty,
            )
        }
    }

    fun processGroup(id: String, group: Group?) {
        if (group == null) {
            groups.remove(id)
        } else {
            require(id == group.id)
            groups[id] = group
        }
    }

    fun processSubmission(submission: Submission): ClicsRunInfo {
        val id = liveSubmissionId(submission.id)
        val problem = problems[submission.problem_id]
            ?: throw IllegalStateException("Failed to load submission with problem_id ${submission.problem_id}")
        val team = teams[submission.team_id]
            ?: throw IllegalStateException("Failed to load submission with team_id ${submission.team_id}")
        val run = ClicsRunInfo(
            id = id,
            problem = problem,
            liveProblemId = liveProblemId(problem.id),
            teamId = liveTeamId(team.id),
            submissionTime = submission.contest_time
        )
        submissions[submission.id] = run
        return run
    }

    fun processJudgement(judgement: Judgement): ClicsRunInfo {
        val run = submissions[judgement.submission_id]
            ?: throw IllegalStateException("Failed to load judgment with submission_id ${judgement.submission_id}")
        judgements[judgement.id] = judgement
        if (run.submissionTime >= freezeTime) return run // TODO: why we can know it?
        judgement.judgement_type_id?.let { run.judgementType = judgementTypes[it] }
        logger.debug("Process $judgement")
        return run
    }

    fun processRun(casesRun: Run): ClicsRunInfo {
        val judgement = judgements[casesRun.judgement_id]
            ?: throw IllegalStateException("Failed to load run with judgment_id ${casesRun.judgement_id}")
        val run = submissions[judgement.submission_id]
            ?: throw IllegalStateException("Failed to load run with judgment_id ${casesRun.judgement_id}, submission_id ${judgement.submission_id}")
        if (run.submissionTime >= freezeTime) return run // TODO: why we can know it?
        val judgementType = judgementTypes[casesRun.judgement_type_id]
        if (judgementType?.isAccepted == true) { // may be WA runs also need to add
            run.passedCaseRun.add(casesRun.ordinal)
        }
        logger.debug("$casesRun with verdict $judgementType")
        return run
    }

    fun processState(state: State) {
        status = when {
            state.ended != null -> ContestStatus.OVER
            state.started != null -> ContestStatus.RUNNING
            else -> ContestStatus.BEFORE
        }
    }

    fun liveProblemId(cdsId: String) = problemCdsIdToId.getOrPut(cdsId) { problemCdsIdToId.size + 1 }
    fun liveTeamId(cdsId: String) = teamCdsIdToId.getOrPut(cdsId) { teamCdsIdToId.size + 1 }
    fun liveSubmissionId(cdsId: String) = submissionCdsIdToId.getOrPut(cdsId) { submissionCdsIdToId.size + 1 }

    companion object {
        val logger = getLogger(ClicsModel::class)
    }
}
