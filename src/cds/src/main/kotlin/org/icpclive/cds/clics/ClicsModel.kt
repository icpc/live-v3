package org.icpclive.cds.clics

import kotlinx.datetime.Instant
import org.icpclive.api.*
import org.icpclive.cds.clics.api.*
import org.icpclive.cds.clics.model.ClicsJudgementTypeInfo
import org.icpclive.cds.clics.model.ClicsOrganisationInfo
import org.icpclive.cds.clics.model.ClicsRunInfo
import org.icpclive.util.getLogger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class ClicsModel(
    private val addTeamNames: Boolean,
    private val hiddenGroups: Set<String>,
    private val mediaBaseUrl: String
) {
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
    private val hiddenTeams = mutableSetOf<String>()
    private val teamSubmissions = mutableMapOf<Int, MutableSet<String>>()

    var startTime: Instant? = null
    var contestLength = 5.hours
    var freezeTime = 4.hours
    var status = ContestStatus.BEFORE
    var penaltyPerWrongAttempt = 20
    var holdBeforeStartTime: Duration? = null

    fun getAllRuns() = submissions.values.map { it.toApi() }

    fun teamName(org: String?, name: String) = when {
        org == null -> name
        addTeamNames -> "$org: $name"
        else -> org
    }

    private fun String.maybeRelative() = when  {
        startsWith("http://") -> this
        startsWith("https://") -> this
        else -> "$mediaBaseUrl/$this"
    }

    private fun Media.mediaType(): MediaType? {
        if (mime.startsWith("image")) {
            return MediaType.Photo(href.maybeRelative())
        }
        if (mime.startsWith("video")) {
            return MediaType.Video(href.maybeRelative())
        }
        return null
    }

    fun Team.toApi(): TeamInfo {
        val teamOrganization = organization_id?.let { organisations[it] }
        return TeamInfo(
            id = liveTeamId(id),
            name = teamName(teamOrganization?.formalName, name),
            shortName = teamName(teamOrganization?.name, name),
            contestSystemId = id,
            isHidden = hiddenTeams.contains(id),
            groups = group_ids.mapNotNull { groups[it]?.name },
            hashTag = teamOrganization?.hashtag,
            medias = buildMap {
                photo.firstOrNull()?.mediaType()?.let { put(TeamMediaType.PHOTO, it) }
                video.firstOrNull()?.mediaType()?.let { put(TeamMediaType.RECORD, it) }
                webcam.firstOrNull()?.mediaType()?.let { put(TeamMediaType.CAMERA, it) }
                desktop.firstOrNull()?.mediaType()?.let { put(TeamMediaType.SCREEN, it) }
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
            resultType = ContestResultType.ICPC,
            startTime = startTime ?: Instant.fromEpochSeconds(0),
            contestLength = contestLength,
            freezeTime = freezeTime,
            problems = problems.values.map { it.toApi() },
            teams = teams.values.map { it.toApi() },
            penaltyPerWrongAttempt = penaltyPerWrongAttempt,
            holdBeforeStartTime = holdBeforeStartTime
        )

    fun processContest(contest: Contest): List<RunInfo> {
        startTime = contest.start_time
        contestLength = contest.duration
        contest.scoreboard_freeze_duration?.let { freezeTime = contestLength - it }
        contest.countdown_pause_time?.let {
            holdBeforeStartTime = it
        }
        penaltyPerWrongAttempt = contest.penalty_time ?: 20
        return emptyList()
    }

    fun processProblem(id: String, problem: Problem?): List<RunInfo> {
        if (problem == null) {
            problems.remove(id)
        } else {
            require(id == problem.id)
            problems[problem.id] = problem
        }
        return emptyList()
    }

    fun processOrganization(id: String, organization: Organization?): List<RunInfo> {
        if (organization == null) {
            organisations.remove(id)
        } else {
            require(id == organization.id)
            organisations[organization.id] = ClicsOrganisationInfo(
                id = organization.id,
                name = organization.name,
                formalName = organization.formal_name ?: organization.name,
                logo = organization.logo.lastOrNull()?.href?.maybeRelative(),
                hashtag = organization.twitter_hashtag,
            )
        }
        return emptyList()
    }

    fun processTeam(id: String, team: Team?): List<RunInfo> {
        if (team == null) {
            teams.remove(id)
        } else {
            require(id == team.id)
            teams[id] = team
            setTeamHidden(team.id, team.is_hidden || team.group_ids.any { groups[it]?.name in hiddenGroups })
        }
        return emptyList()
    }

    fun processJudgementType(id: String, judgementType: JudgementType?): List<RunInfo> {
        if (judgementType == null) {
            judgementTypes.remove(id)
        } else {
            require(id == judgementType.id)
            judgementTypes[judgementType.id] = ClicsJudgementTypeInfo(
                id = judgementType.id,
                isAccepted = judgementType.solved,
                isAddingPenalty = judgementType.penalty,
            )
        }
        return emptyList()
    }

    fun processGroup(id: String, group: Group?): List<RunInfo> {
        if (group == null) {
            groups.remove(id)
        } else {
            require(id == group.id)
            groups[id] = group
        }
        return emptyList()
    }

    private fun setTeamHidden(teamId: String, isHidden: Boolean): List<RunInfo> {
        val wasHidden = teamId in hiddenTeams
        if (wasHidden == isHidden) return emptyList()
        if (isHidden) {
            hiddenTeams.add(teamId)
        } else {
            hiddenTeams.remove(teamId)
        }
        return teamSubmissions[teamCdsIdToId[teamId]]
            ?.mapNotNull { submissions[it]?.apply { this.isHidden = isHidden } }
            ?.map { it.toApi() }
            ?: emptyList()
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
            isHidden = team.id in hiddenTeams,
            reactionVideos = submission.reaction?.mapNotNull { it.mediaType() } ?: emptyList()
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

    fun processState(state: State): List<RunInfo> {
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
