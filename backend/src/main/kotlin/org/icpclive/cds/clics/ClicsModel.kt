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
    private val submissions = mutableMapOf<String, ClicsRunInfo>()
    private val judgements = mutableMapOf<String, Judgement>()
    private val groups = mutableMapOf<String, Group>()
    private val accounts = mutableMapOf<String, Account>()
    private val specialTeams = mutableSetOf<String>()
    private val teamSubmissions = mutableMapOf<Int, MutableSet<String>>()

    var startTime = Instant.fromEpochMilliseconds(0)
    var contestLength = 5.hours
    var freezeTime = 4.hours
    var status = ContestStatus.BEFORE
    var penaltyPerWrongAttempt = 20
    var holdBeforeStartTime: Duration? = null

    fun getAllRuns() = submissions.values.map { it.toApi() }

    fun Team.toApi(): TeamInfo {
        val teamOrganization = organization_id?.let { organisations[it] }
        return TeamInfo(
            id = liveTeamId(id),
            name = teamOrganization?.formalName ?: name,
            shortName = teamOrganization?.name ?: name,
            contestSystemId = id,
            isHidden = specialTeams.contains(id),
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

    fun processContest(contest: Contest) : List<RunInfo> {
        contest.start_time?.let { startTime = it }
        contestLength = contest.duration
        contest.scoreboard_freeze_duration?.let { freezeTime = contestLength - it }
        contest.countdown_pause_time?.let {
            holdBeforeStartTime = it
        }
        penaltyPerWrongAttempt = contest.penalty_time ?: 20
        return emptyList()
    }

    fun processProblem(id:String, problem: Problem?) : List<RunInfo> {
        if (problem == null) {
            problems.remove(id)
        } else {
            require(id == problem.id)
            problems[problem.id] = problem
        }
        return emptyList();
    }

    fun processOrganization(id: String, organization: Organization?) : List<RunInfo> {
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
        return emptyList()
    }

    fun processTeam(id: String, team: Team?) : List<RunInfo> {
        if (team == null) {
            teams.remove(id)
        } else {
            require(id == team.id)
            teams[id] = team
        }
        return emptyList()
    }

    fun processJudgementType(id: String, judgementType: JudgementType?) : List<RunInfo> {
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
        return emptyList()
    }

    fun processGroup(id: String, group: Group?) : List<RunInfo> {
        if (group == null) {
            groups.remove(id)
        } else {
            require(id == group.id)
            groups[id] = group
        }
        return emptyList()
    }

    private fun setTeamType(teamId: String, type: Account.TYPE) : List<RunInfo> {
        val wasSpecial = teamId in specialTeams
        val isSpecial =  type != Account.TYPE.TEAM
        if (wasSpecial == isSpecial) return emptyList()
        if (isSpecial) {
            specialTeams.add(teamId)
        } else {
            specialTeams.remove(teamId)
        }
        return teamSubmissions[teamCdsIdToId[teamId]]
            ?.mapNotNull { submissions[it]?.apply { isHidden = isSpecial } }
            ?.map { it.toApi() }
            ?: emptyList()
    }

    fun processAccount(id: String, account: Account?) : List<RunInfo> {
        return if (account == null) {
            val old = accounts[id]
            accounts.remove(id)
            old?.team_id?.let { setTeamType(it, Account.TYPE.TEAM) }
        } else {
            accounts[id] = account
            account.team_id?.let { setTeamType(account.team_id, account.type ?: Account.TYPE.TEAM) }
        } ?: emptyList()
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
            submissionTime = submission.contest_time,
            isHidden = team.id in specialTeams,
        )
        submissions[submission.id]?.let { teamSubmissions[it.teamId]?.remove(submission.id) }
        submissions[submission.id] = run
        teamSubmissions.getOrPut(run.teamId) { mutableSetOf() }.add(submission.id)
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

    fun processState(state: State) : List<RunInfo> {
        status = when {
            state.ended != null -> ContestStatus.OVER
            state.started != null -> ContestStatus.RUNNING
            else -> ContestStatus.BEFORE
        }
        return emptyList()
    }

    fun liveProblemId(cdsId: String) = problemCdsIdToId.getOrPut(cdsId) { problemCdsIdToId.size + 1 }
    fun liveTeamId(cdsId: String) = teamCdsIdToId.getOrPut(cdsId) { teamCdsIdToId.size + 1 }
    fun liveSubmissionId(cdsId: String) = submissionCdsIdToId.getOrPut(cdsId) { submissionCdsIdToId.size + 1 }

    companion object {
        val logger = getLogger(ClicsModel::class)
    }
}
