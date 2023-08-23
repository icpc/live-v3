package org.icpclive.cds.clics

import kotlinx.datetime.Instant
import org.icpclive.api.*
import org.icpclive.cds.clics.api.*
import org.icpclive.cds.clics.api.Organization
import org.icpclive.cds.clics.model.*
import org.icpclive.util.Enumerator
import org.icpclive.util.getLogger
import java.awt.Color
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

internal class ClicsModel(
    private val addTeamNames: Boolean,
    private val mediaBaseUrl: String
) {
    private val judgementTypes = mutableMapOf<String, ClicsJudgementTypeInfo>()
    private val problems = mutableMapOf<String, Problem>()
    private val organisations = mutableMapOf<String, ClicsOrganisationInfo>()
    private val teams = mutableMapOf<String, Team>()
    private val submissionId = Enumerator<String>()
    private val teamId = Enumerator<String>()
    private val problemToId = Enumerator<String>()
    private val submissions = mutableMapOf<String, ClicsRunInfo>()
    private val judgements = mutableMapOf<String, Judgement>()
    private val groups = mutableMapOf<String, Group>()

    var startTime: Instant? = null
    var contestLength = 5.hours
    var freezeTime = 4.hours
    var status = ContestStatus.BEFORE
    var penaltyPerWrongAttempt = 20.minutes
    var holdBeforeStartTime: Duration? = null
    var name: String = ""

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

    private fun Group.toApi() : GroupInfo = GroupInfo(name, isHidden = false, isOutOfContest = false)

    private fun Team.toApi(): TeamInfo {
        val teamOrganization = organization_id?.let { organisations[it] }
        return TeamInfo(
            id = teamId[id],
            fullName = teamName(teamOrganization?.formalName, name),
            displayName = teamName(teamOrganization?.name, name),
            contestSystemId = id,
            isHidden = hidden,
            groups = group_ids.mapNotNull { groups[it]?.name },
            hashTag = teamOrganization?.hashtag,
            medias = buildMap {
                photo.firstOrNull()?.mediaType()?.let { put(TeamMediaType.PHOTO, it) }
                video.firstOrNull()?.mediaType()?.let { put(TeamMediaType.RECORD, it) }
                webcam.firstOrNull()?.mediaType()?.let { put(TeamMediaType.CAMERA, it) }
                desktop.firstOrNull()?.mediaType()?.let { put(TeamMediaType.SCREEN, it) }
            },
            organizationId = organization_id,
            isOutOfContest = false,
            customFields = mapOf(
                "name" to name,
            )
        )
    }

    private fun Problem.toApi() = ProblemInfo(
        letter = label,
        name = name,
        id = problemToId[id],
        ordinal = ordinal,
        contestSystemId = id,
        color = rgb ?: Color.BLACK
    )

    private fun ClicsOrganisationInfo.toApi() = OrganizationInfo(
        cdsId = id,
        displayName = name,
        fullName = formalName,
    )

    val contestInfo: ContestInfo
        get() = ContestInfo(
            name = name,
            status = status,
            resultType = ContestResultType.ICPC,
            startTime = startTime ?: Instant.fromEpochSeconds(0),
            contestLength = contestLength,
            freezeTime = freezeTime,
            problemList = problems.values.map { it.toApi() },
            teamList = teams.values.map { it.toApi() },
            groupList = groups.values.map { it.toApi() },
            penaltyPerWrongAttempt = penaltyPerWrongAttempt,
            holdBeforeStartTime = holdBeforeStartTime,
            penaltyRoundingMode = PenaltyRoundingMode.EACH_SUBMISSION_DOWN_TO_MINUTE,
            organizationList = organisations.values.map { it.toApi() }
        )

    fun processContest(contest: Contest): List<RunInfo> {
        name = contest.formal_name ?: ""
        startTime = contest.start_time
        contestLength = contest.duration
        freezeTime = contestLength - (contest.scoreboard_freeze_duration ?: Duration.ZERO)
        holdBeforeStartTime = contest.countdown_pause_time
        penaltyPerWrongAttempt = contest.penalty_time ?: 20.minutes
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

    fun processSubmission(submission: Submission): ClicsRunInfo {
        val id = submissionId[submission.id]
        val problem = problems[submission.problem_id]
            ?: throw IllegalStateException("Failed to load submission with problem_id ${submission.problem_id}")
        val team = teams[submission.team_id]
            ?: throw IllegalStateException("Failed to load submission with team_id ${submission.team_id}")
        val run = ClicsRunInfo(
            id = id,
            problem = problem,
            liveProblemId = problemToId[problem.id],
            teamId = teamId[team.id],
            submissionTime = submission.contest_time,
            reactionVideos = submission.reaction?.mapNotNull { it.mediaType() } ?: emptyList()
        )
        submissions[submission.id] = run
        return run
    }

    fun processJudgement(judgement: Judgement): ClicsRunInfo {
        val run = submissions[judgement.submission_id]
            ?: throw IllegalStateException("Failed to load judgment with submission_id ${judgement.submission_id}")
        judgements[judgement.id] = judgement
        judgement.judgement_type_id?.let { run.judgementType = judgementTypes[it] }
        logger.debug("Process $judgement")
        return run
    }

    fun processRun(casesRun: Run): ClicsRunInfo {
        val judgement = judgements[casesRun.judgement_id]
            ?: throw IllegalStateException("Failed to load run with judgment_id ${casesRun.judgement_id}")
        val run = submissions[judgement.submission_id]
            ?: throw IllegalStateException("Failed to load run with judgment_id ${casesRun.judgement_id}, submission_id ${judgement.submission_id}")
        val judgementType = judgementTypes[casesRun.judgement_type_id]
        if (judgementType?.isAccepted == true) { // may be WA runs also need to add
            run.passedCaseRun.add(casesRun.ordinal)
        }
        logger.debug("$casesRun with verdict $judgementType")
        return run
    }

    fun processCommentary(commentary: Commentary) =
        AnalyticsCommentaryEvent(
            commentary.id,
            commentary.message,
            commentary.time,
            commentary.contest_time,
            commentary.team_ids?.map { teamId[it] } ?: emptyList(),
            commentary.submission_ids?.map { submissionId[it] } ?: emptyList(),
        )


    fun processState(state: State): List<RunInfo> {
        status = when {
            state.finalized != null && (state.frozen == null || state.thawed != null) -> ContestStatus.FINALIZED
            state.ended != null -> ContestStatus.OVER
            state.started != null -> ContestStatus.RUNNING
            else -> ContestStatus.BEFORE
        }
        return emptyList()
    }

    companion object {
        val logger = getLogger(ClicsModel::class)
    }
}
